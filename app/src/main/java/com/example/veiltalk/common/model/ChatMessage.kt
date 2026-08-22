package com.example.veiltalk.common.model

enum class MessageType { TEXT, IMAGE, FILE, STICKER, GIF, VOICE }
enum class MessageStatus { SENT, DELIVERED, READ }

data class ChatMessage(
    val id: String,
    val sender: String,
    val recipient: String,
    val content: String,
    val messageType: MessageType,
    val fileUrl: String? = null,
    val timestamp: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val isPinned: Boolean = false,
    val replyToId: String? = null,
    val mediaKey: String? = null
)
