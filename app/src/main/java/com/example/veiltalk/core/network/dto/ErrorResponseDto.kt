package com.example.veiltalk.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val error: String? = null
)