package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.CreatedWebDownload
import com.github.livingwithhippos.unchained.data.model.Hoster
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import javax.inject.Inject
import retrofit2.Response

class WebDownloadApiHelperImpl @Inject constructor(private val webDownloadApi: WebDownloadApi) :
    WebDownloadApiHelper {

    override suspend fun createWebDownload(
        token: String,
        link: String,
        password: String?,
        name: String?,
    ): Response<TorBoxEnvelope<CreatedWebDownload>> =
        webDownloadApi.createWebDownload(token, link, password, name)

    override suspend fun controlWebDownload(
        token: String,
        body: ItemControlRequest,
    ): Response<TorBoxEnvelope<Any?>> = webDownloadApi.controlWebDownload(token, body)

    override suspend fun requestDownloadLink(
        apiKey: String,
        webId: Long,
        fileId: Long?,
        zipLink: Boolean?,
    ): Response<TorBoxEnvelope<String>> =
        webDownloadApi.requestDownloadLink(apiKey, webId, fileId, zipLink)

    override suspend fun getWebDownloadsList(
        token: String,
        bypassCache: Boolean?,
        offset: Int?,
        limit: Int?,
    ): Response<TorBoxEnvelope<List<WebDownloadItem>>> =
        webDownloadApi.getWebDownloadsList(token, bypassCache, offset, limit)

    override suspend fun getWebDownloadInfo(
        token: String,
        id: Long,
    ): Response<TorBoxEnvelope<WebDownloadItem>> = webDownloadApi.getWebDownloadInfo(token, id)

    override suspend fun getHosters(token: String): Response<TorBoxEnvelope<List<Hoster>>> =
        webDownloadApi.getHosters(token)
}
