package com.github.livingwithhippos.unchained.torrentfilepicker.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorBoxApiError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repository.DownloadResult
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentProcessingBinding
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel.TorrentEvent
import com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel.TorrentProcessingViewModel
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.isMagnet
import com.github.livingwithhippos.unchained.utilities.extension.isTorrent
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern
import timber.log.Timber

/** This fragments is shown after a user uploads a torrent or a magnet. */
@AndroidEntryPoint
class TorrentProcessingFragment : UnchainedFragment() {

    private val args: TorrentProcessingFragmentArgs by navArgs()

    // https://developer.android.com/training/dependency-injection/hilt-jetpack#viewmodel-navigation
    private val viewModel: TorrentProcessingViewModel by
        hiltNavGraphViewModels(R.id.navigation_lists)

    private var _binding: FragmentTorrentProcessingBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding
        get() = _binding!!

    /** Save the torrent/magnet has when loaded */
    private var torrentHash: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTorrentProcessingBinding.inflate(inflater, container, false)

        setup(binding)

        viewModel.torrentLiveData.observe(viewLifecycleOwner) {
            when (val content = it.getContentIfNotHandled()) {
                is TorrentEvent.Created -> {
                    binding.tvStatus.text = getString(R.string.loading_torrent)
                    // get torrent info
                    viewModel.fetchTorrentDetails(content.torrent.torrentId.toString())
                    // todo: add a loop so this is repeated if it fails, instead of wasting the
                    // fragment
                }

                is TorrentEvent.TorrentInfo -> {
                    torrentHash = content.item.hash
                    // TorBox has no file-selection step: every file downloads automatically, so
                    // there's nothing to wait for beyond the torrent's metadata (its file list)
                    // being populated.
                    if (content.item.files.isNullOrEmpty()) {
                        viewModel.startMetadataPollLoop()
                    } else {
                        navigateToDetails(content.item)
                    }
                }

                is TorrentEvent.FilesReady -> {
                    activityViewModel.setListState(ListState.UpdateTorrent)
                    navigateToDetails(content.torrent)
                }

                TorrentEvent.DownloadedFileFailure -> {
                    binding.tvStatus.text = getString(R.string.error_loading_torrent)
                    binding.tvLoadingTorrent.visibility = View.INVISIBLE
                    binding.loadingCircle.isIndeterminate = false
                    binding.loadingCircle.progress = 100
                    binding.loadingLayout.visibility = View.VISIBLE
                    binding.loadedLayout.visibility = View.INVISIBLE
                }

                is TorrentEvent.DownloadedFileProgress -> {
                    binding.tvStatus.text = getString(R.string.downloading_torrent)
                    binding.loadingCircle.isIndeterminate = false
                    binding.loadingCircle.progress = content.progress
                }

                TorrentEvent.DownloadedFileSuccess -> {
                    // do nothing
                }

                else -> {
                    Timber.d("Found unknown torrentLiveData event $content")
                    // reloaded fragment, close?
                }
            }
        }

        viewModel.networkExceptionLiveData.observe(viewLifecycleOwner) {
            when (val response = it.getContentIfNotHandled()) {
                null -> {}
                is TorBoxApiError -> {
                    Timber.e("API error: ${response.error}")
                    if (response.error == "BAD_TOKEN" || response.error == "NO_AUTH") {
                        activityViewModel.transitionAuthenticationMachine(
                            FSMAuthenticationEvent.OnNotWorking
                        )
                    } else {
                        context?.let { c ->
                            c.showToast(c.getApiErrorMessage(response.error, response.detail))
                        }
                    }
                    findNavController().popBackStack()
                }

                is NetworkError -> {
                    context?.showToast(R.string.network_error)
                    findNavController().popBackStack()
                }

                is ApiConversionError -> {
                    context?.showToast(R.string.unknown_error)
                    findNavController().popBackStack()
                }

                is EmptyBodyError -> {
                    context?.showToast(R.string.network_error)
                    findNavController().popBackStack()
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setup(binding: FragmentTorrentProcessingBinding) {

        if (args.torrentID != null) {
            // we are loading an already available torrent
            args.torrentID?.let { viewModel.fetchTorrentDetails(it) }
        } else if (args.link != null) {
            // we are loading a new torrent
            args.link?.let {
                when {
                    it.isTorrent() -> {
                        Timber.d("Found torrent $it")
                        downloadTorrentToCache(it)
                    }

                    args.link.isMagnet() -> {
                        Timber.d("Found magnet $it")
                        viewModel.fetchAddedMagnet(it)
                    }

                    else -> {
                        Timber.e("Torrent processing link not recognized: $it")
                    }
                }
            }
        } else {
            throw IllegalArgumentException(
                "No torrent link or torrent id was passed to TorrentProcessingFragment"
            )
        }
    }

    /**
     * TorBox downloads every file as soon as the torrent is added - once its metadata (file list)
     * is known there's nothing left to wait for, so this screen hands off straight to the details
     * screen where the user can browse files and request download links for whichever they want.
     */
    private fun navigateToDetails(item: TorrentItem) {
        val action =
            TorrentProcessingFragmentDirections.actionTorrentProcessingFragmentToTorrentDetailsDest(
                item = item
            )
        findNavController().navigate(action)
    }

    private fun downloadTorrentToCache(link: String) {
        val nameRegex = "/([^/]+\\.torrent)\$"
        val m: Matcher = Pattern.compile(nameRegex).matcher(link)
        val torrentName = if (m.find()) m.group(1) else null
        val cacheDir = context?.cacheDir
        if (!torrentName.isNullOrBlank() && cacheDir != null) {
            activityViewModel.downloadFileToCache(link, torrentName, cacheDir).observe(
                viewLifecycleOwner
            ) {
                when (it) {
                    is DownloadResult.End -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileSuccess)
                        loadCachedTorrent(cacheDir, it.fileName)
                    }

                    DownloadResult.Failure -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileFailure)
                    }

                    is DownloadResult.Progress -> {
                        viewModel.triggerTorrentEvent(
                            TorrentEvent.DownloadedFileProgress(it.percent)
                        )
                    }

                    DownloadResult.WrongURL -> {
                        viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileFailure)
                    }
                }
            }
        }
    }

    private fun loadCachedTorrent(cacheDir: File, fileName: String) {
        try {
            val cacheFile = File(cacheDir, fileName)
            cacheFile.inputStream().use { inputStream ->
                val buffer: ByteArray = inputStream.readBytes()
                viewModel.fetchUploadedTorrent(buffer)
            }
        } catch (exception: Exception) {
            viewModel.triggerTorrentEvent(TorrentEvent.DownloadedFileFailure)
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
        }
    }
}
