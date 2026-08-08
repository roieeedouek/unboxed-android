package com.github.livingwithhippos.unchained.newdownload.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.CreatedTorrent
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.WebDownloadRepository
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

/** A [ViewModel] subclass. It offers LiveData to be observed while creating new downloads */
@HiltViewModel
class NewDownloadViewModel
@Inject
constructor(
    private val webDownloadRepository: WebDownloadRepository,
    private val torrentsRepository: TorrentsRepository,
) : ViewModel() {

    // use Event since navigating back to this fragment would trigger this observable again
    val downloadLiveData = MutableLiveData<Event<DownloadItem>>()

    /** Posted when a webdl link resolved into more than one file - see [FolderListFragment]. */
    val webDownloadReadyLiveData = MutableLiveData<Event<WebDownloadItem>>()

    val networkExceptionLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val linkLiveData = MutableLiveData<Event<Link>>()
    val toastLiveData = MutableLiveData<Event<String>>()

    private var pollJob: Job? = null

    /**
     * Submits a hoster link for resolving. Unlike RD's one-shot `unrestrict/link`, TorBox needs a
     * create -> poll -> requestdl round trip; this may take a while if the link isn't already
     * cached on TorBox's side.
     */
    fun fetchUnrestrictedLink(link: String, password: String?) {
        viewModelScope.launch {
            when (val created = webDownloadRepository.createWebDownload(link, password)) {
                is EitherResult.Failure -> networkExceptionLiveData.postEvent(created.failure)
                is EitherResult.Success -> pollWebDownload(created.success.webDownloadId)
            }
        }
    }

    /** Fires off several links at once without waiting for each to resolve, see [postMessage]. */
    fun submitLinks(links: List<String>, password: String?) {
        viewModelScope.launch {
            var failures = 0
            links.forEach { link ->
                when (webDownloadRepository.createWebDownload(link, password)) {
                    is EitherResult.Failure -> failures++
                    is EitherResult.Success -> {}
                }
            }
            toastLiveData.postEvent(
                if (failures == 0) "${links.size} links added"
                else "${links.size - failures}/${links.size} links added"
            )
        }
    }

    private fun pollWebDownload(webId: Long) {
        pollJob?.cancelIfActive()
        pollJob =
            viewModelScope.launch(Dispatchers.IO) {
                while (isActive) {
                    val item = webDownloadRepository.getWebDownloadInfo(webId)
                    val files = item?.files
                    if (!files.isNullOrEmpty()) {
                        pollJob?.cancelIfActive()
                        if (files.size == 1) {
                            val file = files.first()
                            when (
                                val result =
                                    webDownloadRepository.getDownloadLink(webId, file.id, file = file)
                            ) {
                                is EitherResult.Failure ->
                                    networkExceptionLiveData.postEvent(result.failure)
                                is EitherResult.Success -> downloadLiveData.postEvent(result.success)
                            }
                        } else {
                            webDownloadReadyLiveData.postEvent(item)
                        }
                    }
                    delay(1500.milliseconds)
                }
            }
    }

    fun fetchUploadedTorrent(binaryTorrent: ByteArray) {
        viewModelScope.launch {
            when (val created = torrentsRepository.createTorrent(torrentFile = binaryTorrent)) {
                is EitherResult.Failure -> {
                    networkExceptionLiveData.postEvent(created.failure)
                }
                is EitherResult.Success -> {
                    linkLiveData.postEvent(Link.Torrent(created.success))
                }
            }
        }
    }

    /**
     * This function is used to manage multiple toast spawning from different parts of the logic to
     * avoid the queue getting too long and a lot of messages being shown, see the collect on the
     * fragment
     */
    fun postMessage(message: String) {
        toastLiveData.postEvent(message)
    }
}

sealed class Link {
    data class Torrent(val created: CreatedTorrent) : Link()
}
