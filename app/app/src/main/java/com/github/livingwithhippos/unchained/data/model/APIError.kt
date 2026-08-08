package com.github.livingwithhippos.unchained.data.model

/**
 * A structured, non-2xx (or `success:false`) TorBox response, e.g.
 * `{"success":false,"error":"BAD_TOKEN","detail":"Your token is invalid or has expired."}`.
 *
 * @property error TorBox's short machine-readable error code, e.g. "BAD_TOKEN". Null if the
 *   server didn't provide one.
 * @property detail a user-friendly message, safe to show directly, per TorBox's docs.
 */
data class TorBoxApiError(val error: String?, val detail: String?) : UnchainedNetworkException

/** A 2xx response whose envelope reported success but carried a null `data` payload. */
data class EmptyBodyError(val returnCode: Int) : UnchainedNetworkException

data class NetworkError(val error: Int, val message: String) : UnchainedNetworkException

data class ApiConversionError(val error: Int) : UnchainedNetworkException

interface UnchainedNetworkException
