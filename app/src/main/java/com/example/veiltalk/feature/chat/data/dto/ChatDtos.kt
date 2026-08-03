package com.example.veiltalk.feature.chat.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val id: String,
    val sender: String? = null,
    val recipient: String,
    val content: String,
    val messageType: String = "TEXT",
    val fileUrl: String? = null,
    val timestamp: String? = null
)

@Serializable
data class ReceiptDto(
    val messageId: String,
    val sender: String? = null,
    val recipient: String,
    val status: String
)

@Serializable
data class TypingEventDto(
    val sender: String? = null,
    val recipient: String,
    val typing: Boolean
)