package com.github.livingwithhippos.unchained.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * Response of `GET stream/createstream` and `GET stream/getstreamdata`. Unlike RD, which returned
 * a list of quality-specific direct MP4/WebM links, TorBox streams are a single adaptive-bitrate
 * HLS manifest ([hlsUrl]); [audioIndex]/[subtitleIndex]/[resolutionIndex] select which tracks
 * that manifest carries, and can be resubmitted to `createstream` to change the selection.
 * Passing `null`/`0` indexes on the first call returns the available options in [metadata].
 */
@JsonClass(generateAdapter = true)
data class Stream(
    @param:Json(name = "hls_url") val hlsUrl: String?,
    @param:Json(name = "presigned_token") val presignedToken: String?,
    @param:Json(name = "subtitle_index") val subtitleIndex: Int?,
    @param:Json(name = "audio_index") val audioIndex: Int?,
    @param:Json(name = "resolution_index") val resolutionIndex: Int?,
    @param:Json(name = "is_transcoding") val isTranscoding: Boolean?,
    @param:Json(name = "needs_transcoding") val needsTranscoding: Boolean?,
    @param:Json(name = "metadata") val metadata: StreamMetadata?,
)

@JsonClass(generateAdapter = true)
data class StreamMetadata(
    @param:Json(name = "video") val video: StreamVideoTrack?,
    @param:Json(name = "audios") val audios: List<StreamAudioTrack>?,
    @param:Json(name = "subtitles") val subtitles: List<StreamSubtitleTrack>?,
)

@JsonClass(generateAdapter = true)
data class StreamVideoTrack(
    @param:Json(name = "width") val width: Int?,
    @param:Json(name = "height") val height: Int?,
    @param:Json(name = "codec") val codec: String?,
)

@JsonClass(generateAdapter = true)
@Parcelize
data class StreamAudioTrack(
    @param:Json(name = "index") val index: Int,
    @param:Json(name = "language") val language: String?,
    @param:Json(name = "language_full") val languageFull: String?,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "default") val default: Boolean?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class StreamSubtitleTrack(
    @param:Json(name = "index") val index: Int,
    @param:Json(name = "language") val language: String?,
    @param:Json(name = "language_full") val languageFull: String?,
    @param:Json(name = "title") val title: String?,
) : Parcelable
