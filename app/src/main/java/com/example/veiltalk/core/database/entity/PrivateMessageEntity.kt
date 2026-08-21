package com.example.veiltalk.core.database.entity

import androidx.room.Entity

@Entity(tableName = "private_messages", primaryKeys = ["id", "ownerUsername"])
data class PrivateMessageEntity(
    val id: String,
    val ownerUsername: String, // برای پشتیبانی چند اکانت روی یک دستگاه (معادل جدا بودن dbName بر اساس username در chatDB.ts)
    val sender: String,
    val recipient: String,
    val content: String,
    val messageType: String,
    val fileUrl: String?,
    val timestamp: String?,
    val status: String, // SENT, DELIVERED, READ
    val isPinned: Boolean = false,
    val replyToId: String? = null,
    val mediaKey: String? = null
)
