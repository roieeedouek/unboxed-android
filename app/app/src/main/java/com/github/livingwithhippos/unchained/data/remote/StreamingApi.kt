package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/** Content types accepted by TorBox's `type` query param on the stream endpoints. */
object StreamContentType {
    const val TORRENT = "torrent"
    const val WEB_DOWNLOAD = "webdownload"
}

/**
 * This interface is used by Retrofit to manage all the REST calls to the endpoints needed to
 * retrieve streaming links from a download. Like RD, streaming is a premium-only feature
 * ("PLAN_RESTRICTED_FEATURE" on a free plan).
 */
interface StreamingApi {

    @GET("stream/createstream")
    suspend fun createStream(
        @Header("Authorization") token: String,
        @Query("id") id: Long,
        @Query("file_id") fileId: Long,
        @Query("type") type: String,
        @Query("chosen_subtitle_index") subtitleIndex: Int? = null,
        @Query("chosen_audio_index") audioIndex: Int? = null,
        @Query("chosen_resolution_index") resolutionIndex: Int? = null,
    ): Response<TorBoxEnvelope<Stream>>

    @GET("stream/getstreamdata")
    suspend fun getStreamData(
        @Header("Authorization") token: String,
        @Query("presigned_token") presignedToken: String,
        @Query("token") apiKey: String,
        @Query("chosen_subtitle_index") subtitleIndex: Int? = null,
        @Query("chosen_audio_index") audioIndex: Int? = null,
        @Query("chosen_resolution_index") resolutionIndex: Int? = null,
    ): Response<TorBoxEnvelope<Stream>>
}
