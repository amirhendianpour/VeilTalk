package com.example.veiltalk.common.model

data class GroupInfo(
    val id: Long,
    val name: String,
    val role: String? = null,
    val imageUrl: String? = null
)

data class GroupMessage(
    override val id: String,
    val groupId: Long,
    val sender: String?,
    override val content: String,
    override val timestamp: String?,
    override val messageType: MessageType = MessageType.TEXT,
    override val fileUrl: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    override val isPinned: Boolean = false,
    val replyToId: String? = null,
    override val mediaKey: String? = null,
    val reactions: Map<String, String> = emptyMap()
) : BaseMessage

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