package com.github.livingwithhippos.unchained.authentication.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.authentication.viewmodel.AuthenticationViewModel
import com.github.livingwithhippos.unchained.authentication.viewmodel.TokenPollResult
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentAuthenticationBinding
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.AUTH_METHOD_DEVICE_FLOW
import com.github.livingwithhippos.unchained.utilities.AUTH_METHOD_MANUAL
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.hideKeyboard
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

/**
 * A simple [UnchainedFragment] subclass. It is capable of authenticating a user either by pasting
 * their TorBox API key directly, or through TorBox's device-code login flow.
 */
@AndroidEntryPoint
class AuthenticationFragment : UnchainedFragment() {

    private val viewModel: AuthenticationViewModel by viewModels()
    private var _binding: FragmentAuthenticationBinding? = null
    private val binding
        get() = _binding!!

    private var pollIntervalSeconds: Int = 5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentAuthenticationBinding.inflate(inflater, container, false)

        binding.bPastePrivateCode.setOnClickListener {
            val pasteText = getClipboardText()
            binding.tiPrivateCode.setText(pasteText, TextView.BufferType.EDITABLE)
            binding.tiPrivateCode.hideKeyboard()
        }

        binding.tiPrivateCode.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                v.hideKeyboard()
            }
        }

        binding.bInsertPrivate.setOnClickListener { onSaveCodeClick(binding.tiPrivateCode) }

        binding.bCopyLink.setOnClickListener {
            copyToClipboard(
                getString(R.string.code_copied),
                binding.tvUserCodeValue.text.toString(),
            )
        }

        activityViewModel.fsmAuthenticationState.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it.peekContent()) {
                    FSMAuthenticationState.Authenticated -> {
                        val action = AuthenticationFragmentDirections.actionAuthenticationToUser()
                        findNavController().navigate(action)
                    }

                    FSMAuthenticationState.StartNewLogin -> {
                        // reset the current data
                        binding.cbToken.isChecked = false
                        binding.cbToken.text = getString(R.string.waiting_token)
                        binding.tvAuthenticationLink.text = ""
                        binding.tvAuthenticationLink.visibility = View.GONE
                        binding.cbLink.isChecked = false
                        binding.cbLink.text = getString(R.string.waiting_link)
                        binding.tvUserCodeValue.text = getString(R.string.copy_code)
                        binding.bCopyLink.isEnabled = false

                        // get the device code to start the process
                        viewModel.fetchAuthenticationInfo()
                    }

                    FSMAuthenticationState.WaitingUserConfirmation -> {
                        // keep polling for the token, at the interval TorBox told us to use
                        viewModel.pollToken(pollIntervalSeconds)
                    }

                    FSMAuthenticationState.CheckCredentials -> {
                        // managed by activity
                    }

                    is FSMAuthenticationState.WaitingUserAction -> {
                        // todo: depending on the action required show an error or restart the
                        // process
                    }

                    FSMAuthenticationState.Start -> {
                        // this shouldn't happen
                    }
                }
            }
        }

        // 1. start checking for the device code / auth link
        viewModel.authLiveData.observe(viewLifecycleOwner) { event ->
            event?.peekContent()?.let { auth ->
                binding.tvAuthenticationLink.text = auth.verificationUrl
                binding.tvAuthenticationLink.visibility = View.VISIBLE
                binding.cbLink.isChecked = true
                binding.cbLink.text = getString(R.string.link_loaded)
                // let the user copy the code to enter in the website
                binding.tvUserCodeValue.text = auth.code
                binding.bCopyLink.isEnabled = true
                pollIntervalSeconds = auth.interval
                // transition state machine
                if (
                    activityViewModel.getAuthenticationMachineState()
                        is FSMAuthenticationState.StartNewLogin
                ) {
                    activityViewModel.transitionAuthenticationMachine(
                        FSMAuthenticationEvent.OnAuthLoaded
                    )
                }
            }
        }

        // 2. keep polling for the token
        viewModel.tokenLiveData.observe(
            viewLifecycleOwner,
            EventObserver { result ->
                if (
                    activityViewModel.getAuthenticationMachineState()
                        !is FSMAuthenticationState.WaitingUserConfirmation
                )
                    return@EventObserver
                when (result) {
                    TokenPollResult.Waiting -> {
                        // will launch another poll, re-entering WaitingUserConfirmation
                        activityViewModel.transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserConfirmationMissing
                        )
                    }
                    TokenPollResult.Expired -> {
                        // will restart the authentication process
                        activityViewModel.transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnUserConfirmationExpired
                        )
                    }
                    is TokenPollResult.Retrieved -> {
                        binding.cbToken.isChecked = true
                        binding.cbToken.text = getString(R.string.obtained_token)
                        activityViewModel.saveNewCredentials(
                            result.accessToken,
                            AUTH_METHOD_DEVICE_FLOW,
                        )
                        activityViewModel.transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnTokenSaved
                        )
                    }
                }
            },
        )

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onSaveCodeClick(codeInputField: TextInputEditText) {
        val token: String = codeInputField.text.toString().trim()
        if (token.length < MIN_TOKEN_LENGTH) context?.showToast(R.string.invalid_token)
        else {
            // pass the value to be checked and eventually saved
            activityViewModel.saveNewCredentials(token, AUTH_METHOD_MANUAL)
            activityViewModel.transitionAuthenticationMachine(FSMAuthenticationEvent.OnTokenSaved)
        }
    }

    companion object {
        // TorBox API keys are UUID-shaped, e.g. "00000000-0000-0000-0000-000000000000" (36 chars)
        const val MIN_TOKEN_LENGTH = 30
    }
}
