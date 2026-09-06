package com.example.veiltalk.feature.auth.data

import com.example.veiltalk.feature.auth.data.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponseDto>

    @POST("api/auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequestDto): Response<MessageResponseDto>

    @POST("api/auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<AuthResponseDto>

    @POST("api/auth/login/password")
    suspend fun loginWithPassword(@Body request: LoginPasswordRequest): Response<AuthResponseDto>

    @POST("api/auth/password/change")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponseDto>

    @POST("api/auth/password/reset/request")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<MessageResponseDto>

    @POST("api/auth/password/reset/confirm")
    suspend fun confirmPasswordReset(@Body request: PasswordResetConfirmRequest): Response<MessageResponseDto>
}