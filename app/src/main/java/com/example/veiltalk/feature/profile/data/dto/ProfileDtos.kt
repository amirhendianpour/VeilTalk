package com.example.veiltalk.feature.profile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponseDto(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null
)

@Serializable
data class ProfileUpdateRequestDto(
    val firstName: String,
    val lastName: String,
    val bio: String,
    val email: String? = null,
    val phoneNumber: String? = null
)
