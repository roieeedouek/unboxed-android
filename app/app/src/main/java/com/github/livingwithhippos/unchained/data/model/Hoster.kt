package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A supported hoster, from `GET webdl/hosters`. Replaces RD's `hosts/regex` + `availableHosts`:
 * TorBox already provides a ready-made [regex] per hoster, so no client-side regex construction
 * is needed to detect whether a pasted link is a supported hoster link.
 */
@JsonClass(generateAdapter = true)
data class Hoster(
    @param:Json(name = "id") val id: Int,
    @param:Json(name = "name") val name: String,
    @param:Json(name = "domains") val domains: List<String>?,
    @param:Json(name = "url") val url: String?,
    @param:Json(name = "icon") val icon: String?,
    @param:Json(name = "status") val status: Boolean,
    @param:Json(name = "type") val type: String?,
    @param:Json(name = "nsfw") val nsfw: Boolean?,
    @param:Json(name = "daily_link_limit") val dailyLinkLimit: Long?,
    @param:Json(name = "daily_bandwidth_limit") val dailyBandwidthLimit: Long?,
    @param:Json(name = "per_link_size_limit") val perLinkSizeLimit: Long?,
    @param:Json(name = "regex") val regex: String,
)
