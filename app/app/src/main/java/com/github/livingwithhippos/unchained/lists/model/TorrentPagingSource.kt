package com.github.livingwithhippos.unchained.lists.model

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import java.io.IOException
import retrofit2.HttpException

private const val TORRENT_STARTING_OFFSET = 0

/**
 * Paging Source Using Paging V3. See
 * https://github.com/android/architecture-components-samples/tree/main/PagingWithNetworkSample for
 * a sample
 *
 * TorBox's `mylist` only supports `offset`/`limit` (no `page`/`filter` like RD's endpoint), so keys
 * here are item offsets rather than page numbers.
 */
class TorrentPagingSource(
    private val torrentsRepository: TorrentsRepository,
    private val query: String,
) : PagingSource<Int, TorrentItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TorrentItem> {
        val offset = params.key ?: TORRENT_STARTING_OFFSET
        val limit = params.loadSize

        return try {
            val response =
                if (query.isBlank()) torrentsRepository.getTorrentsList(offset, limit)
                else
                    torrentsRepository.getTorrentsList(offset, limit).filter {
                        it.name.contains(query, ignoreCase = true)
                    }

            LoadResult.Page(
                data = response,
                prevKey = if (offset == TORRENT_STARTING_OFFSET) null else offset - limit,
                nextKey = if (response.isEmpty()) null else offset + limit,
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }

    override val jumpingSupported: Boolean = true

    override fun getRefreshKey(state: PagingState<Int, TorrentItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
