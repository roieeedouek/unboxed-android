package com.github.livingwithhippos.unchained.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

/**
 * A webdl (hoster link) job, from `webdl/mylist`. TorBox's docs describe `/webdl` as "nearly the
 * same as `/torrents` apart from some different named inputs and outputs" - this mirrors
 * [TorrentItem]'s shape, adding [originalUrl] for the link the user originally pasted.
 */
@JsonClass(generateAdapter = true)
@Parcelize
data class WebDownloadItem(
    @param:Json(name = "id") val id: Long,
    @param:Json(name = "hash") val hash: String?,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "size") val size: Long,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "download_state") val downloadState: String,
    @param:Json(name = "download_speed") val downloadSpeed: Long?,
    @param:Json(name = "progress") val progress: Float,
    @param:Json(name = "eta") val eta: Long?,
    @param:Json(name = "download_present") val downloadPresent: Boolean,
    @param:Json(name = "download_finished") val downloadFinished: Boolean,
    @param:Json(name = "files") val files: List<InnerTorrentFile>?,
    @param:Json(name = "original_url") val originalUrl: String?,
    @param:Json(name = "cached") val cached: Boolean?,
) : Parcelable

/** Response of `POST webdl/createwebdownload`. */
@JsonClass(generateAdapter = true)
@Parcelize
data class CreatedWebDownload(
    @param:Json(name = "hash") val hash: String?,
    @param:Json(name = "webdownload_id") val webDownloadId: Long,
    @param:Json(name = "auth_id") val authId: String?,
) : Parcelable
