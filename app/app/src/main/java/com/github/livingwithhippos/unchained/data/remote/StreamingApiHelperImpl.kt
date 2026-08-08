package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import javax.inject.Inject
import retrofit2.Response

class StreamingApiHelperImpl @Inject constructor(private val streamingApi: StreamingApi) :
    StreamingApiHelper {

    override suspend fun createStream(
        token: String,
        id: Long,
        fileId: Long,
        type: String,
        subtitleIndex: Int?,
        audioIndex: Int?,
        resolutionIndex: Int?,
    ): Response<TorBoxEnvelope<Stream>> =
        streamingApi.createStream(token, id, fileId, type, subtitleIndex, audioIndex, resolutionIndex)

    override suspend fun getStreamData(
        token: String,
        presignedToken: String,
        apiKey: String,
        subtitleIndex: Int?,
        audioIndex: Int?,
        resolutionIndex: Int?,
    ): Response<TorBoxEnvelope<Stream>> =
        streamingApi.getStreamData(
            token,
            presignedToken,
            apiKey,
            subtitleIndex,
            audioIndex,
            resolutionIndex,
        )
}
