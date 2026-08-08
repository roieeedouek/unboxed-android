package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.User
import javax.inject.Inject
import retrofit2.Response

class UserApiHelperImpl @Inject constructor(private val userApi: UserApi) : UserApiHelper {
    override suspend fun getUser(token: String): Response<TorBoxEnvelope<User>> =
        userApi.getUser(token)
}
