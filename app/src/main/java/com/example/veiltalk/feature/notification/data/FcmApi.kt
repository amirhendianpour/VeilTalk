package com.example.veiltalk.feature.notification.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class FcmTokenRequestDto(
    val token: String,
    val deviceName: String? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null
)

@Serializable
data class ActiveSessionDto(
    val id: Long,
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val ipAddress: String,
    val lastActive: String? = null,
    val isCurrent: Boolean
)

interface FcmApi {
    @POST("api/fcm/register")
    suspend fun registerToken(@Body request: FcmTokenRequestDto): Response<Unit>

    @POST("api/fcm/unregister")
    suspend fun unregisterToken(@Body request: FcmTokenRequestDto): Response<Unit>

    @GET("api/fcm/sessions")
    suspend fun getActiveSessions(): Response<List<ActiveSessionDto>>

    @DELETE("api/fcm/sessions/{id}")
    suspend fun terminateSession(@Path("id") id: Long): Response<Unit>

    @DELETE("api/fcm/sessions/others")
    suspend fun terminateOtherSessions(@Query("currentToken") currentToken: String): Response<Unit>
}