package com.github.livingwithhippos.unchained.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * A single resolved download link, ready to be opened/downloaded. Unlike RD's `DownloadItem`,
 * TorBox has no endpoint that returns this shape directly: it's built locally (see
 * `TorrentsRepository.getDownloadLink`/`WebDownloadRepository.getDownloadLink`) from a
 * `requestdl` URL plus the matching file's metadata, since minting the link and knowing the
 * file's name/size/type are two separate API calls (`mylist` + `requestdl`).
 *
 * @property id a synthetic id, e.g. "torrent:$torrentId:$fileId" or "webdl:$webId:$fileId"
 * @property contentId the owning torrent's or webdl item's id, needed to request a stream
 * @property fileId the file's own id within that torrent/webdl item, needed to request a stream
 * @property contentType [StreamContentType.TORRENT] or [StreamContentType.WEB_DOWNLOAD]
 * @property filename
 * @property mimeType guessed client-side from the file extension when TorBox doesn't provide one
 * @property fileSize in bytes, 0 if unknown
 * @property link the `requestdl` URL - there's no separate "original vs generated" link in TorBox
 * @property host "torbox.app" for torrents, or the hoster's domain for webdl links
 * @property hostIcon from `webdl/hosters`' `icon` field, when available
 * @property streamable a heuristic (1 or 0) derived from [mimeType]/[filename], not server data
 * @property alternative streaming quality alternatives, see [Stream]
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class DownloadItem(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "content_id") val contentId: Long,
    @param:Json(name = "file_id") val fileId: Long,
    @param:Json(name = "content_type") val contentType: String,
    @param:Json(name = "filename") val filename: String,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "filesize") val fileSize: Long,
    @param:Json(name = "link") val link: String,
    @param:Json(name = "host") val host: String,
    @param:Json(name = "host_icon") val hostIcon: String?,
    @param:Json(name = "streamable") val streamable: Int?,
    @param:Json(name = "alternative") val alternative: List<Alternative>?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class Alternative(
    @param:Json(name = "id") val id: String,
    @param:Json(name = "filename") val filename: String,
    @param:Json(name = "download") val download: String,
    @param:Json(name = "mimeType") val mimeType: String?,
    @param:Json(name = "quality") val quality: String?,
) : Parcelable
