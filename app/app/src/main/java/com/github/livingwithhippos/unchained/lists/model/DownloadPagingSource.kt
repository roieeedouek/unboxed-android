package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import com.github.livingwithhippos.unchained.data.repository.DownloadRepository
import java.io.IOException
import retrofit2.HttpException

private const val DOWNLOAD_STARTING_OFFSET = 0

/**
 * TorBox's `webdl/mylist` only supports `offset`/`limit` (no `page`/`filter` like RD's downloads
 * endpoint), so keys here are item offsets rather than page numbers.
 */
class DownloadPagingSource(
    private val downloadRepository: DownloadRepository,
    private val query: String,
) : PagingSource<Int, WebDownloadItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, WebDownloadItem> {
        val offset = params.key ?: DOWNLOAD_STARTING_OFFSET
        val limit = params.loadSize

        return try {
            val response =
                if (query.isBlank()) downloadRepository.getDownloads(offset, limit)
                else
                    downloadRepository.getDownloads(offset, limit).filter {
                        it.name.contains(query, ignoreCase = true)
                    }

            LoadResult.Page(
                data = response,
                prevKey = if (offset == DOWNLOAD_STARTING_OFFSET) null else offset - limit,
                nextKey = if (response.isEmpty()) null else offset + limit,
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, WebDownloadItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
