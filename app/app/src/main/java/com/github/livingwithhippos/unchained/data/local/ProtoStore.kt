package com.github.livingwithhippos.unchained.data.local

import kotlinx.coroutines.flow.Flow

interface ProtoStore {

    val credentialsFlow: Flow<Credentials.CurrentCredential>

    /** Persists a freshly obtained, permanent TorBox API token, replacing any previous one. */
    suspend fun setCredentials(accessToken: String, authMethod: String)

    /** Updates just the access token, e.g. after the user pastes a new one. */
    suspend fun updateAccessToken(accessToken: String)

    suspend fun deleteCredentials()

    suspend fun getCredentials(): Credentials.CurrentCredential
}
