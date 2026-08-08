package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.CreatedWebDownload
import com.github.livingwithhippos.unchained.data.model.Hoster
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * This interface manages the REST calls to TorBox's `webdl` endpoints, the equivalent of RD's
 * Unrestrict API: submitting an arbitrary hoster link and getting a direct download link back.
 * Unlike RD's one-shot `unrestrict/link`, this is a 3-step round trip: [createWebDownload], then
 * poll [getWebDownloadInfo] until it's ready, then [requestDownloadLink].
 */
interface WebDownloadApi {

    @FormUrlEncoded
    @POST("webdl/createwebdownload")
    suspend fun createWebDownload(
        @Header("Authorization") token: String,
        @Field("link") link: String,
        @Field("password") password: String? = null,
        @Field("name") name: String? = null,
    ): Response<TorBoxEnvelope<CreatedWebDownload>>

    @POST("webdl/controlwebdownload")
    suspend fun controlWebDownload(
        @Header("Authorization") token: String,
        @Body body: ItemControlRequest,
    ): Response<TorBoxEnvelope<Any?>>

    /** See [TorrentsApi.requestDownloadLink] - the API key goes in the [apiKey] query param. */
    @GET("webdl/requestdl")
    suspend fun requestDownloadLink(
        @Query("token") apiKey: String,
        @Query("web_id") webId: Long,
        @Query("file_id") fileId: Long?,
        @Query("zip_link") zipLink: Boolean? = null,
    ): Response<TorBoxEnvelope<String>>

    @GET("webdl/mylist")
    suspend fun getWebDownloadsList(
        @Header("Authorization") token: String,
        @Query("bypass_cache") bypassCache: Boolean? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
    ): Response<TorBoxEnvelope<List<WebDownloadItem>>>

    @GET("webdl/mylist")
    suspend fun getWebDownloadInfo(
        @Header("Authorization") token: String,
        @Query("id") id: Long,
    ): Response<TorBoxEnvelope<WebDownloadItem>>

    @GET("webdl/hosters")
    suspend fun getHosters(
        @Header("Authorization") token: String
    ): Response<TorBoxEnvelope<List<Hoster>>>
}
