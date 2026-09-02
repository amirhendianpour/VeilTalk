package com.example.veiltalk.common.model

enum class MessageType { TEXT, IMAGE, FILE, STICKER, GIF, VOICE, CONTACT }
enum class MessageStatus { SENT, DELIVERED, READ }

interface BaseMessage {
    val id: String
    val content: String
    val messageType: MessageType
    val timestamp: String?
    val isPinned: Boolean
    val fileUrl: String?
    val mediaKey: String?
}

data class ChatMessage(
    override val id: String,
    val sender: String,
    val recipient: String,
    override val content: String,
    override val messageType: MessageType,
    override val fileUrl: String? = null,
    override val timestamp: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    override val isPinned: Boolean = false,
    val replyToId: String? = null,
    override val mediaKey: String? = null,
    val isForwarded: Boolean = false,
    val reactions: Map<String, String> = emptyMap() // key: username, value: emoji
) : BaseMessage
