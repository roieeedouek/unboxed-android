package com.github.livingwithhippos.unchained.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

class ProtoStoreImpl @Inject constructor(@param:ApplicationContext private val context: Context) :
    ProtoStore {

    override val credentialsFlow: Flow<Credentials.CurrentCredential> =
        context.credentialsDataStore.data.catch { exception ->
            if (exception is IOException) {
                exception.printStackTrace()
                emit(Credentials.CurrentCredential.getDefaultInstance())
            } else {
                throw exception
            }
        }

    override suspend fun setCredentials(accessToken: String, authMethod: String) {
        context.credentialsDataStore.updateData { credentials ->
            credentials
                .toBuilder()
                .setAccessToken(accessToken)
                .setAuthMethod(authMethod)
                .build()
        }
    }

    override suspend fun updateAccessToken(accessToken: String) {
        context.credentialsDataStore.updateData { credentials ->
            credentials.toBuilder().setAccessToken(accessToken).build()
        }
    }

    override suspend fun deleteCredentials() {
        context.credentialsDataStore.updateData { it.toBuilder().clear().build() }
    }

    override suspend fun getCredentials(): Credentials.CurrentCredential {
        return try {
            credentialsFlow.first()
        } catch (e: Exception) {
            e.printStackTrace()
            Credentials.CurrentCredential.getDefaultInstance()
        }
    }
}
