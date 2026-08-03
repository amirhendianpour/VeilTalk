package com.example.veiltalk.feature.auth.data

import com.example.veiltalk.common.util.ApiResult
import com.example.veiltalk.common.util.safeApiCall
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.auth.data.dto.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val json: Json,
    private val sessionManager: SessionManager,
    private val fcmTokenRepository: com.example.veiltalk.feature.notification.data.FcmTokenRepository // جدید

) {
    suspend fun register(
        firstName: String,
        lastName: String,
        email: String?,
        phoneNumber: String?,
        password: String
    ): ApiResult<RegisterResponseDto> = safeApiCall(json) {
        api.register(RegisterRequest(firstName, lastName, email, phoneNumber, password))
    }

    suspend fun requestOtp(identifier: String): ApiResult<MessageResponseDto> = safeApiCall(json) {
        api.requestOtp(OtpRequestDto(identifier))
    }

    suspend fun verifyOtp(identifier: String, code: String): ApiResult<AuthResponseDto> = safeApiCall(json) {
        api.verifyOtp(OtpVerifyRequest(identifier, code))
    }

    suspend fun loginWithPassword(identifier: String, password: String): ApiResult<AuthResponseDto> = safeApiCall(json) {
        api.loginWithPassword(LoginPasswordRequest(identifier, password))
    }

    // معادل saveAuthSession در useAuth.ts
    suspend fun persistSession(auth: AuthResponseDto) {
        val displayName = "${auth.firstName} ${auth.lastName}".trim()
        sessionManager.saveSession(auth.token, auth.username, displayName)
        fcmTokenRepository.registerCurrentDeviceToken()
    }
}