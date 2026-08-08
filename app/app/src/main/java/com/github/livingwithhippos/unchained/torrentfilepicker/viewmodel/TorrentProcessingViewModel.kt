package com.github.livingwithhippos.unchained.torrentfilepicker.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.CreatedTorrent
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
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
import timber.log.Timber

@HiltViewModel
class TorrentProcessingViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val torrentsRepository: TorrentsRepository,
) : ViewModel() {

    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val torrentLiveData = MutableLiveData<Event<TorrentEvent>>()

    private var job: Job? = null

    fun fetchAddedMagnet(magnet: String) {
        viewModelScope.launch {
            when (val created = torrentsRepository.createTorrent(magnet = magnet)) {
                is EitherResult.Failure -> {
                    Timber.e("Error adding magnet: ${created.failure}")
                    networkExceptionLiveData.postEvent(created.failure)
                }
                is EitherResult.Success -> {
                    setTorrentID(created.success.torrentId.toString())
                    torrentLiveData.postEvent(TorrentEvent.Created(created.success))
                }
            }
        }
    }

    fun fetchTorrentDetails(torrentID: String) {

        setTorrentID(torrentID)

        viewModelScope.launch {
            val id = torrentID.toLongOrNull()
            val torrentData: TorrentItem? = id?.let { torrentsRepository.getTorrentInfo(it) }
            if (torrentData != null) {
                setTorrentDetails(torrentData)
                torrentLiveData.postEvent(TorrentEvent.TorrentInfo(torrentData))
            } else {
                Timber.e("Retrieved torrent info were null for id $torrentID")
            }
        }
    }

    private fun setTorrentDetails(item: TorrentItem) {
        savedStateHandle[KEY_CURRENT_TORRENT] = item
    }

    fun getTorrentID(): String? {
        return savedStateHandle[KEY_CURRENT_TORRENT_ID]
    }

    private fun setTorrentID(id: String) {
        savedStateHandle[KEY_CURRENT_TORRENT_ID] = id
    }

    /**
     * TorBox has no server-side file-selection step: every file starts downloading as soon as the
     * torrent is added. This just polls [TorrentsRepository.getTorrentInfo] until the torrent's
     * file list is populated (metadata fetched), which is all there is to wait for before the
     * torrent details screen can show something useful.
     */
    fun startMetadataPollLoop() {

        val id = getTorrentID()?.toLongOrNull()

        if (id == null) {
            Timber.e("Torrent metadata poll requested but torrent id was not ready")
            return
        }

        job?.cancelIfActive()

        job =
            viewModelScope.launch(Dispatchers.IO) {
                while (isActive) {
                    val torrentItem: TorrentItem? = torrentsRepository.getTorrentInfo(id)
                    if (torrentItem != null && !torrentItem.files.isNullOrEmpty()) {
                        job?.cancelIfActive()
                        torrentLiveData.postEvent(TorrentEvent.FilesReady(torrentItem))
                    }
                    delay(1500.milliseconds)
                }
            }
    }

    fun triggerTorrentEvent(event: TorrentEvent) {
        torrentLiveData.postEvent(event)
    }

    fun fetchUploadedTorrent(binaryTorrent: ByteArray) {
        viewModelScope.launch {
            when (val created = torrentsRepository.createTorrent(torrentFile = binaryTorrent)) {
                is EitherResult.Failure -> {
                    networkExceptionLiveData.postEvent(created.failure)
                    torrentLiveData.postEvent(TorrentEvent.DownloadedFileFailure)
                }
                is EitherResult.Success -> {
                    fetchTorrentDetails(created.success.torrentId.toString())
                }
            }
        }
    }

    companion object {
        const val KEY_CURRENT_TORRENT = "current_torrent_key"
        const val KEY_CURRENT_TORRENT_ID = "current_torrent_id_key"
    }
}

sealed class TorrentEvent {
    data class Created(val torrent: CreatedTorrent) : TorrentEvent()

    data class TorrentInfo(val item: TorrentItem) : TorrentEvent()

    data class FilesReady(val torrent: TorrentItem) : TorrentEvent()

    data object DownloadedFileSuccess : TorrentEvent()

    data object DownloadedFileFailure : TorrentEvent()

    data class DownloadedFileProgress(val progress: Int) : TorrentEvent()
}
