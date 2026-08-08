package com.github.livingwithhippos.unchained.folderlist.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.model.InnerTorrentFile
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.data.repository.WebDownloadRepository
import com.github.livingwithhippos.unchained.folderlist.view.FolderListFragment
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.Event
import com.github.livingwithhippos.unchained.utilities.postEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Resolves a direct download link for each file of a multi-file torrent or webdl item, one
 * `requestdl` call at a time, showing progress as they come in - TorBox has no bulk "download all"
 * link, only a per-file (or whole-item zip) one.
 */
@HiltViewModel
class FolderListViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    private val preferences: SharedPreferences,
    private val torrentsRepository: TorrentsRepository,
    private val webDownloadRepository: WebDownloadRepository,
) : ViewModel() {

    val folderLiveData = MutableLiveData<Event<List<DownloadItem>>>()
    val removedDownloadLiveData = MutableLiveData<Event<DownloadItem>>()
    val errorsLiveData = MutableLiveData<Event<UnchainedNetworkException>>()
    val progressLiveData = MutableLiveData<Int>()

    // used to simulate a debounce effect while typing on the search bar
    private var queryJob: Job? = null

    // stores the last query value
    val queryLiveData = MutableLiveData<String>()

    fun shouldShowFilters(): Boolean {
        return preferences.getBoolean(KEY_SHOW_FOLDER_FILTERS, false)
    }

    fun retrieveTorrentFiles(torrentId: Long, files: List<InnerTorrentFile>) {
        resolveFiles(files) { file -> torrentsRepository.getDownloadLink(torrentId, file.id, file = file) }
    }

    fun retrieveWebDownloadFiles(webId: Long, files: List<InnerTorrentFile>) {
        resolveFiles(files) { file -> webDownloadRepository.getDownloadLink(webId, file.id, file = file) }
    }

    private fun resolveFiles(
        files: List<InnerTorrentFile>,
        request: suspend (InnerTorrentFile) -> EitherResult<UnchainedNetworkException, DownloadItem>,
    ) {
        viewModelScope.launch {
            // either first time or there were some errors, re-resolve
            if (files.size != getRetrievedLinks()) {
                val hitList = mutableListOf<DownloadItem>()

                files.forEachIndexed { index, file ->
                    when (val result = request(file)) {
                        is EitherResult.Failure -> {
                            errorsLiveData.postEvent(result.failure)
                            progressLiveData.postValue((index + 1) * 100 / files.size)
                        }
                        is EitherResult.Success -> {
                            hitList.add(result.success)
                            folderLiveData.postEvent(hitList.toList())
                            setRetrievedLinks(hitList.size)
                            progressLiveData.postValue((index + 1) * 100 / files.size)
                        }
                    }
                }
            } else {
                // I already resolved all the files, repost the last value
                folderLiveData.value?.let { folderLiveData.postEvent(it.peekContent()) }
            }
        }
    }

    private fun setRetrievedLinks(links: Int) {
        savedStateHandle[KEY_RETRIEVED_LINKS] = links
    }

    private fun getRetrievedLinks(): Int {
        return savedStateHandle[KEY_RETRIEVED_LINKS] ?: -1
    }

    /**
     * Removes the given items from the displayed list. Unlike RD, there's no "delete this one
     * resolved link" server operation in TorBox (only whole-torrent/webdl-item deletion), so this
     * is a local-only, view-level removal.
     */
    fun removeFromView(items: List<DownloadItem>) {
        items.forEach { removedDownloadLiveData.postEvent(it) }
    }

    fun filterList(query: String?) {
        // simulate debounce
        queryJob?.cancel()

        queryJob = viewModelScope.launch {
            delay(500.milliseconds)
            if (isActive) queryLiveData.postValue(query?.trim() ?: "")
        }
    }

    fun getMinFileSize(): Long {
        val minMBString = preferences.getString("filter_size_mb", "10")
        val minMB: Long = minMBString?.toLongOrNull() ?: 10
        return minMB * 1024 * 1024
    }

    fun setFilterSizePreference(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_LIST_FILTER_SIZE, enabled) }
    }

    fun getFilterSizePreference(): Boolean {
        return preferences.getBoolean(KEY_LIST_FILTER_SIZE, false)
    }

    fun setFilterTypePreference(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_LIST_FILTER_TYPE, enabled) }
    }

    fun getFilterTypePreference(): Boolean {
        return preferences.getBoolean(KEY_LIST_FILTER_TYPE, false)
    }

    fun setListSortPreference(tag: String) {
        preferences.edit { putString(KEY_LIST_SORTING, tag) }
    }

    fun getListSortPreference(): String {
        return preferences.getString(KEY_LIST_SORTING, FolderListFragment.TAG_DEFAULT_SORT)
            ?: FolderListFragment.TAG_DEFAULT_SORT
    }

    fun setScrollingAllowed(allow: Boolean) {
        preferences.edit { putBoolean(KEY_ALLOW_SCROLLING, allow) }
    }

    fun getScrollingAllowed(): Boolean {
        return preferences.getBoolean(KEY_ALLOW_SCROLLING, true)
    }

    companion object {
        const val KEY_ALLOW_SCROLLING = "allow_scrolling"
        const val KEY_RETRIEVED_LINKS = "retrieve_links"
        const val KEY_LIST_FILTER_SIZE = "filter_list_size"
        const val KEY_LIST_FILTER_TYPE = "filter_list_type"
        const val KEY_LIST_SORTING = "sort_list_type"
        const val KEY_SHOW_FOLDER_FILTERS = "show_folders_filters"
    }
}
