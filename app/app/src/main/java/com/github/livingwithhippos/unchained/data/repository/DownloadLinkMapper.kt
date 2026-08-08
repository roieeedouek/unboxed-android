package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.InnerTorrentFile

private val videoExtensions =
    setOf("3g2", "3gp", "avi", "flv", "m4v", "mkv", "mov", "mp4", "mpg", "mpeg", "rm", "vob", "wmv")
private val audioExtensions =
    setOf("aif", "cda", "mid", "midi", "mp3", "mpa", "ogg", "wav", "wma")

/**
 * TorBox's `requestdl` only returns a bare URL - not a filename/size/mimetype - so a [DownloadItem]
 * has to be assembled locally from the matching file's metadata (from `mylist`) plus the minted
 * [link]. [file] is null for a `zip_link=true` request, where there's no single matching file (and
 * [fileId] should be passed as `0`, since streaming a zip isn't supported).
 */
fun buildDownloadItem(
    id: String,
    contentId: Long,
    fileId: Long,
    contentType: String,
    file: InnerTorrentFile?,
    link: String,
    host: String,
    hostIcon: String? = null,
): DownloadItem {
    val filename = file?.shortName ?: link.substringAfterLast('/').substringBefore('?')
    val extension = filename.substringAfterLast('.', "").lowercase()
    val mimeType = file?.mimetype ?: guessMimeType(extension)
    val streamable = if (extension in videoExtensions || extension in audioExtensions) 1 else 0

    return DownloadItem(
        id = id,
        contentId = contentId,
        fileId = fileId,
        contentType = contentType,
        filename = filename,
        mimeType = mimeType,
        fileSize = file?.size ?: 0L,
        link = link,
        host = host,
        hostIcon = hostIcon,
        streamable = streamable,
        alternative = null,
    )
}

private fun guessMimeType(extension: String): String? =
    when (extension) {
        in videoExtensions -> "video/$extension"
        in audioExtensions -> "audio/$extension"
        "jpg",
        "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        "txt" -> "text/plain"
        "" -> null
        else -> null
    }
