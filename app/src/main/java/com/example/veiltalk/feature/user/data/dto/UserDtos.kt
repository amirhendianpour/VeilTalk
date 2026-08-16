package com.example.veiltalk.feature.user.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserInfoDto(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val profilePictureUrl: String? = null
)

@Serializable
data class BatchInfoRequestDto(
    val usernames: List<String>
)