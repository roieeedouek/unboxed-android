package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.CreatedTorrent
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

/** This interface is used by Retrofit to manage all the REST calls to the torrents endpoints */
interface TorrentsApi {

    @Multipart
    @POST("torrents/createtorrent")
    suspend fun createTorrent(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part?,
        @Part("magnet") magnet: RequestBody?,
        @Part("name") name: RequestBody?,
        @Part("seed") seed: RequestBody? = null,
        @Part("allow_zip") allowZip: RequestBody? = null,
    ): Response<TorBoxEnvelope<CreatedTorrent>>

    @POST("torrents/controltorrent")
    suspend fun controlTorrent(
        @Header("Authorization") token: String,
        @Body body: ItemControlRequest,
    ): Response<TorBoxEnvelope<Any?>>

    /**
     * Mints a direct download link for a single file (or a zip of the whole torrent). The API key
     * must be passed as the [apiKey] query parameter here - unlike every other endpoint, the
     * `Authorization` header alone is not accepted (confirmed live: 422 "missing query token").
     */
    @GET("torrents/requestdl")
    suspend fun requestDownloadLink(
        @Query("token") apiKey: String,
        @Query("torrent_id") torrentId: Long,
        @Query("file_id") fileId: Long?,
        @Query("zip_link") zipLink: Boolean? = null,
    ): Response<TorBoxEnvelope<String>>

    @GET("torrents/mylist")
    suspend fun getTorrentsList(
        @Header("Authorization") token: String,
        @Query("bypass_cache") bypassCache: Boolean? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
    ): Response<TorBoxEnvelope<List<TorrentItem>>>

    @GET("torrents/mylist")
    suspend fun getTorrentInfo(
        @Header("Authorization") token: String,
        @Query("id") id: Long,
    ): Response<TorBoxEnvelope<TorrentItem>>
}
