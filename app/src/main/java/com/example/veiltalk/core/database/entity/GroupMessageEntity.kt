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
    val isPinned: Boolean = false
)