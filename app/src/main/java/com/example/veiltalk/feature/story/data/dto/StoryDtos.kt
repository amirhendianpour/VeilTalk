package com.example.veiltalk.feature.story.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoryResponseDto(
    val id: Long,
    val creatorUsername: String,
    val creatorDisplayName: String,
    val creatorProfilePicture: String?,
    val mediaUrl: String,
    val caption: String?,
    val mediaType: String,
    val createdAt: String,
    val liked: Boolean = false,
    val reactionEmoji: String? = null
)

@Serializable
data class PostStoryRequest(
    val mediaUrl: String,
    val caption: String = "",
    val type: String = "IMAGE"
)
