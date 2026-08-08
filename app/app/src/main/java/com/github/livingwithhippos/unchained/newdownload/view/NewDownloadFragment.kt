package com.github.livingwithhippos.unchained.newdownload.view

import android.annotation.SuppressLint
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorBoxApiError
import com.github.livingwithhippos.unchained.databinding.NewDownloadFragmentBinding
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.newdownload.viewmodel.Link
import com.github.livingwithhippos.unchained.newdownload.viewmodel.NewDownloadViewModel
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTP
import com.github.livingwithhippos.unchained.utilities.SCHEME_HTTPS
import com.github.livingwithhippos.unchained.utilities.SCHEME_MAGNET
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.getClipboardText
import com.github.livingwithhippos.unchained.utilities.extension.getDownloadedFileUri
import com.github.livingwithhippos.unchained.utilities.extension.getFileName
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isSimpleWebUrl
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A simple [UnchainedFragment] subclass. Allow the user to create a new download from a link or a
 * torrent file.
 */
@AndroidEntryPoint
class NewDownloadFragment : UnchainedFragment() {

    // if we receive an intent and new download is already selected and showing a
    // DownloadDetailsFragment, it may not trigger the observers in this class
    private val viewModel: NewDownloadViewModel by viewModels()

    private val args: NewDownloadFragmentArgs by navArgs()
    private var _binding: NewDownloadFragmentBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = NewDownloadFragmentBinding.inflate(inflater, container, false)

        setupObservers(binding)
        setupClickListeners(binding)
        setupArgs(binding)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers(binding: NewDownloadFragmentBinding) {

        viewModel.downloadLiveData.observe(
            viewLifecycleOwner,
            EventObserver { linkDetails ->
                // new download item, alert the list fragment that it needs updating
                activityViewModel.setListState(ListState.UpdateDownload)
                val action =
                    NewDownloadFragmentDirections.actionUnrestrictDownloadToDetailsFragment(
                        linkDetails
                    )
                findNavController().navigate(action)
            },
        )

        viewModel.webDownloadReadyLiveData.observe(
            viewLifecycleOwner,
            EventObserver { webDownload ->
                // multi-file result, alert the list fragment that it needs updating
                activityViewModel.setListState(ListState.UpdateDownload)
                val action =
                    NewDownloadFragmentDirections.actionNewDownloadDestToFolderListFragment(
                        torrent = null,
                        webDownload = webDownload,
                    )
                findNavController().navigate(action)
            },
        )

        viewModel.linkLiveData.observe(
            viewLifecycleOwner,
            EventObserver { link ->
                when (link) {
                    is Link.Torrent -> {
                        val action =
                            NewDownloadFragmentDirections
                                .actionNewDownloadFragmentToTorrentProcessingFragment(
                                    torrentID = link.created.torrentId.toString()
                                )
                        findNavController().navigate(action)
                    }
                }
            },
        )

        activityViewModel.downloadedFileLiveData.observe(
            viewLifecycleOwner,
            EventObserver { fileID ->
                val uri = requireContext().getDownloadedFileUri(fileID)
                // no need to recheck the extension since it was checked on download
                // if (uri?.path?.endsWith(".torrent") == true)
                if (uri?.path != null) loadTorrent(binding, uri)
            },
        )

        viewModel.networkExceptionLiveData.observe(
            viewLifecycleOwner,
            EventObserver { exception ->

                // re-enable the buttons to allow the user to take new actions
                enableButtons(binding, true)

                when (exception) {
                    is TorBoxApiError -> {
                        val errorMessage =
                            requireContext().getApiErrorMessage(exception.error, exception.detail)
                        when (exception.error) {
                            "UNSUPPORTED_SITE" ->
                                viewModel.postMessage(getString(R.string.error_unsupported_site))
                            "BAD_TOKEN",
                            "NO_AUTH",
                            "AUTH_ERROR" -> {
                                activityViewModel.transitionAuthenticationMachine(
                                    FSMAuthenticationEvent.OnNotWorking
                                )
                            }
                            "PLAN_RESTRICTED_FEATURE" -> {
                                viewModel.postMessage(getString(R.string.premium_needed))
                            }
                            else -> {
                                viewModel.postMessage(errorMessage)
                            }
                        }
                    }

                    is EmptyBodyError -> {
                        // call successful, fit to singular api case
                    }

                    is NetworkError -> {
                        // todo: alert the user according to the different network error
                        viewModel.postMessage(getString(R.string.network_error))
                    }

                    else -> {
                        viewModel.postMessage(getString(R.string.unknown_error))
                    }
                }
            },
        )

        @SuppressLint("ShowToast")
        val currentToast: Toast = Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT)
        var lastToastTime = System.currentTimeMillis()

