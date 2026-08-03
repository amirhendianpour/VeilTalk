package com.example.veiltalk.feature.notification.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class FcmTokenRequestDto(val token: String)

interface FcmApi {
    @POST("api/fcm/register")
    suspend fun registerToken(@Body request: FcmTokenRequestDto): Response<Unit>

    @POST("api/fcm/unregister")
    suspend fun unregisterToken(@Body request: FcmTokenRequestDto): Response<Unit>
}