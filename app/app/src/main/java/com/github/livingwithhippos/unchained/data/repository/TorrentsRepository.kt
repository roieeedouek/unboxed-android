package com.github.livingwithhippos.unchained.data.repository

import android.os.SystemClock
import android.util.LruCache
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.CreatedTorrent
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.InnerTorrentFile
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.TorrentOperation
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.StreamContentType
import com.github.livingwithhippos.unchained.data.remote.TorrentApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class TorrentsRepository
@Inject
constructor(protoStore: ProtoStore, private val torrentApiHelper: TorrentApiHelper) :
    BaseRepository(protoStore) {

    private val downloadLinkCache =
        LruCache<DownloadLinkCacheKey, CachedDownloadLink>(DOWNLOAD_LINK_CACHE_MAX_ENTRIES)

    /**
     * Creates a torrent from either a magnet link or a .torrent file's bytes. Unlike RD, TorBox
     * has no "host" concept to pick, and every file downloads automatically - there's no
     * subsequent file-selection call to make.
     */
    suspend fun createTorrent(
        magnet: String? = null,
        torrentFile: ByteArray? = null,
        fileName: String? = null,
    ): EitherResult<UnchainedNetworkException, CreatedTorrent> {
        val token = getToken()

        val filePart: MultipartBody.Part? =
            torrentFile?.let {
                val body = it.toRequestBody("application/x-bittorrent".toMediaType(), 0, it.size)
                MultipartBody.Part.createFormData("file", fileName ?: "torrent.torrent", body)
            }
        val magnetPart: RequestBody? = magnet?.toRequestBody("text/plain".toMediaTypeOrNull())
        val namePart: RequestBody? = fileName?.toRequestBody("text/plain".toMediaTypeOrNull())

        return eitherApiResult(
            call = {
                torrentApiHelper.createTorrent(
                    token = "Bearer $token",
                    file = filePart,
                    magnet = magnetPart,
                    name = namePart,
                )
            },
            errorMessage = "Error Creating Torrent",
        )
    }

    suspend fun getTorrentInfo(id: Long): TorrentItem? {
        val token = getToken()
        return safeApiCall(
            call = { torrentApiHelper.getTorrentInfo(token = "Bearer $token", id = id) },
            errorMessage = "Error Retrieving Torrent Info",
        )
    }

    suspend fun getTorrentsList(offset: Int? = null, limit: Int? = null): List<TorrentItem> {
        val token = getToken()
        val torrentsResponse =
            safeApiCall(
                call = {
                    torrentApiHelper.getTorrentsList(
                        token = "Bearer $token",
                        offset = offset,
                        limit = limit,
                    )
                },
                errorMessage = "Error retrieving the torrents List, or list empty",
            )
        return torrentsResponse ?: emptyList()
    }

    suspend fun deleteTorrent(id: Long): EitherResult<UnchainedNetworkException, Unit> {
        val token = getToken()
        val response =
            eitherApiResultUnit(
                call = {
                    torrentApiHelper.controlTorrent(
                        token = "Bearer $token",
                        body = ItemControlRequest(torrentId = id, operation = TorrentOperation.DELETE),
                    )
                },
                errorMessage = "Error deleting Torrent",
            )
        if (response is EitherResult.Success) clearDownloadLinkCache()
        return response
    }

    suspend fun controlTorrent(
        id: Long,
        operation: String,
    ): EitherResult<UnchainedNetworkException, Unit> {
        val token = getToken()
        return eitherApiResultUnit(
            call = {
                torrentApiHelper.controlTorrent(
                    token = "Bearer $token",
                    body = ItemControlRequest(torrentId = id, operation = operation),
                )
            },
            errorMessage = "Error Controlling Torrent",
        )
    }

    /**
     * Mints a direct download link for one file of a torrent (or a zip of all its files if
     * [zipLink] is true), returning it wrapped in a locally-built [DownloadItem]. TorBox has no
     * server-side file-selection step, so this is called on demand for whichever file(s) the user
     * actually wants a link for. Results are cached for a couple hours since `requestdl` calls are
     * billable, same as RD's unrestrict was.
     */
    suspend fun getDownloadLink(
        torrentId: Long,
        fileId: Long?,
        zipLink: Boolean = false,
        file: InnerTorrentFile? = null,
    ): EitherResult<UnchainedNetworkException, DownloadItem> {
        val cacheKey = DownloadLinkCacheKey(torrentId, fileId, zipLink)
        getCachedDownloadLink(cacheKey)?.let {
            return EitherResult.Success(it)
        }

        val token = getToken()
        val response =
            eitherApiResult(
                call = {
                    torrentApiHelper.requestDownloadLink(
                        apiKey = token,
                        torrentId = torrentId,
                        fileId = fileId,
                        zipLink = zipLink,
                    )
                },
                errorMessage = "Error Requesting Torrent Download Link",
            )

        return when (response) {
            is EitherResult.Success -> {
                val item =
                    buildDownloadItem(
                        id = "torrent:$torrentId:${fileId ?: "zip"}",
                        contentId = torrentId,
                        fileId = fileId ?: 0L,
                        contentType = StreamContentType.TORRENT,
                        file = file,
                        link = response.success,
                        host = "torbox.app",
                    )
                cacheDownloadLink(cacheKey, item)
                EitherResult.Success(item)
            }
            is EitherResult.Failure -> response
        }
    }

    suspend fun getDownloadLinkList(
        items: List<Triple<Long, Long?, InnerTorrentFile?>>,
        callDelay: Long = 100,
    ): List<EitherResult<UnchainedNetworkException, DownloadItem>> {
        val results = mutableListOf<EitherResult<UnchainedNetworkException, DownloadItem>>()
        items.forEach { (torrentId, fileId, file) ->
            results.add(getDownloadLink(torrentId = torrentId, fileId = fileId, file = file))
            delay(callDelay.milliseconds)
        }
        return results
    }

    fun clearDownloadLinkCache() {
        synchronized(downloadLinkCache) { downloadLinkCache.evictAll() }
    }

    private fun cacheDownloadLink(key: DownloadLinkCacheKey, item: DownloadItem) {
        synchronized(downloadLinkCache) {
            downloadLinkCache.put(
                key,
                CachedDownloadLink(item = item, createdAtElapsedRealtime = SystemClock.elapsedRealtime()),
            )
        }
    }

    private fun getCachedDownloadLink(key: DownloadLinkCacheKey): DownloadItem? =
        synchronized(downloadLinkCache) {
            val cached = downloadLinkCache.get(key) ?: return@synchronized null
            val cacheAge = SystemClock.elapsedRealtime() - cached.createdAtElapsedRealtime
            if (cacheAge <= DOWNLOAD_LINK_CACHE_TTL_MS) {
                cached.item
            } else {
                downloadLinkCache.remove(key)
                null
            }
        }

    private data class DownloadLinkCacheKey(val id: Long, val fileId: Long?, val zipLink: Boolean)

    private data class CachedDownloadLink(val item: DownloadItem, val createdAtElapsedRealtime: Long)

    companion object {
        private const val DOWNLOAD_LINK_CACHE_MAX_ENTRIES = 5000
        private const val DOWNLOAD_LINK_CACHE_TTL_MS = 2 * 60 * 60 * 1000L
    }
}
