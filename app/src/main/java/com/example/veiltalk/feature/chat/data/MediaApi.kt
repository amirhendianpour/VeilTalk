package com.example.veiltalk.feature.chat.data

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@Serializable
data class MediaUploadResponseDto(val fileUrl: String)

interface MediaApi {
    @Multipart
    @POST("api/media/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<MediaUploadResponseDto>
}