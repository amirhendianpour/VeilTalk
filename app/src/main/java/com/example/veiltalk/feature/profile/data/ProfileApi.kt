package com.example.veiltalk.feature.profile.data

import com.example.veiltalk.feature.profile.data.dto.ProfileUpdateRequestDto
import com.example.veiltalk.feature.profile.data.dto.UserProfileResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ProfileApi {

    @GET("api/users/me")
    suspend fun getMyProfile(): Response<UserProfileResponseDto>

    @PUT("api/users/me")
    suspend fun updateMyProfile(@Body request: ProfileUpdateRequestDto): Response<UserProfileResponseDto>

    @Multipart
    @POST("api/users/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): Response<UserProfileResponseDto>
}