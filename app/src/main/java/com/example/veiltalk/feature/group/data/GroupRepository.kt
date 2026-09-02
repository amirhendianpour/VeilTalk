package com.example.veiltalk.feature.group.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.veiltalk.common.model.*
import com.example.veiltalk.common.util.generateId
import com.example.veiltalk.common.util.uriToMultipart
import com.example.veiltalk.core.database.dao.GroupMessageDao
import com.example.veiltalk.core.database.entity.GroupMessageEntity
import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.core.service.NotificationHelper
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.core.websocket.StompManager
import com.example.veiltalk.feature.chat.data.dto.*
import com.example.veiltalk.feature.group.data.dto.*
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class GroupRepository @Inject constructor(
    private val api: GroupApi,
    private val stompManager: StompManager,
    private val groupMessageDao: GroupMessageDao,
    private val sessionManager: SessionManager,
    private val mediaRepository: com.example.veiltalk.feature.chat.data.MediaRepository,
    private val userDirectory: UserDirectoryRepository,
    private val json: Json,
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var currentUsername: String? = null
    private var activeGroupId: Long? = null

    private val _myGroups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val myGroups: StateFlow<List<GroupInfo>> = _myGroups.asStateFlow()

    fun setActiveGroupId(groupId: Long?) {
        activeGroupId = groupId
    }

    private val _groupUpdateEvent = MutableStateFlow<GroupUpdateEvent?>(null)
    val groupUpdateEvent: StateFlow<GroupUpdateEvent?> = _groupUpdateEvent.asStateFlow()

    init {
        sessionManager.usernameFlow
            .distinctUntilChanged()
            .onEach { currentUsername = it }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/group-messages")
            .onEach { frame -> handleIncomingGroupMessage(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/group-history")
            .onEach { frame -> handleIncomingGroupMessage(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/group-updates")
            .onEach { frame -> handleGroupUpdate(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/group-receipts")
            .onEach { frame -> handleGroupReceipt(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/group-messages/delete")
            .onEach { frame -> handleRemoteGroupDeletion(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/group-reactions")
            .onEach { frame -> handleGroupReaction(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/group-pin")
            .onEach { frame -> handleRemoteGroupPin(frame.body) }
            .launchIn(scope)
    }

    private suspend fun handleIncomingGroupMessage(rawBody: String) {
        val dto = runCatching { json.decodeFromString<GroupChatMessageDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        
        // چک کن آیا این یک ویرایش روی پیام موجود در گروه است؟
        val existing = groupMessageDao.getMessageById(dto.id, me)
        if (existing != null) {
            groupMessageDao.updateMessageContent(dto.id, me, dto.content)
            return
        }

        val isGroupOpen = activeGroupId == dto.groupId
        val status = if (dto.sender == me) "SENT" else if (isGroupOpen) "READ" else "DELIVERED"
        
        groupMessageDao.upsert(
            GroupMessageEntity(
                id = dto.id,
                ownerUsername = me,
                groupId = dto.groupId,
                sender = dto.sender,
                content = dto.content,
                timestamp = dto.timestamp,
                messageType = dto.messageType,
                fileUrl = dto.fileUrl,
                status = status,
                isPinned = false,
                isForwarded = dto.isForwarded,
                replyToId = dto.replyToId,
                mediaKey = dto.mediaKey
            )
        )

        if (dto.sender != null && dto.sender != me) {
            val receipt = ReceiptDto(
                messageId = dto.id,
                recipient = dto.sender,
                status = if (isGroupOpen) "READ" else "DELIVERED",
                groupId = dto.groupId
            )
            stompManager.publish("/app/chat/receipt", json.encodeToString(receipt))
        }

        if (dto.sender != me && !isGroupOpen) {
            showGroupNotification(dto)
        }
    }

    private suspend fun handleGroupReceipt(rawBody: String) {
        val receiptDto = runCatching { json.decodeFromString<ReceiptDto>(rawBody) }.getOrNull()
        val me = currentUsername ?: return

        if (receiptDto != null) {
            if (receiptDto.messageId != null) {
                groupMessageDao.updateStatusIfHigher(receiptDto.messageId, me, receiptDto.status)
            } else if (receiptDto.groupId != null && receiptDto.status == "READ") {
                groupMessageDao.markAllSentMessagesAsRead(me, receiptDto.groupId)
            }
            return
        }

        val eventDto = runCatching { json.decodeFromString<GroupMessageStatusEventDto>(rawBody) }.getOrNull()
        if (eventDto != null) {
            if (eventDto.messageId != null) {
                groupMessageDao.updateStatusIfHigher(eventDto.messageId, me, eventDto.status)
            } else if (eventDto.status == "READ") {
                groupMessageDao.markAllSentMessagesAsRead(me, eventDto.groupId)
            }
        }
    }

    private suspend fun handleRemoteGroupDeletion(rawBody: String) {
        val dto = runCatching { json.decodeFromString<MessageDeleteDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        groupMessageDao.deleteMessages(dto.messageIds, me)
    }

    private suspend fun handleRemoteGroupEdit(rawBody: String) {
        val dto = runCatching { json.decodeFromString<GroupChatMessageDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        // آپدیت فقط محتوای پیام در گروه
        groupMessageDao.updateMessageContent(dto.id, me, dto.content)
    }

    private suspend fun handleGroupReaction(rawBody: String) {
        val dto = runCatching { json.decodeFromString<ReactionDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        
        val message = groupMessageDao.getMessageById(dto.messageId, me) ?: return
        val currentReactions = if (!message.reactionsJson.isNullOrBlank()) {
            runCatching { json.decodeFromString<Map<String, String>>(message.reactionsJson) }.getOrDefault(emptyMap())
        } else emptyMap()
        
        val newReactions = currentReactions + (dto.sender!! to dto.emoji)
        groupMessageDao.updateReactions(dto.messageId, me, json.encodeToString(newReactions))
    }

    private suspend fun handleRemoteGroupPin(rawBody: String) {
        val dto = runCatching { json.decodeFromString<PinMessageDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        groupMessageDao.updatePinStatus(dto.messageId, me, dto.pinned)
    }

    private fun showGroupNotification(dto: GroupChatMessageDto) {
        val sender = dto.sender ?: return
        scope.launch {
            userDirectory.ensureLoaded(listOf(sender))
            val groupInfo = _myGroups.value.find { it.id == dto.groupId }
            val lastEntities = groupMessageDao.getLastMessages(currentUsername ?: "", dto.groupId, 5).reversed()
            val senderUsernames = lastEntities.mapNotNull { it.sender }.distinct()
            userDirectory.ensureLoaded(senderUsernames)
            
            val notificationMessages = lastEntities.map { entity ->
                val s = entity.sender ?: "Unknown"
                NotificationHelper.NotificationMessage(
                    senderUsername = s,
                    senderName = userDirectory.getDisplayName(s),
                    content = entity.content,
                    timestamp = runCatching { Instant.parse(entity.timestamp).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
                )
            }
            
            val partnerAvatarUrl = userDirectory.getProfilePicture(sender)
            val bitmap = partnerAvatarUrl?.let { loadAvatar(it) }

            withContext(Dispatchers.Main) {
                NotificationHelper.showMessageNotification(
                    context = appContext,
                    partnerUsername = sender,
                    partnerDisplayName = userDirectory.getDisplayName(sender),
                    messages = notificationMessages,
                    avatarBitmap = bitmap,
                    isGroup = true,
                    groupId = dto.groupId,
                    groupName = groupInfo?.name ?: "گروه #${dto.groupId}"
                )
            }
        }
    }

    private suspend fun loadAvatar(url: String): Bitmap? {
        val loader = ImageLoader(appContext)
        val request = ImageRequest.Builder(appContext).data(url).allowHardware(false).build()
        return when (val result = loader.execute(request)) {
            is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
            else -> null
        }
    }

    private fun handleGroupUpdate(rawBody: String) {
        val dto = runCatching { json.decodeFromString<GroupUpdateEventDto>(rawBody) }.getOrNull() ?: return
        val event = GroupUpdateEvent(
            type = dto.type,
            groupId = dto.groupId,
            groupName = dto.groupName,
            role = dto.role,
            imageUrl = dto.imageUrl,
            targetUsername = dto.targetUsername
        )
        _groupUpdateEvent.value = event
        scope.launch { refreshMyGroups() }
    }

    fun groupMessagesFlow(groupId: Long): Flow<List<GroupMessage>> {
        return sessionManager.usernameFlow.flatMapLatest { me ->
            if (me == null) flowOf(emptyList())
            else groupMessageDao.getGroupMessagesFlow(me, groupId).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    suspend fun forwardMessagesToGroup(targetGroupId: Long, messages: List<com.example.veiltalk.common.model.ChatMessage>) {
        messages.forEach { msg ->
            sendGroupMessage(
                groupId = targetGroupId,
                content = msg.content,
                messageType = msg.messageType,
                fileUrl = msg.fileUrl,
                mediaKey = msg.mediaKey,
                isForwarded = true
            )
        }
    }

    suspend fun forwardGroupMessagesToGroup(targetGroupId: Long, messages: List<GroupMessage>) {
        messages.forEach { msg ->
            sendGroupMessage(
                groupId = targetGroupId,
                content = msg.content,
                messageType = msg.messageType,
                fileUrl = msg.fileUrl,
                mediaKey = msg.mediaKey,
                isForwarded = true
            )
        }
    }

    suspend fun deleteMessages(messageIds: List<String>) {
        val me = currentUsername ?: return
        groupMessageDao.deleteMessages(messageIds, me)
    }

    suspend fun deleteMessagesForEveryone(groupId: Long, messageIds: List<String>) {
        val me = currentUsername ?: return
        groupMessageDao.deleteMessages(messageIds, me)
        val dto = MessageDeleteDto(messageIds = messageIds, groupId = groupId)
        stompManager.publish("/app/group/delete", json.encodeToString(dto))
    }

    suspend fun togglePin(groupId: Long, messageId: String, currentPinned: Boolean, forEveryone: Boolean) {
        val me = currentUsername ?: return
        val newPinned = !currentPinned
        groupMessageDao.updatePinStatus(messageId, me, newPinned)
        
        if (forEveryone) {
            val dto = PinMessageDto(messageId = messageId, groupId = groupId, pinned = newPinned)
            stompManager.publish("/app/group/pin", json.encodeToString(dto))
        }
    }

    fun groupConversationSummariesFlow(): Flow<Map<Long, GroupSummary>> {
        return sessionManager.usernameFlow.flatMapLatest { me ->
            if (me == null) flowOf(emptyMap())
            else groupMessageDao.getAllForOwnerFlow(me).map { messages ->
                val map = mutableMapOf<Long, GroupSummary>()
                for (m in messages) {
                    val existing = map[m.groupId]
                    val isUnread = m.sender != me && m.status != "READ"
                    if (existing == null || (m.timestamp != null && (existing.timestamp == null || m.timestamp > existing.timestamp))) {
                        map[m.groupId] = GroupSummary(
                            lastMessage = formatLastMessage(m.content, m.messageType),
                            timestamp = m.timestamp,
                            unreadCount = (existing?.unreadCount ?: 0) + (if (isUnread) 1 else 0)
                        )
                    } else if (isUnread) {
                        map[m.groupId] = existing.copy(unreadCount = existing.unreadCount + 1)
                    }
                }
                map
            }
        }
    }

    private fun formatLastMessage(content: String, type: String): String {
        return when (type) {
            "IMAGE" -> "📷 تصویر"
            "VOICE" -> "🎤 پیام صوتی"
            "FILE" -> "📁 فایل"
            "STICKER" -> "🏷️ استیکر"
            "GIF" -> "🎬 گیف"
            "CONTACT" -> "👤 مخاطب"
            else -> content
        }
    }

    data class GroupSummary(val lastMessage: String, val timestamp: String?, val unreadCount: Int)

    suspend fun sendGroupMessage(groupId: Long, content: String, messageType: MessageType = MessageType.TEXT, fileUrl: String? = null, replyToId: String? = null, mediaKey: String? = null, isForwarded: Boolean = false) {
        val me = currentUsername ?: return
        val id = generateId()
        val nowIso = Instant.now().toString()
        
        groupMessageDao.upsert(
            GroupMessageEntity(
                id = id,
                ownerUsername = me,
                groupId = groupId,
                sender = me,
                content = content,
                timestamp = nowIso,
                messageType = messageType.name,
                fileUrl = fileUrl,
                status = "SENT",
                isForwarded = isForwarded,
                replyToId = replyToId,
                mediaKey = mediaKey
            )
        )

        val dto = GroupChatMessageDto(
            id = id, 
            groupId = groupId, 
            sender = me, 
            content = content, 
            messageType = messageType.name, 
            fileUrl = fileUrl, 
            timestamp = null,
            replyToId = replyToId,
            mediaKey = mediaKey,
            isForwarded = isForwarded
        )
        stompManager.publish("/app/group/chat", json.encodeToString(dto))
    }

    suspend fun sendImageMessage(groupId: Long, uri: android.net.Uri): Result<Unit> {
        return mediaRepository.uploadFile(uri).map { uploaded ->
            sendGroupMessage(groupId, uploaded.thumbnail ?: "", MessageType.IMAGE, uploaded.fileUrl, mediaKey = uploaded.mediaKey)
        }
    }

    suspend fun sendVoiceMessage(groupId: Long, file: java.io.File): Result<Unit> {
        val bytes = withContext(Dispatchers.IO) { file.readBytes() }
        val fileName = file.name
        return mediaRepository.uploadBytes(bytes, "audio/m4a", fileName = fileName).map { uploaded ->
            sendGroupMessage(groupId, fileName, MessageType.VOICE, uploaded.fileUrl, mediaKey = uploaded.mediaKey)
            file.delete()
        }
    }

    suspend fun sendFileMessage(groupId: Long, uri: android.net.Uri): Result<Unit> {
        return mediaRepository.uploadFile(uri).map { uploaded ->
            sendGroupMessage(groupId, uploaded.displayName, MessageType.FILE, uploaded.fileUrl, mediaKey = uploaded.mediaKey)
        }
    }

    suspend fun sendReaction(groupId: Long, messageId: String, emoji: String) {
        val me = currentUsername ?: return
        val dto = ReactionDto(messageId = messageId, emoji = emoji, groupId = groupId)
        stompManager.publish("/app/group/reaction", json.encodeToString(dto))
        
        val message = groupMessageDao.getMessageById(messageId, me) ?: return
        val currentReactions = if (!message.reactionsJson.isNullOrBlank()) {
            runCatching { json.decodeFromString<Map<String, String>>(message.reactionsJson) }.getOrDefault(emptyMap())
        } else emptyMap()
        
        val newReactions = currentReactions + (me to emoji)
        groupMessageDao.updateReactions(messageId, me, json.encodeToString(newReactions))
    }

    suspend fun editGroupMessage(groupId: Long, messageId: String, newContent: String) {
        val me = currentUsername ?: return
        val dto = GroupChatMessageDto(
            id = messageId, 
            groupId = groupId, 
            sender = me, 
            content = newContent, 
            messageType = MessageType.TEXT.name, 
            fileUrl = null, 
            timestamp = Instant.now().toString()
        )
        stompManager.publish("/app/group/edit", json.encodeToString(dto))
        groupMessageDao.updateMessageContent(messageId, me, newContent)
    }

    suspend fun markGroupAsRead(groupId: Long) {
        val me = currentUsername ?: return
        groupMessageDao.markGroupAsRead(me, groupId)
        val request = GroupReadRequestDto(groupId)
        stompManager.publish("/app/group/read", json.encodeToString(request))
    }

    suspend fun refreshMyGroups() {
        val membershipsResp = runCatching { api.getMyGroups() }.getOrNull()
        if (membershipsResp == null || !membershipsResp.isSuccessful) return
        val groups = membershipsResp.body().orEmpty().map { member ->
            val detail = runCatching { api.getGroupById(member.groupId) }.getOrNull()?.body()
            GroupInfo(member.groupId, detail?.name ?: "گروه #${member.groupId}", member.role, detail?.imageUrl)
        }
        _myGroups.value = groups
    }

    suspend fun createGroup(name: String): Result<GroupInfo> {
        return try {
            val response = api.createGroup(CreateGroupRequestDto(name))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val info = GroupInfo(body.id, body.name, "ADMIN", body.imageUrl)
                _myGroups.value = listOf(info) + _myGroups.value
                Result.success(info)
            } else Result.failure(Exception("Error"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getGroupMembersInfo(groupId: Long): Result<List<GroupMemberInfo>> {
        return try {
            val response = api.getGroupMembersInfo(groupId)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty().map {
                    GroupMemberInfo(it.username, it.firstName, it.lastName, it.profilePictureUrl, it.role)
                })
            } else {
                Result.failure(Exception("خطا در دریافت لیست اعضا"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(groupId: Long, username: String): Result<Unit> {
        return try {
            val response = api.addMember(groupId, AddMemberRequestDto(username))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "خطا در افزودن عضو"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroupName(groupId: Long, newName: String): Result<GroupInfo> {
        return try {
            val response = api.updateGroupName(groupId, UpdateGroupNameRequestDto(newName))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val info = GroupInfo(body.id, body.name, imageUrl = body.imageUrl)
                updateLocalGroup(info)
                Result.success(info)
            } else {
                Result.failure(Exception("خطا در تغییر نام گروه"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadGroupImage(groupId: Long, imageUri: android.net.Uri): Result<GroupInfo> {
        val part = uriToMultipart(appContext, imageUri) ?: return Result.failure(Exception("فایل نامعتبر است."))
        return try {
            val response = api.uploadGroupImage(groupId, part)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val info = GroupInfo(body.id, body.name, imageUrl = body.imageUrl)
                updateLocalGroup(info)
                Result.success(info)
            } else {
                Result.failure(Exception("خطا در آپلود عکس گروه"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(groupId: Long, username: String, role: String): Result<Unit> {
        return try {
            val response = api.updateMemberRole(groupId, username, UpdateMemberRoleRequestDto(role))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "خطا در تغییر نقش عضو"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(groupId: Long, username: String): Result<Unit> {
        return try {
            val response = api.removeMember(groupId, username)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(response.errorBody()?.string() ?: "خطا در حذف عضو"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(groupId: Long): Result<Unit> {
        return try {
            val response = api.deleteGroup(groupId)
            if (response.isSuccessful) {
                _myGroups.value = _myGroups.value.filterNot { it.id == groupId }
                Result.success(Unit)
            } else {
                Result.failure(Exception("خطا در حذف گروه"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(groupId: Long) {
        val me = currentUsername ?: return
        groupMessageDao.deleteGroupConversation(me, groupId)
    }

    private fun updateLocalGroup(updated: GroupInfo) {
        _myGroups.value = _myGroups.value.map { if (it.id == updated.id) it.copy(name = updated.name, imageUrl = updated.imageUrl) else it }
    }

    suspend fun ensureUsernameLoaded() {
        if (currentUsername == null) currentUsername = sessionManager.usernameFlow.first()
    }

    private fun GroupMessageEntity.toDomain(): GroupMessage {
        val reactionsMap: Map<String, String> = if (!reactionsJson.isNullOrBlank()) {
            runCatching { Json.decodeFromString<Map<String, String>>(reactionsJson!!) }.getOrDefault(emptyMap())
        } else emptyMap()

        return GroupMessage(
            id = id,
            groupId = groupId,
            sender = sender,
            content = content,
            timestamp = timestamp,
            messageType = runCatching { MessageType.valueOf(messageType) }.getOrDefault(MessageType.TEXT),
            fileUrl = fileUrl,
            status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
            isPinned = isPinned,
            replyToId = replyToId,
            mediaKey = mediaKey,
            isForwarded = isForwarded,
            reactions = reactionsMap
        )
    }
}
