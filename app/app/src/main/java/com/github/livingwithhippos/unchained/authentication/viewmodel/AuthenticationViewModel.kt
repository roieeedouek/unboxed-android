package com.github.livingwithhippos.unchained.authentication.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.DeviceAuthStart
import com.github.livingwithhippos.unchained.data.model.TorBoxApiError
import com.github.livingwithhippos.unchained.data.repository.AuthenticationRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass. It offers LiveData to be observed during the device-code authentication
 * process and uses the [AuthenticationRepository] to manage it.
 *
 * Unlike RD's 3-step OAuth flow (link -> secrets -> token), TorBox's device flow is 2 steps: get a
 * device code ([fetchAuthenticationInfo]), then poll for the resulting permanent token
 * ([pollToken]) until the user authorizes it on TorBox's site.
 */
@HiltViewModel
class AuthenticationViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthenticationRepository,
) : ViewModel() {

    val authLiveData = MutableLiveData<Event<DeviceAuthStart?>>()
    val tokenLiveData = MutableLiveData<Event<TokenPollResult>>()

    fun fetchAuthenticationInfo() {
        viewModelScope.launch {
            val authData = authRepository.startDeviceAuth(APP_NAME)
            if (authData != null) savedStateHandle[KEY_DEVICE_CODE] = authData.deviceCode
            authLiveData.postEvent(authData)
        }
    }

    /** Polls once for the token, waiting [interval] seconds beforehand as TorBox asks. */
    fun pollToken(interval: Int) {
        val deviceCode = savedStateHandle.get<String>(KEY_DEVICE_CODE)
        if (deviceCode.isNullOrBlank()) {
            tokenLiveData.postEvent(TokenPollResult.Expired)
            return
        }
        viewModelScope.launch {
            delay(interval.seconds)
            when (val result = authRepository.pollDeviceToken(deviceCode)) {
                is EitherResult.Success -> {
                    tokenLiveData.postEvent(TokenPollResult.Retrieved(result.success.accessToken))
                }
                is EitherResult.Failure -> {
                    val error = (result.failure as? TorBoxApiError)?.error
                    if (error == "ITEM_NOT_FOUND") {
                        tokenLiveData.postEvent(TokenPollResult.Expired)
                    } else {
                        // includes "DEVICE_CODE_NOT_USED": the user hasn't confirmed yet
                        tokenLiveData.postEvent(TokenPollResult.Waiting)
                    }
                }
            }
        }
    }

    companion object {
        const val APP_NAME = "Unchained"
        const val KEY_DEVICE_CODE = "device_code_key"
    }
}

sealed class TokenPollResult {
    data object Waiting : TokenPollResult()

    data object Expired : TokenPollResult()

    data class Retrieved(val accessToken: String) : TokenPollResult()
}
