package com.example.veiltalk.feature.group.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequestDto(val groupName: String)

@Serializable
data class ChatGroupDto(
    val id: Long,
    val name: String,
    val creator: String? = null,
    val createdAt: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class GroupMemberDto(
    val id: Long? = null,
    val groupId: Long,
    val username: String,
    val role: String,
    val joinedAt: String? = null
)

@Serializable
data class AddMemberRequestDto(val username: String, val role: String = "MEMBER")

@Serializable
data class GroupMemberInfoDto(
    val username: String,
    val firstName: String,
    val lastName: String,
    val profilePictureUrl: String? = null,
    val role: String
)

@Serializable
data class UpdateGroupNameRequestDto(val groupName: String)

@Serializable
data class UpdateMemberRoleRequestDto(val role: String)

@Serializable
data class GroupChatMessageDto(
    val id: String,
    val groupId: Long,
    val sender: String? = null,
    val content: String,
    val messageType: String = "TEXT",
    val fileUrl: String? = null,
    val timestamp: String? = null,
    val replyToId: String? = null,
    val mediaKey: String? = null
)

@Serializable
data class GroupUpdateEventDto(
    val type: String,
    val groupId: Long,
    val groupName: String? = null,
    val role: String? = null,
    val imageUrl: String? = null,
    val targetUsername: String? = null
)