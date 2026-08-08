package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.TorBoxEnvelope
import com.github.livingwithhippos.unchained.data.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface UserApi {
    @GET("user/me")
    suspend fun getUser(@Header("Authorization") token: String): Response<TorBoxEnvelope<User>>
}
