package com.example.veiltalk.common.model

data class GroupInfo(
    val id: Long,
    val name: String,
    val role: String? = null,
    val imageUrl: String? = null
)

data class GroupMessage(
    val id: String,
    val groupId: Long,
    val sender: String?,
    val content: String,
    val timestamp: String?,
    val isPinned: Boolean = false
)

data class GroupMemberInfo(
    val username: String,
    val firstName: String,
    val lastName: String,
    val profilePictureUrl: String?,
    val role: String
)

data class GroupUpdateEvent(
    val type: String, // ADDED, DELETED, IMAGE_UPDATED, NAME_UPDATED, ROLE_UPDATED, REMOVED, MEMBER_REMOVED
    val groupId: Long,
    val groupName: String?,
    val role: String?,
    val imageUrl: String?,
    val targetUsername: String?
)