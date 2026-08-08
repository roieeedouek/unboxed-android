package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.StreamingApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject

class StreamingRepository
@Inject
constructor(protoStore: ProtoStore, private val streamingApiHelper: StreamingApiHelper) :
    BaseRepository(protoStore) {

    /**
     * Starts (or re-selects tracks for) a stream. Pass null/0 indexes on the first call to just
     * discover the available audio/subtitle/resolution options in the response's metadata, then
     * call again with the chosen indexes to lock in the stream. Premium-only on TorBox's side
     * (fails with a "PLAN_RESTRICTED_FEATURE" error on free plans), same as RD's was.
     */
    suspend fun createStream(
        id: Long,
        fileId: Long,
        type: String,
        subtitleIndex: Int? = null,
        audioIndex: Int? = null,
        resolutionIndex: Int? = null,
    ): EitherResult<UnchainedNetworkException, Stream> =
        eitherApiResult(
            call = {
                streamingApiHelper.createStream(
                    token = "Bearer ${getToken()}",
                    id = id,
                    fileId = fileId,
                    type = type,
                    subtitleIndex = subtitleIndex,
                    audioIndex = audioIndex,
                    resolutionIndex = resolutionIndex,
                )
            },
            errorMessage = "Error Creating Stream",
        )

    suspend fun getStreamData(
        presignedToken: String,
        subtitleIndex: Int? = null,
        audioIndex: Int? = null,
        resolutionIndex: Int? = null,
    ): EitherResult<UnchainedNetworkException, Stream> {
        val token = getToken()
        return eitherApiResult(
            call = {
                streamingApiHelper.getStreamData(
                    token = "Bearer $token",
                    presignedToken = presignedToken,
                    apiKey = token,
                    subtitleIndex = subtitleIndex,
                    audioIndex = audioIndex,
                    resolutionIndex = resolutionIndex,
                )
            },
            errorMessage = "Error Fetching Streaming Info",
        )
    }
}
