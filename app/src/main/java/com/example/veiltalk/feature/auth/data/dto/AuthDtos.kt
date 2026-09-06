package com.example.veiltalk.feature.auth.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val password: String
)

@Serializable
data class RegisterResponseDto(
    val message: String,
    val identifier: String
)

@Serializable
data class OtpRequestDto(
    val identifier: String
)

@Serializable
data class MessageResponseDto(
    val message: String
)

@Serializable
data class OtpVerifyRequest(
    val identifier: String,
    val code: String
)

@Serializable
data class LoginPasswordRequest(
    val identifier: String,
    val password: String
)

@Serializable
data class AuthResponseDto(
    val token: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null
)

@Serializable
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class PasswordResetRequest(
    val identifier: String
)

@Serializable
data class PasswordResetConfirmRequest(
    val identifier: String,
    val code: String,
    val newPassword: String
)