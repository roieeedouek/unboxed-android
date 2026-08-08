package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import retrofit2.Response

interface StreamingApiHelper {
    suspend fun createStream(
        token: String,
        id: Long,
        fileId: Long,
        type: String,
        subtitleIndex: Int? = null,
        audioIndex: Int? = null,
        resolutionIndex: Int? = null,
    ): Response<TorBoxEnvelope<Stream>>

    suspend fun getStreamData(
        token: String,
        presignedToken: String,
        apiKey: String,
        subtitleIndex: Int? = null,
        audioIndex: Int? = null,
        resolutionIndex: Int? = null,
    ): Response<TorBoxEnvelope<Stream>>
}
