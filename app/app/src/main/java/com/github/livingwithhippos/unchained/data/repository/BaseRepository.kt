package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.NetworkResponse
import com.github.livingwithhippos.unchained.data.model.TorBoxApiError
import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.TorBoxErrorBody
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber

/**
 * Base repository class to be extended by other repositories. Manages the calls between retrofit
 * and the actual repositories.
 *
 * Every TorBox endpoint wraps its payload in a [TorBoxEnvelope]: `{success, error, detail, data}`,
 * on both success and failure. The helpers below unwrap that envelope so the rest of the app can
 * keep working with plain data classes.
 */
open class BaseRepository(private val protoStore: ProtoStore) {

    // todo: inject this
    private val errorBodyAdapter: JsonAdapter<TorBoxErrorBody> =
        Moshi.Builder().build().adapter(TorBoxErrorBody::class.java)

    suspend fun <T : Any> safeApiCall(
        call: suspend () -> Response<TorBoxEnvelope<T>>,
        errorMessage: String,
    ): T? {
        val result: NetworkResponse<T> =
            try {
                safeApiResult(call, errorMessage)
            } catch (e: Exception) {
                NetworkResponse.Error(e)
            }

        var data: T? = null

        when (result) {
            is NetworkResponse.Success -> data = result.data
            is NetworkResponse.SuccessEmptyBody ->
                Timber.d("Successful call with empty body : ${result.code}")
            is NetworkResponse.Error -> Timber.d(errorMessage)
        }

        return data
    }

    private suspend fun <T : Any> safeApiResult(
        call: suspend () -> Response<TorBoxEnvelope<T>>,
        errorMessage: String,
    ): NetworkResponse<T> {
        try {
            val response: Response<TorBoxEnvelope<T>> = call.invoke()
            val envelope = response.body()
            if (response.isSuccessful && envelope?.success == true) {
                val body = envelope.data
                return if (body != null) NetworkResponse.Success(body)
                else NetworkResponse.SuccessEmptyBody(response.code())
            }
        } catch (e: Exception) {
            NetworkResponse.Error(e)
        }

        return NetworkResponse.Error(
            IOException("Error Occurred while getting api result, error : $errorMessage")
        )
    }

    suspend fun <T : Any> eitherApiResult(
        call: suspend () -> Response<TorBoxEnvelope<T>>,
        errorMessage: String,
    ): EitherResult<UnchainedNetworkException, T> =
        withContext(Dispatchers.IO) {
            val response: Response<TorBoxEnvelope<T>> =
                try {
                    call.invoke()
                } catch (e: Exception) {
                    Timber.e(e, "Error Occurred while getting api result")
                    return@withContext EitherResult.Failure(NetworkError(-1, errorMessage))
                }
            val code = response.code()

            if (response.isSuccessful) {
                val envelope: TorBoxEnvelope<T>? = response.body()
                return@withContext when {
                    envelope == null ->
                        EitherResult.Failure(NetworkError(-1, "$errorMessage, http code $code"))
                    envelope.success && envelope.data != null ->
                        EitherResult.Success(envelope.data)
                    envelope.success ->
                        // e.g. controltorrent's "data": null on a successful operation
                        EitherResult.Failure(EmptyBodyError(code))
                    else -> EitherResult.Failure(TorBoxApiError(envelope.error, envelope.detail))
                }
            } else {
                try {
                    val error: TorBoxErrorBody? =
                        errorBodyAdapter.fromJson(response.errorBody()!!.string())
                    return@withContext if (error != null)
                        EitherResult.Failure(TorBoxApiError(error.error, error.detail))
                    else EitherResult.Failure(ApiConversionError(-1))
                } catch (e: IOException) {
                    Timber.e(e, "Error parsing error body")
                    // todo: analyze error to return code
                    return@withContext EitherResult.Failure(
                        NetworkError(-1, "$errorMessage, http code $code")
                    )
                }
            }
        }

