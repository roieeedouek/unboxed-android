package com.github.livingwithhippos.unchained.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/*
[
{
    "id": 0,
    "auth_id": "string",
    "hash": "string",
    "name": "string", // Name of the torrent
    "magnet": "string",
    "size": 0, // Total size of the torrent, in bytes
    "active": false,
    "created_at": "string", // jsonDate
    "download_state": "string", // downloading, uploading, stalled (no seeds), paused, completed,
                                 // cached, metaDL, checkingResumeData, + qBittorrent states
    "seeds": 0,
    "peers": 0,
    "progress": 0.0, // 0.0 to 1.0
    "download_speed": 0,
    "eta": 0,
    "download_present": true, // at least one file is ready to be downloaded
    "download_finished": true, // the whole torrent finished downloading
    "files": [
    {
        "id": 0,
        "name": "string", // path relative to the torrent's root, e.g. "SubFolder/file.mkv"
        "short_name": "string", // just the file name
        "absolute_path": "string", // full path inside TorBox's storage, starting with "/"
        "size": 0,
        "mimetype": "string"
    }
    ],
    "cached": true,
    "expires_at": "string" // jsonDate, nullable
}
]
*/

/**
 * Torrent item: this class is used for the torrents/mylist (list and single-item variants)
 * endpoint.
 *
 * @property id
 * @property hash
 * @property name
 * @property magnet
 * @property size total size of the torrent, in bytes
 * @property active
 * @property createdAt
 * @property downloadState TorBox's status string, see [com.github.livingwithhippos.unchained.utilities.extension.getStatusTranslation]
 * @property seeds
 * @property peers
 * @property progress a fraction between 0.0 and 1.0, not a 0-100 percentage
 * @property downloadSpeed in bytes/s
 * @property eta in seconds
 * @property downloadPresent true as soon as at least one file can be downloaded
 * @property downloadFinished true once the whole torrent finished downloading
 * @property files
 * @property cached
 * @property expiresAt
 * @constructor Create empty Torrent item
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class TorrentItem(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "hash") val hash: String,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "magnet") val magnet: String?,
    @param:Json(name = "size") val size: Long,
    @param:Json(name = "active") val active: Boolean,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "download_state") val downloadState: String,
    @param:Json(name = "seeds") val seeds: Int?,
    @param:Json(name = "peers") val peers: Int?,
    @param:Json(name = "progress") val progress: Float,
    @param:Json(name = "download_speed") val downloadSpeed: Long?,
    @param:Json(name = "eta") val eta: Long?,
    @param:Json(name = "download_present") val downloadPresent: Boolean,
    @param:Json(name = "download_finished") val downloadFinished: Boolean,
    @param:Json(name = "files") val files: List<InnerTorrentFile>?,
    @param:Json(name = "cached") val cached: Boolean?,
    @param:Json(name = "expires_at") val expiresAt: String?,
) : Parcelable

@JsonClass(generateAdapter = true)
@Parcelize
data class InnerTorrentFile(
    @param:Json(name = "id") val id: Long,
    /** Path relative to the torrent's root, e.g. "SubFolder/file.mkv" - no leading slash. */
    @param:Json(name = "name") val name: String,
    @param:Json(name = "short_name") val shortName: String,
    @param:Json(name = "absolute_path") val absolutePath: String,
    @param:Json(name = "size") val size: Long,
    @param:Json(name = "mimetype") val mimetype: String?,
) : Parcelable

/** Response of `POST torrents/createtorrent`. */
@JsonClass(generateAdapter = true)
@Parcelize
data class CreatedTorrent(
    @param:Json(name = "hash") val hash: String,
    @param:Json(name = "torrent_id") val torrentId: Long,
    @param:Json(name = "auth_id") val authId: String?,
) : Parcelable

/** Body of `POST torrents/controltorrent` and `POST webdl/controlwebdownload`. */
@JsonClass(generateAdapter = true)
data class ItemControlRequest(
    @param:Json(name = "torrent_id") val torrentId: Long? = null,
    @param:Json(name = "web_id") val webId: Long? = null,
    @param:Json(name = "operation") val operation: String,
    @param:Json(name = "all") val all: Boolean = false,
)

object TorrentOperation {
    const val REANNOUNCE = "reannounce"
    const val DELETE = "delete"
    const val RESUME = "resume"
    const val STOP_SEEDING = "stop_seeding"
}
