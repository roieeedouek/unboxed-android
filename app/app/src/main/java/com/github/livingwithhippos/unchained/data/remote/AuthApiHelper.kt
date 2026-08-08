package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DeviceAuthStart
import com.github.livingwithhippos.unchained.data.model.DeviceToken
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import retrofit2.Response

interface AuthApiHelper {

    suspend fun startDeviceAuth(app: String): Response<TorBoxEnvelope<DeviceAuthStart>>

    suspend fun pollDeviceToken(deviceCode: String): Response<TorBoxEnvelope<DeviceToken>>
}
