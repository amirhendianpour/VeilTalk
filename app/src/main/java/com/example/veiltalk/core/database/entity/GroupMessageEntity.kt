package com.example.veiltalk.core.database.entity

import androidx.room.Entity

@Entity(tableName = "group_messages", primaryKeys = ["id", "ownerUsername"])
data class GroupMessageEntity(
    val id: String,
    val ownerUsername: String,
    val groupId: Long,
    val sender: String?,
    val content: String,
    val timestamp: String?,
    val messageType: String = "TEXT",
    val fileUrl: String? = null,
    val status: String = "SENT",
    val isPinned: Boolean = false,
    val replyToId: String? = null,
    val mediaKey: String? = null,
    val reactionsJson: String? = null
)

