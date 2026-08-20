package com.github.livingwithhippos.unchained.torrentdetails.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.DeleteDialogFragment
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.TorBoxApiError
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.databinding.FragmentTorrentDetailsBinding
import com.github.livingwithhippos.unchained.lists.view.ListState
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentFilesAdapter
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentContentListener
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.torrentdetails.viewmodel.TorrentDetailsViewModel
import com.github.livingwithhippos.unchained.utilities.EventObserver
import com.github.livingwithhippos.unchained.utilities.Node
import com.github.livingwithhippos.unchained.utilities.extension.copyToClipboard
import com.github.livingwithhippos.unchained.utilities.extension.getApiErrorMessage
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString
import com.github.livingwithhippos.unchained.utilities.extension.getStatusTranslation
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.loadingStatusList
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * A simple [Fragment] subclass. It is capable of showing the details of a [TorrentItem] and
 * updating it.
 */
@AndroidEntryPoint
class TorrentDetailsFragment : UnchainedFragment(), TorrentContentListener {

    private val viewModel: TorrentDetailsViewModel by viewModels()

    private val args: TorrentDetailsFragmentArgs by navArgs()

    private var _binding: FragmentTorrentDetailsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTorrentDetailsBinding.inflate(inflater, container, false)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    // Add menu items here
                    menuInflater.inflate(R.menu.torrent_details_bar, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.delete -> {
                            val dialog = DeleteDialogFragment()
                            dialog.show(parentFragmentManager, "DeleteDialogFragment")
                            true
                        }

                        R.id.reselect -> {
                            val link = "magnet:?xt=urn:btih:${args.item.hash}"
                            val action =
                                TorrentDetailsFragmentDirections
                                    .actionTorrentDetailsDestToTorrentProcessingFragment(
                                        link = link
                                    )
                            findNavController().navigate(action)
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        binding.tvStatus.text = requireContext().getStatusTranslation(args.item.downloadState)
        binding.fabShareMagnet.setOnClickListener { onShareMagnetClick() }
        binding.fabCopyMagnet.setOnClickListener { onCopyMagnetClick() }
        binding.bDownload.setOnClickListener { onDownloadClick() }

        val adapter = TorrentContentFilesAdapter()
        binding.rvFileList.adapter = adapter

        viewModel.torrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                it?.let { torrent ->
                    val totalFiles = torrent.files?.count() ?: 0
                    // TorBox has no file-selection step: every file is always "selected"
                    binding.tvSelectedFilesNumber.text = totalFiles.toString()

                    binding.tvStatus.text = requireContext().getStatusTranslation(torrent.downloadState)

                    binding.tvTotalFiles.text = totalFiles.toString()
                    binding.tvName.text = torrent.name
                    val progressPercent = (torrent.progress * 100).toInt()
                    binding.tvProgressPercent.text =
                        getString(R.string.percent_format, progressPercent.toFloat())
                    binding.tvProgress.text =
                        getString(R.string.percent_format, progressPercent.toFloat())
                    if (progressPercent in 0..99) {
                        binding.tvProgress.visibility = View.VISIBLE
                    } else {
                        binding.tvProgress.visibility = View.GONE
                    }
                    try {
                        val torrentSpeed = torrent.downloadSpeed
                        if (torrentSpeed == null) {
                            binding.tvSpeed.text = ""
                        } else {
                            binding.tvSpeed.text =
                                when (torrentSpeed.toString().length) {
                                    in 0..3 -> getString(R.string.speed_format_b, torrentSpeed)
                                    in 4..6 ->
                                        getString(R.string.speed_format_kb, torrentSpeed / 1000.0)
                                    in 7..15 ->
                                        getString(
                                            R.string.speed_format_mb,
                                            torrentSpeed / 1000000.0,
                                        )
                                    else -> getString(R.string.speed_error)
                                }
                        }
                    } catch (ex: Exception) {
                        Timber.e(ex, "Error formatting speed from '${torrent.downloadSpeed}'")
                        binding.tvSpeed.text = ""
                    }
                    if (torrent.seeds == null) {
                        binding.tvSeeders.visibility = View.GONE
                    } else {
                        binding.tvSeeders.text =
                            resources.getQuantityString(
                                R.plurals.seeders_format,
                                torrent.seeds,
                                torrent.seeds,
                            )
                        binding.tvSeeders.visibility = View.VISIBLE
                    }
                    binding.pbDownload.setProgressCompat(progressPercent, true)
                    binding.bDownload.visibility =
                        if (torrent.downloadPresent) View.VISIBLE else View.GONE
                    context?.let { ctx ->
                        binding.tvFileSize.text = getFileSizeString(ctx, torrent.size)
                        binding.tvSelectedSize.text = getFileSizeString(ctx, torrent.size)
                    }
                    binding.cvDownloadDetails.visibility =
                        if (torrent.downloadState.equals("downloading", true)) View.VISIBLE
                        else View.GONE

                    // Data should not change between updates so we should just populate it once
                    if (adapter.itemCount == 0) {
                        val torrentStructure: Node<TorrentFileItem> = getFilesNodes(torrent)
                        // show list only if it's populated enough
                        if (torrentStructure.children.isNotEmpty()) {
                            val filesList = mutableListOf<TorrentFileItem>()
                            var skippedFirst = false
                            Node.traverseDepthFirst(torrentStructure) { item ->
                                // avoid root item "/"
                                if (!skippedFirst) skippedFirst = true else filesList.add(item)
                            }
                            adapter.submitList(filesList)
                            binding.cvSelectedTorrentFiles.visibility = View.VISIBLE
                        }
                    }
                }
            },
        )

