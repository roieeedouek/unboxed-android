package com.github.livingwithhippos.unchained.downloaddetails.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteService
import com.github.livingwithhippos.unchained.data.local.CompleteRemoteServiceDetails
import com.github.livingwithhippos.unchained.data.local.RemoteServiceType
import com.github.livingwithhippos.unchained.data.model.KodiDevice
import com.github.livingwithhippos.unchained.data.model.Stream
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.StreamContentType
import com.github.livingwithhippos.unchained.data.repository.KodiRepository
import com.github.livingwithhippos.unchained.data.repository.ServiceRepository
import com.github.livingwithhippos.unchained.data.repository.StreamingRepository
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.VLCRemoteRepository
import com.github.livingwithhippos.unchained.data.repository.WebDownloadRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

/** A [ViewModel] subclass. It offers LiveData to observe the calls to the streaming endpoint */
@HiltViewModel
class DownloadDetailsViewModel
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val streamingRepository: StreamingRepository,
    private val webDownloadRepository: WebDownloadRepository,
    private val torrentsRepository: TorrentsRepository,
    private val kodiRepository: KodiRepository,
    private val remoteServiceRepository: VLCRemoteRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModel() {

    val streamLiveData = MutableLiveData<Stream?>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()
    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()
    val messageLiveData = MutableLiveData<Event<DownloadDetailsMessage>>()
    val eventLiveData = MutableLiveData<Event<DownloadEvent>>()

    // avoids sending the same delete twice from an impatient double-tap: TorBox's backend races
    // on two concurrent deletes of the same item, 500ing one of them with a raw DATABASE_ERROR
    // even though the item genuinely does get removed either way
    private var isDeleting = false

    fun fetchStreamingInfo(contentId: Long, fileId: Long, contentType: String) {
        viewModelScope.launch {
            when (val result = streamingRepository.createStream(contentId, fileId, contentType)) {
                is EitherResult.Success -> streamLiveData.postValue(result.success)
                is EitherResult.Failure -> {
                    Timber.e("Error creating stream: ${result.failure}")
                    streamLiveData.postValue(null)
                }
            }
        }
    }

    /**
     * There's no "delete this one resolved link" operation in TorBox (unlike RD's downloads
     * list), so this deletes the whole torrent/webdl item the file belongs to.
     */
    fun deleteDownload(contentId: Long, contentType: String) {
        if (isDeleting) return
        isDeleting = true
        viewModelScope.launch {
            try {
                val result =
                    when (contentType) {
                        StreamContentType.WEB_DOWNLOAD ->
                            webDownloadRepository.deleteWebDownload(contentId)
                        else -> torrentsRepository.deleteTorrent(contentId)
                    }
                when (result) {
                    is EitherResult.Failure -> {
                        errorsLiveData.postEvent(listOf(result.failure))
                        deletedDownloadLiveData.postEvent(-1)
                    }
                    is EitherResult.Success -> deletedDownloadLiveData.postEvent(1)
                }
            } finally {
                isDeleting = false
            }
        }
    }

    fun openUrlOnKodi(mediaURL: String, kodiService: CompleteRemoteService) {
        viewModelScope.launch {
            try {
                val response =
                    kodiRepository.openUrl(
                        kodiService.address,
                        mediaURL,
                        kodiService.username,
                        kodiService.password,
                    )
                if (response != null) messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            } catch (e: Exception) {
                Timber.e("Error playing on Kodi: ${e.message}")
                messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            }
        }
    }

    fun addSubtitleOnKodi(subtitleURL: String, kodiService: CompleteRemoteService) {
        viewModelScope.launch {
            try {
                val response =
                    kodiRepository.addSubtitle(
                        kodiService.address,
                        subtitleURL,
                        kodiService.username,
                        kodiService.password,
                    )
                if (response != null) messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            } catch (e: Exception) {
                Timber.e("Error adding subtitle on Kodi: ${e.message}")
                messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            }
        }
    }

    fun openUrlOnVLC(mediaURL: String, vlcService: CompleteRemoteService) {

        viewModelScope.launch {
            try {
                val response =
                    remoteServiceRepository.openUrl(
                        vlcService.address,
                        mediaURL,
                        vlcService.username,
                        vlcService.password,
                    )
                // todo: use a single message valid for all players
                if (response is EitherResult.Failure)
                    messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
                else messageLiveData.postEvent(DownloadDetailsMessage.KodiSuccess)
            } catch (e: Exception) {
                Timber.e("Error playing on VLC: ${e.message}")
                messageLiveData.postEvent(DownloadDetailsMessage.KodiError)
            }
        }
    }

    fun getButtonVisibilityPreference(buttonKey: String, default: Boolean = true): Boolean {
        return preferences.getBoolean(buttonKey, default)
    }

    fun fetchServices(mediaPlayerOnly: Boolean = true) {
        // todo: replace other uses with [allServices]
        viewModelScope.launch {
            val services: List<CompleteRemoteService> =
                if (mediaPlayerOnly) {
                    serviceRepository.getServicesTypes(
                        types = listOf(RemoteServiceType.KODI.value, RemoteServiceType.VLC.value)
                    )
                } else serviceRepository.getServices()

            eventLiveData.postEvent(DownloadEvent.AllServices(services))
        }
    }

    suspend fun allServices(): Flow<List<CompleteRemoteService>> {
        return serviceRepository.getServicesTypesFlow(
            types = listOf(RemoteServiceType.KODI.value, RemoteServiceType.VLC.value)
        )
    }

    /**
     * returns the IDs of the most recently used service, which also has its device ID the IDs are
     * the DB entities' IDs
     */
    fun getRecentService(): Int {
        return preferences.getInt(RECENT_SERVICE_KEY, -1)
    }

    private fun setRecentService(serviceId: Int) {
        preferences.edit { putInt(RECENT_SERVICE_KEY, serviceId) }
    }

    fun openOnRemoteService(serviceDetails: CompleteRemoteServiceDetails, link: String) {
        setRecentService(serviceDetails.service.id)
        when (serviceDetails.service.type) {
            RemoteServiceType.KODI.value -> {
                openUrlOnKodi(link, serviceDetails.service)
            }
            RemoteServiceType.VLC.value -> {
                openUrlOnVLC(link, serviceDetails.service)
            }
            else -> {
                Timber.e("Unknown service type: ${serviceDetails.service.type}")
            }
        }
    }

    companion object {
        const val RECENT_SERVICE_KEY = "RECENT_SERVICE"
    }
}

sealed class DownloadDetailsMessage {
    data object KodiError : DownloadDetailsMessage()

    data object KodiSuccess : DownloadDetailsMessage()

    data object KodiMissingCredentials : DownloadDetailsMessage()

    data object KodiMissingDefault : DownloadDetailsMessage()
}

sealed class DownloadEvent {
    data class KodiDevices(val devices: List<KodiDevice>) : DownloadEvent()

    data class AllServices(val services: List<CompleteRemoteService>) : DownloadEvent()

    data class DefaultDeviceService(val service: CompleteRemoteService) : DownloadEvent()
}
