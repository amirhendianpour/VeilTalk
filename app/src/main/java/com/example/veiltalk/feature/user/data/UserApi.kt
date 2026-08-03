package com.example.veiltalk.feature.user.data

import com.example.veiltalk.feature.user.data.dto.BatchInfoRequestDto
import com.example.veiltalk.feature.user.data.dto.UserInfoDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserApi {

    @GET("api/users/lookup")
    suspend fun lookupUser(@Query("identifier") identifier: String): Response<UserInfoDto>

    @POST("api/users/batch-info")
    suspend fun batchInfo(@Body request: BatchInfoRequestDto): Response<List<UserInfoDto>>
}