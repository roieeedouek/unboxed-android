package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.CreatedTorrent
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import javax.inject.Inject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class TorrentApiHelperImpl @Inject constructor(private val torrentsApi: TorrentsApi) :
    TorrentApiHelper {

    override suspend fun createTorrent(
        token: String,
        file: MultipartBody.Part?,
        magnet: RequestBody?,
        name: RequestBody?,
        seed: RequestBody?,
        allowZip: RequestBody?,
    ): Response<TorBoxEnvelope<CreatedTorrent>> =
        torrentsApi.createTorrent(token, file, magnet, name, seed, allowZip)

    override suspend fun controlTorrent(
        token: String,
        body: ItemControlRequest,
    ): Response<TorBoxEnvelope<Any?>> = torrentsApi.controlTorrent(token, body)

    override suspend fun requestDownloadLink(
        apiKey: String,
        torrentId: Long,
        fileId: Long?,
        zipLink: Boolean?,
    ): Response<TorBoxEnvelope<String>> =
        torrentsApi.requestDownloadLink(apiKey, torrentId, fileId, zipLink)

    override suspend fun getTorrentsList(
        token: String,
        bypassCache: Boolean?,
        offset: Int?,
        limit: Int?,
    ): Response<TorBoxEnvelope<List<TorrentItem>>> =
        torrentsApi.getTorrentsList(token, bypassCache, offset, limit)

    override suspend fun getTorrentInfo(
        token: String,
        id: Long,
    ): Response<TorBoxEnvelope<TorrentItem>> = torrentsApi.getTorrentInfo(token, id)
}