    /**
     * Like [eitherApiResult], but for endpoints whose successful response legitimately carries a
     * null `data` (e.g. `controltorrent`/`controlwebdownload`'s `"data": null`) - unlike
     * [eitherApiResult], a null `data` on `success: true` is treated as [EitherResult.Success],
     * not [EmptyBodyError]. Callers pass `Any?` as the envelope's type param since the payload is
     * discarded either way; that also sidesteps asking Moshi for a `JsonAdapter<Unit>`, which it
     * doesn't have.
     */
    suspend fun eitherApiResultUnit(
        call: suspend () -> Response<TorBoxEnvelope<Any?>>,
        errorMessage: String,
    ): EitherResult<UnchainedNetworkException, Unit> =
        withContext(Dispatchers.IO) {
            val response: Response<TorBoxEnvelope<Any?>> =
                try {
                    call.invoke()
                } catch (e: Exception) {
                    Timber.e(e, "Error Occurred while getting api result")
                    return@withContext EitherResult.Failure(NetworkError(-1, errorMessage))
                }
            val code = response.code()

            if (response.isSuccessful) {
                val envelope: TorBoxEnvelope<Any?>? = response.body()
                return@withContext when {
                    envelope == null ->
                        EitherResult.Failure(NetworkError(-1, "$errorMessage, http code $code"))
                    envelope.success -> EitherResult.Success(Unit)
                    else -> EitherResult.Failure(TorBoxApiError(envelope.error, envelope.detail))
                }
            } else {
                try {
                    val error: TorBoxErrorBody? =
                        errorBodyAdapter.fromJson(response.errorBody()!!.string())
                    return@withContext if (error != null)
                        EitherResult.Failure(TorBoxApiError(error.error, error.detail))
                    else EitherResult.Failure(ApiConversionError(-1))
                } catch (e: IOException) {
                    Timber.e(e, "Error parsing error body")
                    return@withContext EitherResult.Failure(
                        NetworkError(-1, "$errorMessage, http code $code")
                    )
                }
            }
        }

    /**
     * Generic version of [safeApiCall] for endpoints that don't use TorBox's envelope - Kodi's
     * JSON-RPC devices, the plugin repository JSON files, and the app's own update-check endpoint
     * are all external, non-TorBox services whose responses are the plain resource, not
     * `{success, error, detail, data}`.
     */
    suspend fun <T : Any> safeApiCallRaw(call: suspend () -> Response<T>, errorMessage: String): T? {
        return try {
            val response: Response<T> = call.invoke()
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            Timber.d(e, errorMessage)
            null
        }
    }

    /** Generic version of [eitherApiResult], see [safeApiCallRaw]. */
    suspend fun <T : Any> eitherApiResultRaw(
        call: suspend () -> Response<T>,
        errorMessage: String,
    ): EitherResult<UnchainedNetworkException, T> =
        withContext(Dispatchers.IO) {
            val response: Response<T> =
                try {
                    call.invoke()
                } catch (e: Exception) {
                    Timber.e(e, "Error Occurred while getting api result")
                    return@withContext EitherResult.Failure(NetworkError(-1, errorMessage))
                }
            val code = response.code()
            if (response.isSuccessful) {
                val body: T? = response.body()
                return@withContext if (body != null) EitherResult.Success(body)
                else EitherResult.Failure(EmptyBodyError(code))
            } else {
                return@withContext EitherResult.Failure(
                    NetworkError(-1, "$errorMessage, http code $code")
                )
            }
        }

    /**
     * Get the access token saved in the db. Used by most calls to the TorBox APIs. Throws an
     * exception if token is missing or malformed.
     *
     * @return the token string
     * @throws IllegalArgumentException if not valid token is found
     */
    suspend fun getToken(): String {
        val token = protoStore.getCredentials().accessToken
        if (token.isBlank() || token.length < 5)
            throw IllegalArgumentException("Loaded token was empty or wrong: $token")

        return token
    }
}
