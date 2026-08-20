package com.github.livingwithhippos.unchained.lists.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.liveData
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.WebDownloadRepository
import com.github.livingwithhippos.unchained.lists.model.DownloadPagingSource
import com.github.livingwithhippos.unchained.lists.model.TorrentPagingSource
import com.github.livingwithhippos.unchained.utilities.DOWNLOADS_TAB
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

/**
 * A [ViewModel] subclass. It offers LiveData to be observed to populate lists with paging support
 */
@HiltViewModel
class ListTabsViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: SharedPreferences,
    private val downloadRepository: DownloadRepository,
    private val torrentsRepository: TorrentsRepository,
    private val webDownloadRepository: WebDownloadRepository,
) : ViewModel() {

    // stores the last query value
    private val queryLiveData = MutableLiveData<String>()

    // items are filtered returning only if their names contain the query
    val downloadsLiveData: LiveData<PagingData<WebDownloadItem>> =
        queryLiveData.switchMap { query: String ->
            val size = getPagingSize()
            val initialSize = max(size, INITIAL_LOAD)
            Pager(PagingConfig(pageSize = size, initialLoadSize = initialSize)) {
                    DownloadPagingSource(downloadRepository, query)
                }
                .liveData
                .cachedIn(viewModelScope)
        }

    val torrentsLiveData: LiveData<PagingData<TorrentItem>> =
        queryLiveData.switchMap { query: String ->
            val size = getPagingSize()
            val initialSize = max(size, INITIAL_LOAD)
            Pager(PagingConfig(pageSize = size, initialLoadSize = initialSize)) {
                    TorrentPagingSource(torrentsRepository, query)
                }
                .liveData
                .cachedIn(viewModelScope)
        }

    val errorsLiveData = MutableLiveData<Event<List<UnchainedNetworkException>>>()

    val downloadItemLiveData = MutableLiveData<Event<List<DownloadItem>>>()

    val resolvedDownloadsLiveData = MutableLiveData<Event<ResolvedDownloadsResult>>()

    val deletedTorrentLiveData = MutableLiveData<Event<Int>>()
    val deletedDownloadLiveData = MutableLiveData<Event<Int>>()

    val eventLiveData = MutableLiveData<Event<ListEvent>>()

    // tab index of the list to be scrolled to the top, see ListsTabFragment fabScrollToTop
    val scrollToTopLiveData = MutableLiveData<Event<Int>>()

    /**
     * Requests a download link for every file of a torrent and moves it to the download section.
     * Unlike RD there's no separate "unrestrict" call - a file is downloadable as soon as it's
     * part of a torrent with [TorrentItem.downloadPresent], so this just mints a `requestdl` link
     * per file.
     */
    fun unrestrictTorrent(torrent: TorrentItem) {
        viewModelScope.launch {
            val files = torrent.files
            if (files.isNullOrEmpty()) return@launch

            val items =
                torrentsRepository.getDownloadLinkList(files.map { Triple(torrent.id, it.id, it) })
            val values =
                items.filterIsInstance<EitherResult.Success<DownloadItem>>().map { it.success }
            val errors =
                items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>().map {
                    it.failure
                }

            downloadItemLiveData.postEvent(values)
            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
        }
    }

    /**
     * Resolves the single file of a torrent into a direct link and opens it in the download
     * details screen. Unlike [unrestrictTorrent] (built for the bulk "download selected" case,
     * where there's no single screen to navigate to), a single-file torrent click needs to
     * actually take the user somewhere - posting to [downloadItemLiveData] alone is a dead end,
     * since nothing observes it to open a details screen.
     */
    fun openTorrentFile(torrent: TorrentItem) {
        viewModelScope.launch {
            val file = torrent.files?.singleOrNull()
            when (
                val result =
                    torrentsRepository.getDownloadLink(torrentId = torrent.id, fileId = file?.id, file = file)
            ) {
                is EitherResult.Success ->
                    eventLiveData.postEvent(ListEvent.DownloadItemClick(result.success))
                is EitherResult.Failure -> errorsLiveData.postEvent(listOf(result.failure))
            }
        }
    }

    /**
     * Resolves the single file of a webdl job into a direct link and opens it in the download
     * details screen, mirroring what clicking an already-resolved link used to do on RD.
     */
    fun openWebDownload(item: WebDownloadItem) {
        viewModelScope.launch {
            val file = item.files?.singleOrNull()
            when (
                val result =
                    webDownloadRepository.getDownloadLink(webId = item.id, fileId = file?.id, file = file)
            ) {
                is EitherResult.Success ->
                    eventLiveData.postEvent(ListEvent.DownloadItemClick(result.success))
                is EitherResult.Failure -> errorsLiveData.postEvent(listOf(result.failure))
            }
        }
    }

    /**
     * Resolves a batch of webdl jobs' files into direct links for the "download selected"/"share
     * selected" buttons, then posts the result tagged with which action triggered it.
     */
    fun resolveDownloadLinks(downloads: List<WebDownloadItem>, intent: DownloadIntent) {
        viewModelScope.launch {
            val files = downloads.flatMap { d -> (d.files ?: emptyList()).map { Triple(d.id, it.id, it) } }
            if (files.isEmpty()) return@launch

            val items = webDownloadRepository.getDownloadLinkList(files)
            val values =
                items.filterIsInstance<EitherResult.Success<DownloadItem>>().map { it.success }
            val errors =
                items.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>().map {
                    it.failure
                }

            if (values.isNotEmpty())
                resolvedDownloadsLiveData.postEvent(ResolvedDownloadsResult(intent, values))
            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
        }
    }

    private fun getPagingSize(): Int {
        return min(preferences.getInt("paging_size", 50), MAX_PAGE_SIZE)
    }

    fun setSelectedTab(tabID: Int) {
        savedStateHandle[KEY_SELECTED_TAB] = tabID
    }

    fun getSelectedTab(): Int {
        return savedStateHandle[KEY_SELECTED_TAB] ?: DOWNLOADS_TAB
    }

    fun setListFilter(query: String?) {
        // Avoid updating the lists if the query hasn't changed. We don't check for cases but we
        // could
        if (queryLiveData.value != query) queryLiveData.postValue(query?.trim() ?: "")
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            deletedDownloadLiveData.postEvent(0)
            var offset = 0
            val pageSize = 50
            val completeDownloadList = mutableListOf<WebDownloadItem>()
            do {
                val downloads = downloadRepository.getDownloads(offset, pageSize)
                completeDownloadList.addAll(downloads)
                offset += pageSize
            } while (downloads.size >= pageSize)

            // post a message every 10% of the deletion progress if there are more than 10 items
            val progressIndicator: Int =
                if (completeDownloadList.size / 10 < 15) 15 else completeDownloadList.size / 10

            val errors = mutableListOf<UnchainedNetworkException>()
            completeDownloadList.forEachIndexed { index, item ->
                val result = downloadRepository.deleteDownload(item.id)
                if (result is EitherResult.Failure) errors.add(result.failure)
                if ((index + 1) % progressIndicator == 0)
                    deletedDownloadLiveData.postEvent(index + 1)
            }

            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
            deletedDownloadLiveData.postEvent(DOWNLOADS_DELETED_ALL)
        }
    }

    fun deleteAllTorrents() {
        viewModelScope.launch {
            var offset = 0
            val pageSize = 50
            val errors = mutableListOf<UnchainedNetworkException>()
            do {
                val torrents = torrentsRepository.getTorrentsList(offset, pageSize)
                torrents.forEach { torrent ->
                    val result = torrentsRepository.deleteTorrent(torrent.id)
                    if (result is EitherResult.Failure) errors.add(result.failure)
                }
                offset += pageSize
            } while (torrents.size >= pageSize)

            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
            deletedTorrentLiveData.postEvent(TORRENTS_DELETED_ALL)
        }
    }

    fun deleteTorrents(torrents: List<TorrentItem>) {
        viewModelScope.launch {
            val results = torrents.map { torrentsRepository.deleteTorrent(it.id) }
            val errors =
                results.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>().map {
                    it.failure
                }
            val deletedCount = results.count { it is EitherResult.Success }

            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
            deletedTorrentLiveData.postEvent(
                when {
                    deletedCount == 0 -> TORRENT_NOT_DELETED
                    torrents.size > 1 -> TORRENTS_DELETED
                    else -> TORRENT_DELETED
                }
            )
        }
    }

    fun downloadItems(torrents: List<TorrentItem>) {
        torrents.filter { it.downloadPresent }.forEach { unrestrictTorrent(it) }
    }

    fun deleteDownloads(downloads: List<WebDownloadItem>) {
        viewModelScope.launch {
            val results = downloads.map { downloadRepository.deleteDownload(it.id) }
            val errors =
                results.filterIsInstance<EitherResult.Failure<UnchainedNetworkException>>().map {
                    it.failure
                }
            val deletedCount = results.count { it is EitherResult.Success }

            if (errors.isNotEmpty()) errorsLiveData.postEvent(errors)
            deletedDownloadLiveData.postEvent(
                when {
                    deletedCount == 0 -> DOWNLOAD_NOT_DELETED
                    downloads.size > 1 -> DOWNLOADS_DELETED
                    else -> DOWNLOAD_DELETED
                }
            )
        }
    }

    fun postEventNotice(event: ListEvent) {
        eventLiveData.postEvent(event)
    }

    fun postScrollToTop(tab: Int) {
        scrollToTopLiveData.postEvent(tab)
    }

    companion object {
        const val KEY_SELECTED_TAB = "selected_tab_key"
        const val TORRENT_DELETED = -1
        const val TORRENTS_DELETED = -2
        const val TORRENTS_DELETED_ALL = -3
        const val TORRENT_NOT_DELETED = -4
        const val DOWNLOAD_DELETED = -1
        const val DOWNLOADS_DELETED = -2
        const val DOWNLOADS_DELETED_ALL = -3
        const val DOWNLOAD_NOT_DELETED = -4

        private const val MAX_PAGE_SIZE = 2500

        private const val INITIAL_LOAD = 100
    }
}

sealed class ListEvent {
    data class DownloadItemClick(val item: DownloadItem) : ListEvent()

    data class TorrentItemClick(val item: TorrentItem) : ListEvent()

    data class OpenTorrent(val item: TorrentItem) : ListEvent()

    data class SetTab(val tab: Int) : ListEvent()

    data object NewDownload : ListEvent()
}

enum class DownloadIntent {
    DOWNLOAD,
    SHARE,
}

data class ResolvedDownloadsResult(val intent: DownloadIntent, val items: List<DownloadItem>)
