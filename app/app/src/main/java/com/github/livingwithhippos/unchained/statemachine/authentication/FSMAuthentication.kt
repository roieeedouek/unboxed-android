package com.github.livingwithhippos.unchained.statemachine.authentication

import com.github.livingwithhippos.unchained.data.model.UserAction

sealed class FSMAuthenticationState {
    data object Start : FSMAuthenticationState()

    data object CheckCredentials : FSMAuthenticationState()

    data class WaitingUserAction(val action: UserAction?) : FSMAuthenticationState()

    data object StartNewLogin : FSMAuthenticationState()

    /**
     * The device-code was obtained and the app is polling TorBox for the resulting token, waiting
     * for the user to authorize the code on TorBox's site. TorBox's device flow has no separate
     * "secrets" step like RD's, so unlike RD there's no further "waiting for token" state after
     * this one: the same poll either returns the token or reports the user hasn't confirmed yet.
     */
    data object WaitingUserConfirmation : FSMAuthenticationState()

    /**
     * TorBox tokens are permanent (no refresh grant for 3rd-party apps), whether obtained by
     * pasting one manually or through the device-code flow - so unlike RD there's only one
     * "logged in" state, not a split Open/Private pair plus a Refreshing state.
     */
    data object Authenticated : FSMAuthenticationState()
}

sealed class FSMAuthenticationEvent {
    data object OnAvailableCredentials : FSMAuthenticationEvent()

    data object OnMissingCredentials : FSMAuthenticationEvent()

    data object OnNotWorking : FSMAuthenticationEvent()

    data object OnAuthLoaded : FSMAuthenticationEvent()

    data class OnUserActionNeeded(val action: UserAction) : FSMAuthenticationEvent()

    data object OnUserActionRetry : FSMAuthenticationEvent()

    data object OnUserActionReset : FSMAuthenticationEvent()

    /** The user pasted a token manually, or the device-code poll obtained one - go verify it. */
    data object OnTokenSaved : FSMAuthenticationEvent()

    /** The device-code poll came back with "not confirmed yet" - keep polling. */
    data object OnUserConfirmationMissing : FSMAuthenticationEvent()

    /** The device-code expired before the user confirmed it - restart the login. */
    data object OnUserConfirmationExpired : FSMAuthenticationEvent()

    data object OnWorking : FSMAuthenticationEvent()

    data object OnLogout : FSMAuthenticationEvent()

    data object OnAuthenticationError : FSMAuthenticationEvent()
}

sealed class FSMAuthenticationSideEffect {
    data object CheckingCredentials : FSMAuthenticationSideEffect()

    data object PostActionNeeded : FSMAuthenticationSideEffect()

    data object PostNewLogin : FSMAuthenticationSideEffect()

    data object ResetAuthentication : FSMAuthenticationSideEffect()

    data object PostWaitUserConfirmation : FSMAuthenticationSideEffect()

    data object PostAuthenticated : FSMAuthenticationSideEffect()
}

// support class
sealed class CurrentFSMAuthentication {
    // auth is ok
    data object Authenticated : CurrentFSMAuthentication()

    // auth may become ok
    data object Waiting : CurrentFSMAuthentication()

    // auth is not ok
    data object Unauthenticated : CurrentFSMAuthentication()
}
