package com.example.veiltalk.feature.user.data

import retrofit2.Response
import retrofit2.http.*

interface BlockApi {
    @POST("api/blocks/{username}")
    suspend fun blockUser(@Path("username") username: String): Response<Map<String, String>>

    @DELETE("api/blocks/{username}")
    suspend fun unblockUser(@Path("username") username: String): Response<Map<String, String>>

    @GET("api/blocks")
    suspend fun getBlockedUsers(): Response<List<String>>

    @GET("api/blocks/check/{username}")
    suspend fun checkBlocked(@Path("username") username: String): Response<Map<String, Boolean>>
}
