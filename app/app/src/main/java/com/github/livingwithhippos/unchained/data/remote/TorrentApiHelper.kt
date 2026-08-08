package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.CreatedTorrent
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

interface TorrentApiHelper {

    suspend fun createTorrent(
        token: String,
        file: MultipartBody.Part?,
        magnet: RequestBody?,
        name: RequestBody?,
        seed: RequestBody? = null,
        allowZip: RequestBody? = null,
    ): Response<TorBoxEnvelope<CreatedTorrent>>

    suspend fun controlTorrent(
        token: String,
        body: ItemControlRequest,
    ): Response<TorBoxEnvelope<Any?>>

    suspend fun requestDownloadLink(
        apiKey: String,
        torrentId: Long,
        fileId: Long?,
        zipLink: Boolean? = null,
    ): Response<TorBoxEnvelope<String>>

    suspend fun getTorrentsList(
        token: String,
        bypassCache: Boolean? = null,
        offset: Int? = null,
        limit: Int? = null,
    ): Response<TorBoxEnvelope<List<TorrentItem>>>

    suspend fun getTorrentInfo(token: String, id: Long): Response<TorBoxEnvelope<TorrentItem>>
}