        viewModel.deletedTorrentLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it > 0) {
                    context?.showToast(R.string.torrent_removed)
                    activityViewModel.setListState(ListState.UpdateTorrent)
                    // if deleted go back
                    findNavController().popBackStack()
                } else {
                    // the actual error is reported through errorsLiveData; still refresh the
                    // other list screen in case the delete actually went through server-side
                    // despite the error
                    activityViewModel.setListState(ListState.UpdateTorrent)
                }
            },
        )

        setFragmentResultListener("deleteActionKey") { _, bundle ->
            if (bundle.getBoolean("deleteConfirmation")) viewModel.deleteTorrent(args.item.id)
        }

        viewModel.downloadLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                it?.let { download ->
                    val action =
                        TorrentDetailsFragmentDirections.actionTorrentDetailsToDownloadDetailsDest(
                            download
                        )
                    findNavController().navigate(action)
                }
            },
        )

        viewModel.errorsLiveData.observe(
            viewLifecycleOwner,
            EventObserver {
                for (error in it) {
                    when (error) {
                        is TorBoxApiError -> {
                            context?.let { c ->
                                c.showToast(c.getApiErrorMessage(error.error, error.detail))
                            }
                        }

                        is EmptyBodyError -> {}
                        is NetworkError -> {
                            context?.showToast(R.string.network_error)
                        }

                        is ApiConversionError -> {
                            context?.showToast(R.string.parsing_error)
                        }
                    }
                }
            },
        )

        // maybe load and save the latest retrieved one in the view-model?
        if (loadingStatusList.contains(args.item.downloadState))
            viewModel.pollTorrentStatus(args.item.id)
        else {
            viewModel.getFullTorrentInfo(args.item.id)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onDownloadClick() {
        val item: TorrentItem = viewModel.torrentLiveData.value?.peekContent() ?: args.item
        val fileCount = item.files?.size ?: 0
        if (fileCount > 1) {
            val action =
                TorrentDetailsFragmentDirections.actionTorrentDetailsToTorrentFolder(
                    torrent = item,
                    webDownload = null,
                )
            findNavController().navigate(action)
        } else {
            viewModel.downloadTorrent(item)
        }
    }

    fun onShareMagnetClick() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "magnet:?xt=urn:btih:${args.item.hash}")
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)))
    }

    fun onCopyMagnetClick() {
        copyToClipboard(getString(R.string.torbox_magnet), "magnet:?xt=urn:btih:${args.item.hash}")
        context?.showToast(R.string.link_copied)
    }

    override fun onSelectedFile(item: TorrentFileItem) {
        // not used here
    }

    override fun onSelectedFolder(item: TorrentFileItem) {
        // not used here
    }
}