        viewModel.toastLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                lifecycleScope.launch {
                    currentToast.cancel()
                    // if we call this too soon between toasts we'll miss some
                    if (System.currentTimeMillis() - lastToastTime < 1000L) delay(1000.milliseconds)
                    currentToast.setText(it)
                    currentToast.show()
                    lastToastTime = System.currentTimeMillis()
                }
            },
        )
    }

    private fun setupClickListeners(binding: NewDownloadFragmentBinding) {
        // add the unrestrict button listener
        binding.bUnrestrict.setOnClickListener {
            val authState = activityViewModel.getAuthenticationMachineState()
            if (authState is FSMAuthenticationState.Authenticated) {
                val link: String = binding.tiLink.text.toString().trim()

                val splitLinks: List<String> =
                    link
                        .split("\n")
                        .dropWhile { it.isBlank() }
                        .map { it.trim() }
                        .filter {
                            it.length > 10 &&
                                (it.isTorrent() || it.isMagnet() || it.isWebUrl() || it.isSimpleWebUrl())
                        }

                if (splitLinks.isEmpty()) {
                    Timber.w("Invalid link: $link")
                    viewModel.postMessage(getString(R.string.invalid_url))
                    return@setOnClickListener
                }

                if (splitLinks.size == 1) {
                    val link = splitLinks.first()

                    when {
                        // this must be before the link.isWebUrl() check or it won't trigger
                        link.isTorrent() -> {
                            val action =
                                NewDownloadFragmentDirections
                                    .actionNewDownloadFragmentToTorrentProcessingFragment(
                                        link = link
                                    )
                            findNavController().navigate(action)
                        }

                        link.isMagnet() -> {
                            // this one must stay above link.isWebUrl() || link.isSimpleWebUrl()
                            // because some magnets have http in their link, getting recognized as
                            // urls
                            val action =
                                NewDownloadFragmentDirections
                                    .actionNewDownloadFragmentToTorrentProcessingFragment(
                                        link = link
                                    )
                            findNavController().navigate(action)
                        }

                        link.isWebUrl() || link.isSimpleWebUrl() -> {
                            viewModel.postMessage(getString(R.string.loading_host_link))
                            enableButtons(binding, false)

                            var password: String? = binding.tePassword.text.toString()
                            // we don't pass the password if it is blank.
                            // N.B. it won't work if your password is made up of spaces but then
                            // again
                            // you deserve it
                            if (password.isNullOrBlank()) password = null

                            viewModel.fetchUnrestrictedLink(link, password)
                        }

                        else -> {
                            Timber.w("Invalid link: $link")
                            viewModel.postMessage(getString(R.string.invalid_url))
                        }
                    }

                    return@setOnClickListener
                }

                val multipleLinks: List<String> = splitLinks.filter {
                    it.isWebUrl() || it.isSimpleWebUrl()
                }

                if (multipleLinks.isEmpty()) {
                    Timber.w("Invalid link: $link")
                    viewModel.postMessage(getString(R.string.invalid_url))
                    return@setOnClickListener
                }

                var password: String? = binding.tePassword.text.toString()
                if (password.isNullOrBlank()) password = null

                // each link resolves independently (create+poll+requestdl); fire them off and let
                // the user check the Downloads tab rather than trying to show them all at once
                viewModel.submitLinks(multipleLinks, password)
            } else viewModel.postMessage(getString(R.string.premium_needed))
        }

        binding.bPasteLink.setOnClickListener {
            val pasteText = getClipboardText().trim()

            if (
                pasteText.isWebUrl() ||
                    pasteText.isSimpleWebUrl() ||
                    pasteText.isMagnet() ||
                    pasteText.isTorrent() ||
                    pasteText.split("\n").firstOrNull()?.trim()?.isWebUrl() == true
            )
                binding.tiLink.setText(pasteText, TextView.BufferType.EDITABLE)
            else {
                Timber.w("Invalid pasted link: $pasteText")
                viewModel.postMessage(getString(R.string.invalid_url))
            }
        }

        binding.bPastePassword.setOnClickListener {
            val pasteText = getClipboardText()
            binding.tePassword.setText(pasteText, TextView.BufferType.EDITABLE)
        }

        val filePicker: ActivityResultLauncher<String> =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    val fileName = uri.getFileName(requireContext())
                    if (fileName.endsWith(".torrent", ignoreCase = true)) loadTorrent(binding, uri)
                    else viewModel.postMessage(getString(R.string.unsupported_file))
                }
                /*
                * if it's null the user didn't pick a file, no message needed
                else {
                context?.showToast(R.string.error_loading_file)
                }
                 */
            }

        binding.bUploadFile.setOnClickListener {
            when (activityViewModel.getAuthenticationMachineState()) {
                FSMAuthenticationState.Authenticated -> {
                    filePicker.launch("*/*")
                }

                else -> {
                    viewModel.postMessage(getString(R.string.premium_needed))
                }
            }
        }
    }

    private fun setupArgs(binding: NewDownloadFragmentBinding) {

        args.externalUri?.let { link ->
            when (link.scheme) {
                SCHEME_MAGNET -> {
                    viewModel.postMessage(getString(R.string.loading_magnet_link))
                    // set as text input text
                    binding.tiLink.setText(link.toString(), TextView.BufferType.EDITABLE)
                    // simulate button click
                    binding.bUnrestrict.performClick()
                }

                SCHEME_CONTENT,
                SCHEME_FILE -> {

                    var handled = false

                    requireContext()
                        .contentResolver
                        .query(
                            link,
                            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                            null,
                            null,
                            null,
                        )
                        ?.use { metaCursor ->
                            if (metaCursor.moveToFirst()) {
                                val fileName = metaCursor.getString(0)
                                Timber.d("Torrent shared file found: $fileName")
                                if (fileName.endsWith(".torrent", ignoreCase = true)) {
                                    handled = true
                                    loadTorrent(binding, link)
                                }
                            }
                        }

                    if (!handled) {
                        if (link.path?.endsWith(".torrent", ignoreCase = true) == true) {
                            loadTorrent(binding, link)
                        } else {
                            Timber.e(
                                "Unsupported content/file passed to NewDownloadFragment: $link"
                            )
                        }
                    }
                }

                SCHEME_HTTP,
                SCHEME_HTTPS -> {
                    // set as text input text
                    binding.tiLink.setText(link.toString(), TextView.BufferType.EDITABLE)
                    // simulate button click
                    binding.bUnrestrict.performClick()
                }

                else -> {
                    // shouldn't trigger
                    Timber.e(
                        "Unknown Uri shared to NewDownloadFragment: ${link.scheme} - ${link.path}"
                    )
                }
            }
        }
    }

    private fun loadTorrent(binding: NewDownloadFragmentBinding, uri: Uri) {
        // https://developer.android.com/training/data-storage/shared/documents-files#open
        try {
            viewModel.postMessage(getString(R.string.loading_torrent_file))
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.fetchUploadedTorrent(buffer)
            }
        } catch (exception: Exception) {
            when (exception) {
                is java.io.FileNotFoundException -> {
                    Timber.e("Torrent conversion: file not found: ${exception.message}")
                }

                is IOException -> {
                    Timber.e(
                        "Torrent conversion: IOException error getting the file: ${exception.message}"
                    )
                }

                else -> {
                    Timber.e(
                        "Torrent conversion: Other error getting the file: ${exception.message}"
                    )
                }
            }
            enableButtons(binding, true)
            viewModel.postMessage(getString(R.string.error_loading_torrent))
        }
    }

    private fun enableButtons(binding: NewDownloadFragmentBinding, enabled: Boolean = true) {
        binding.bUnrestrict.isEnabled = enabled
        binding.bUploadFile.isEnabled = enabled
    }
}
