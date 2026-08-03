package com.example.veiltalk.feature.auth.ui

import com.example.veiltalk.feature.auth.data.dto.AuthResponseDto

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resendMessage: String? = null
)

sealed class AuthEvent {
    data class OtpRequested(val identifier: String) : AuthEvent()
    data class Authenticated(val auth: AuthResponseDto) : AuthEvent()
}