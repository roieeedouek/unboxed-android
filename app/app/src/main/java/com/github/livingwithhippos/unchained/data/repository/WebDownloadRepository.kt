package com.github.livingwithhippos.unchained.data.repository

import android.os.SystemClock
import android.util.LruCache
import com.github.livingwithhippos.unchained.data.local.HostRegexDao
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.CreatedWebDownload
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.Hoster
import com.github.livingwithhippos.unchained.data.model.HostRegex
import com.github.livingwithhippos.unchained.data.model.InnerTorrentFile
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorrentOperation
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import com.github.livingwithhippos.unchained.data.remote.StreamContentType
import com.github.livingwithhippos.unchained.data.remote.WebDownloadApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Manages TorBox's `webdl` endpoints: the equivalent of RD's Unrestrict API, for turning an
 * arbitrary hoster link into a direct download. Unlike RD's one-shot `unrestrict/link`, TorBox
 * needs a 3-step round trip: [createWebDownload], poll [getWebDownloadInfo] until it's ready, then
 * [getDownloadLink] (`requestdl`) to actually mint the URL.
 */
@Singleton
class WebDownloadRepository
@Inject
constructor(
    protoStore: ProtoStore,
    private val webDownloadApiHelper: WebDownloadApiHelper,
    private val hostRegexDao: HostRegexDao,
) : BaseRepository(protoStore) {

    private val downloadLinkCache =
        LruCache<DownloadLinkCacheKey, CachedDownloadLink>(DOWNLOAD_LINK_CACHE_MAX_ENTRIES)

    suspend fun createWebDownload(
        link: String,
        password: String? = null,
        name: String? = null,
    ): EitherResult<UnchainedNetworkException, CreatedWebDownload> {
        val token = getToken()
        return eitherApiResult(
            call = {
                webDownloadApiHelper.createWebDownload(
                    token = "Bearer $token",
                    link = link,
                    password = password,
                    name = name,
                )
            },
            errorMessage = "Error Creating Web Download",
        )
    }

    suspend fun getWebDownloadInfo(id: Long): WebDownloadItem? {
        val token = getToken()
        return safeApiCall(
            call = { webDownloadApiHelper.getWebDownloadInfo(token = "Bearer $token", id = id) },
            errorMessage = "Error Retrieving Web Download Info",
        )
    }

    suspend fun getWebDownloadsList(offset: Int? = null, limit: Int? = null): List<WebDownloadItem> {
        val token = getToken()
        val response =
            safeApiCall(
                call = {
                    webDownloadApiHelper.getWebDownloadsList(
                        token = "Bearer $token",
                        offset = offset,
                        limit = limit,
                    )
                },
                errorMessage = "Error Fetching Web Downloads list or list empty",
            )
        return response ?: emptyList()
    }

    suspend fun deleteWebDownload(id: Long): EitherResult<UnchainedNetworkException, Unit> {
        val token = getToken()
        val response =
            eitherApiResultUnit(
                call = {
                    webDownloadApiHelper.controlWebDownload(
                        token = "Bearer $token",
                        body = ItemControlRequest(webId = id, operation = TorrentOperation.DELETE),
                    )
                },
                errorMessage = "Error deleting Web Download",
            )
        if (response is EitherResult.Success) clearDownloadLinkCache()
        return response
    }

    /**
     * Mints a direct download link for one file of a webdl item (or a zip of all its files if
     * [zipLink] is true), returning it wrapped in a locally-built [DownloadItem]. Results are
     * cached for a couple hours since `requestdl` calls are billable, same as RD's unrestrict was.
     */
    suspend fun getDownloadLink(
        webId: Long,
        fileId: Long?,
        host: String = "torbox.app",
        hostIcon: String? = null,
        zipLink: Boolean = false,
        file: InnerTorrentFile? = null,
    ): EitherResult<UnchainedNetworkException, DownloadItem> {
        val cacheKey = DownloadLinkCacheKey(webId, fileId, zipLink)
        getCachedDownloadLink(cacheKey)?.let {
            return EitherResult.Success(it)
        }

        val token = getToken()
        val response =
            eitherApiResult(
                call = {
                    webDownloadApiHelper.requestDownloadLink(
                        apiKey = token,
                        webId = webId,
                        fileId = fileId,
                        zipLink = zipLink,
                    )
                },
                errorMessage = "Error Requesting Web Download Link",
            )

        return when (response) {
            is EitherResult.Success -> {
                val item =
                    buildDownloadItem(
                        id = "webdl:$webId:${fileId ?: "zip"}",
                        contentId = webId,
                        fileId = fileId ?: 0L,
                        contentType = StreamContentType.WEB_DOWNLOAD,
                        file = file,
                        link = response.success,
                        host = host,
                        hostIcon = hostIcon,
                    )
                cacheDownloadLink(cacheKey, item)
                EitherResult.Success(item)
            }
            is EitherResult.Failure -> response
        }
    }

    suspend fun getDownloadLinkList(
        items: List<Triple<Long, Long?, InnerTorrentFile?>>,
        host: String = "torbox.app",
        callDelay: Long = 100,
    ): List<EitherResult<UnchainedNetworkException, DownloadItem>> {
        val results = mutableListOf<EitherResult<UnchainedNetworkException, DownloadItem>>()
        items.forEach { (webId, fileId, file) ->
            results.add(getDownloadLink(webId = webId, fileId = fileId, host = host, file = file))
            delay(callDelay.milliseconds)
        }
        return results
    }

    fun clearDownloadLinkCache() {
        synchronized(downloadLinkCache) { downloadLinkCache.evictAll() }
    }

    /**
     * Gets the list of hosters TorBox supports, replacing RD's `hosts/regex` + `availableHosts`.
     * Cached in Room since it rarely changes; refreshed from the network once the cache is thin.
     */
    suspend fun getHosters(): List<Hoster> {
        val token = getToken()
        val response =
            safeApiCall(
                call = { webDownloadApiHelper.getHosters(token = "Bearer $token") },
                errorMessage = "Error Fetching Hosters",
            )
        return response ?: emptyList()
    }

    /**
     * Gets the regexps used to detect whether a pasted link matches a supported hoster, from the
     * db if cached, otherwise refreshing them from the network first.
     */
    suspend fun getHostsRegex(): List<HostRegex> {
        var regexps = hostRegexDao.getAllRegexps()
        if (regexps.size < 10) {
            regexps = refreshHostsRegex()
        }
        return regexps
    }

    /** Refreshes the cached hoster regexps from the network, replacing the old ones. */
    suspend fun refreshHostsRegex(): List<HostRegex> {
        val hosters = getHosters()
        val newRegexps = hosters.map { HostRegex(it.regex) }
        if (newRegexps.size > 10) {
            hostRegexDao.deleteAll()
            hostRegexDao.insertAll(newRegexps)
            return newRegexps
        }
        return emptyList()
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
