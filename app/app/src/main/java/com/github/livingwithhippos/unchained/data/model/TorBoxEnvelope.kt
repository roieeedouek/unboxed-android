package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TorBox wraps every response, success or failure, in this envelope. `success` and the HTTP
 * status code both indicate the outcome; `error` is a short machine-readable code (e.g.
 * "BAD_TOKEN"); `detail` is a user-friendly message that's safe to show directly; `data` holds the
 * actual payload, and is null on failure (and on some empty-body successes, e.g. controltorrent).
 */
@JsonClass(generateAdapter = true)
data class TorBoxEnvelope<T>(
    @param:Json(name = "success") val success: Boolean,
    @param:Json(name = "error") val error: String?,
    @param:Json(name = "detail") val detail: String?,
    @param:Json(name = "data") val data: T?,
)

/**
 * Minimal shape used to parse a TorBox error envelope's body when we don't know/care about the
 * (possibly absent) `data` type, e.g. when Retrofit hands us a raw error body to parse manually.
 */
@JsonClass(generateAdapter = true)
data class TorBoxErrorBody(
    @param:Json(name = "success") val success: Boolean,
    @param:Json(name = "error") val error: String?,
    @param:Json(name = "detail") val detail: String?,
)
