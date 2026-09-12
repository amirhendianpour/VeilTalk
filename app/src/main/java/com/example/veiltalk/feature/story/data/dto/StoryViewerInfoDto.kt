package com.example.veiltalk.feature.story.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoryViewerInfoDto(
    val username: String,
    val displayName: String,
    val profilePictureUrl: String?,
    val liked: Boolean,
    val reactionEmoji: String?,
    val viewedAt: String
)