package com.github.livingwithhippos.unchained.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET user/auth/device/start`, the first step of TorBox's device-code login flow.
 * `deviceCode` is valid for 10 minutes; poll `user/auth/device/token` with it every [interval]
 * seconds until the user authorizes it on [verificationUrl]/[friendlyVerificationUrl].
 */
@JsonClass(generateAdapter = true)
data class DeviceAuthStart(
    @param:Json(name = "device_code") val deviceCode: String,
    @param:Json(name = "interval") val interval: Int,
    @param:Json(name = "expires_at") val expiresAt: String?,
    @param:Json(name = "verification_url") val verificationUrl: String,
    @param:Json(name = "friendly_verification_url") val friendlyVerificationUrl: String?,
    @param:Json(name = "code") val code: String,
)

/**
 * Response of `POST user/auth/device/token`. Unlike RD's Token, this has no `expires_in`/
 * `refresh_token` - the returned [accessToken] is a permanent API key.
 */
@JsonClass(generateAdapter = true)
data class DeviceToken(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "token_type") val tokenType: String?,
)

enum class UserAction {
    UNKNOWN,
    NETWORK_ERROR,
    RETRY_LATER,
}
