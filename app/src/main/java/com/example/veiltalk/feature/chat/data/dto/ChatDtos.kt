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
    val timestamp: String? = null,
    val replyToId: String? = null,
    val mediaKey: String? = null
)

@Serializable
data class ReceiptDto(
    val messageId: String? = null,
    val sender: String? = null,
    val recipient: String,
    val status: String,
    val groupId: Long? = null
)

@Serializable
data class GroupReadRequestDto(
    val groupId: Long
)

@Serializable
data class GroupMessageStatusEventDto(
    val messageId: String? = null,
    val groupId: Long,
    val status: String
)

@Serializable
data class TypingEventDto(
    val sender: String? = null,
    val recipient: String,
    val typing: Boolean
)

@Serializable
data class UserStatusDto(
    val username: String,
    val online: Boolean,
    val lastSeen: String? = null
)

@Serializable
data class MessageDeleteDto(
    val messageIds: List<String>,
    val recipient: String? = null,
    val groupId: Long? = null
)

@Serializable
data class ReactionDto(
    val messageId: String,
    val emoji: String,
    val sender: String? = null, // Backend set this
    val recipient: String? = null, // for private chat
    val groupId: Long? = null // for group chat
)

@Serializable
data class PinMessageDto(
    val messageId: String,
    val recipient: String? = null,
    val groupId: Long? = null,
    val pinned: Boolean
)
