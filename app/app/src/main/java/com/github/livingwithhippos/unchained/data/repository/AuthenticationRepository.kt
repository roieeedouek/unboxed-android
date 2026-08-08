package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.DeviceAuthStart
import com.github.livingwithhippos.unchained.data.model.DeviceToken
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.AuthApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject

class AuthenticationRepository
@Inject
constructor(protoStore: ProtoStore, private val apiHelper: AuthApiHelper) :
    BaseRepository(protoStore) {

    /** Starts the device-code login flow. */
    suspend fun startDeviceAuth(appName: String): DeviceAuthStart? =
        safeApiCall(
            call = { apiHelper.startDeviceAuth(appName) },
            errorMessage = "Error Starting Device Authentication",
        )

    /**
     * Polls for the permanent access token once the user has authorized [deviceCode] on TorBox's
     * site. Expect [com.github.livingwithhippos.unchained.data.model.TorBoxApiError] with
     * error "DEVICE_CODE_NOT_USED" while the user hasn't finished yet - keep polling in that case.
     */
    suspend fun pollDeviceToken(
        deviceCode: String
    ): EitherResult<UnchainedNetworkException, DeviceToken> =
        eitherApiResult(
            call = { apiHelper.pollDeviceToken(deviceCode) },
            errorMessage = "Error Polling Device Token",
        )
}
