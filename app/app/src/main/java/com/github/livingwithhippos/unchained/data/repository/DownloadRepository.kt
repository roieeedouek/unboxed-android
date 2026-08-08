package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.ItemControlRequest
import com.github.livingwithhippos.unchained.data.model.TorrentOperation
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import com.github.livingwithhippos.unchained.data.remote.WebDownloadApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject

/**
 * Thin wrapper over the `webdl/mylist` endpoint, backing the "Downloads" tab. Unlike RD's flat,
 * permanent link history (`GET downloads`), this is a list of webdl jobs - see
 * [WebDownloadRepository] for the create/poll/requestdl flow that produces them.
 */
class DownloadRepository
@Inject
constructor(
    protoStore: ProtoStore,
    private val webDownloadApiHelper: WebDownloadApiHelper,
    private val webDownloadRepository: WebDownloadRepository,
) : BaseRepository(protoStore) {

    suspend fun getDownloads(offset: Int? = null, limit: Int? = null): List<WebDownloadItem> {
        val downloadResponse =
            safeApiCall(
                call = {
                    webDownloadApiHelper.getWebDownloadsList(
                        token = "Bearer ${getToken()}",
                        offset = offset,
                        limit = limit,
                    )
                },
                errorMessage = "Error Fetching Downloads list or list empty",
            )

        return downloadResponse ?: emptyList()
    }

    suspend fun deleteDownload(id: Long): EitherResult<UnchainedNetworkException, Unit> {
        val response =
            eitherApiResultUnit(
                call = {
                    webDownloadApiHelper.controlWebDownload(
                        token = "Bearer ${getToken()}",
                        body = ItemControlRequest(webId = id, operation = TorrentOperation.DELETE),
                    )
                },
                errorMessage = "Error deleting download",
            )

        if (response is EitherResult.Success) {
            webDownloadRepository.clearDownloadLinkCache()
        }

        return response
    }
}
