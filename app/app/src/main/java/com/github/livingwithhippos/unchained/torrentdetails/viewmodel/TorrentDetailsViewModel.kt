package com.github.livingwithhippos.unchained.torrentdetails.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.endedStatusList
import com.github.livingwithhippos.unchained.utilities.extension.cancelIfActive
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** a [ViewModel] subclass. Retrieves a torrent's details */
@HiltViewModel
class TorrentDetailsViewModel
@Inject
constructor(private val torrentsRepository: TorrentsRepository) : ViewModel() {

    val torrentLiveData = MutableLiveData<Event<TorrentItem?>>()
    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val downloadLiveData = MutableLiveData<Event<DownloadItem?>>()
    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    private var job: Job? = null

    fun getFullTorrentInfo(id: Long) {
        viewModelScope.launch {
            val torrentData = torrentsRepository.getTorrentInfo(id)
            if (torrentData != null) torrentLiveData.postEvent(torrentData)
        }
    }

    fun pollTorrentStatus(id: Long) {
        // todo: test if I need to recreate a job when it is cancelled
        job?.cancelIfActive()

        job =
            viewModelScope.launch(Dispatchers.IO) {
                // / maybe job.isActive?
                while (isActive) {
                    val torrentData = torrentsRepository.getTorrentInfo(id)
                    if (torrentData != null) torrentLiveData.postEvent(torrentData)
                    if (
                        torrentData?.downloadFinished == true ||
                            endedStatusList.contains(torrentData?.downloadState)
                    )
                        job?.cancelIfActive()

                    delay(2000.milliseconds)
                }
            }
    }

    fun deleteTorrent(id: Long) {
        viewModelScope.launch {
            when (val deleted = torrentsRepository.deleteTorrent(id)) {
                is EitherResult.Failure -> {
                    errorsLiveData.postEvent(listOf(deleted.failure))
                }
                is EitherResult.Success -> {
                    deletedTorrentLiveData.postEvent(204)
                }
            }
        }
    }

    /** Requests a download link for the torrent's single file (see [downloadFiles] for many). */
    fun downloadTorrent(torrent: TorrentItem) {
        val file = torrent.files?.firstOrNull() ?: return
        viewModelScope.launch {
            when (val result = torrentsRepository.getDownloadLink(torrent.id, file.id, file = file)) {
                is EitherResult.Failure -> errorsLiveData.postEvent(listOf(result.failure))
                is EitherResult.Success -> downloadLiveData.postEvent(result.success)
            }
        }
    }

    /** Requests download links for several files of a torrent at once. */
    fun downloadFiles(torrentId: Long, fileIds: List<Long>) {
        viewModelScope.launch {
            val items =
                torrentsRepository.getDownloadLinkList(fileIds.map { Triple(torrentId, it, null) })
            val values = items.filterIsInstance<EitherResult.Success<DownloadItem>>().map { it.success }
            val errors =
                items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>().map {
                    it.failure
                }
            downloadLiveData.postEvent(values.firstOrNull())
            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
        }
    }
}
