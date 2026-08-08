package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DeviceAuthStart
import com.github.livingwithhippos.unchained.data.model.DeviceToken
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import javax.inject.Inject
import retrofit2.Response

class AuthApiHelperImpl @Inject constructor(private val authenticationApi: AuthenticationApi) :
    AuthApiHelper {

    override suspend fun startDeviceAuth(app: String): Response<TorBoxEnvelope<DeviceAuthStart>> =
        authenticationApi.startDeviceAuth(app)

    override suspend fun pollDeviceToken(
        deviceCode: String
    ): Response<TorBoxEnvelope<DeviceToken>> =
        authenticationApi.pollDeviceToken(DeviceTokenRequest(deviceCode))
}
