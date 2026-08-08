package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DeviceAuthStart
import com.github.livingwithhippos.unchained.data.model.DeviceToken
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class DeviceTokenRequest(@param:Json(name = "device_code") val deviceCode: String)

/**
 * This interface is used by Retrofit to manage all the REST calls to the endpoints needed to
 * authenticate the user through TorBox's device-code flow. Neither endpoint requires auth.
 */
interface AuthenticationApi {

    @GET("user/auth/device/start")
    suspend fun startDeviceAuth(
        @Query("app") app: String
    ): Response<TorBoxEnvelope<DeviceAuthStart>>

    @POST("user/auth/device/token")
    suspend fun pollDeviceToken(
        @Body body: DeviceTokenRequest
    ): Response<TorBoxEnvelope<DeviceToken>>
}
