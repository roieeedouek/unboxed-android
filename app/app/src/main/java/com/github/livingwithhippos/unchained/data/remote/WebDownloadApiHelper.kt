package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.CreatedWebDownload
import com.github.livingwithhippos.unchained.data.model.Hoster
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import retrofit2.Response

interface WebDownloadApiHelper {

    suspend fun createWebDownload(
        token: String,
        link: String,
        password: String? = null,
        name: String? = null,
    ): Response<TorBoxEnvelope<CreatedWebDownload>>

    suspend fun controlWebDownload(
        token: String,
        body: ItemControlRequest,
    ): Response<TorBoxEnvelope<Any?>>

    suspend fun requestDownloadLink(
        apiKey: String,
        webId: Long,
        fileId: Long?,
        zipLink: Boolean? = null,
    ): Response<TorBoxEnvelope<String>>

    suspend fun getWebDownloadsList(
        token: String,
        bypassCache: Boolean? = null,
        offset: Int? = null,
        limit: Int? = null,
    ): Response<TorBoxEnvelope<List<WebDownloadItem>>>

    suspend fun getWebDownloadInfo(token: String, id: Long): Response<TorBoxEnvelope<WebDownloadItem>>

    suspend fun getHosters(token: String): Response<TorBoxEnvelope<List<Hoster>>>
}
