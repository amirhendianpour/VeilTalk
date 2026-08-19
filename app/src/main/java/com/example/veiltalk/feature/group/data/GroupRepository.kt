package com.example.veiltalk.feature.group.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.veiltalk.common.model.GroupInfo
import com.example.veiltalk.common.model.GroupMemberInfo
import com.example.veiltalk.common.model.GroupMessage
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.common.model.GroupUpdateEvent
import com.example.veiltalk.common.util.generateId
import com.example.veiltalk.common.util.uriToMultipart
import com.example.veiltalk.core.database.dao.GroupMessageDao
import com.example.veiltalk.core.database.entity.GroupMessageEntity
import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.core.service.NotificationHelper
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.core.websocket.StompManager
import com.example.veiltalk.feature.chat.data.dto.ReceiptDto
import com.example.veiltalk.feature.chat.data.dto.GroupReadRequestDto
import com.example.veiltalk.feature.chat.data.dto.GroupMessageStatusEventDto
import com.example.veiltalk.feature.group.data.dto.*
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val api: GroupApi,
    private val stompManager: StompManager,
    private val groupMessageDao: GroupMessageDao,
    private val sessionManager: SessionManager,
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
    }

    private suspend fun handleIncomingGroupMessage(rawBody: String) {
        val dto = runCatching { json.decodeFromString<GroupChatMessageDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        
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
                isPinned = false
            )
        )

        // ارسال رسید به فرستنده (اگر پیام از طرف ما نیست)
        if (dto.sender != null && dto.sender != me) {
            val receipt = ReceiptDto(
                messageId = dto.id,
                recipient = dto.sender,
                status = if (isGroupOpen) "READ" else "DELIVERED",
                groupId = dto.groupId
            )
            stompManager.publish("/app/chat/receipt", json.encodeToString(ReceiptDto.serializer(), receipt))
        }

        // نمایش نوتیفیکیشن اگر کاربر در صفحه این گروه نیست
        if (dto.sender != me && !isGroupOpen) {
            showGroupNotification(dto)
        }
    }

    private suspend fun handleGroupReceipt(rawBody: String) {
        // سرور رسیدهای رله شده را با مدل ReceiptDto یا GroupMessageStatusEventDto می‌فرستد
        // ابتدا با ReceiptDto امتحان می‌کنیم
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

        // اگر نشد، با GroupMessageStatusEventDto امتحان می‌کنیم
        val eventDto = runCatching { json.decodeFromString<GroupMessageStatusEventDto>(rawBody) }.getOrNull()
        if (eventDto != null) {
            if (eventDto.messageId != null) {
                groupMessageDao.updateStatusIfHigher(eventDto.messageId, me, eventDto.status)
            } else if (eventDto.status == "READ") {
                groupMessageDao.markAllSentMessagesAsRead(me, eventDto.groupId)
            }
        }
    }

    private fun showGroupNotification(dto: GroupChatMessageDto) {
        val sender = dto.sender ?: return
        scope.launch {
            userDirectory.ensureLoaded(listOf(sender))
            val groupInfo = _myGroups.value.find { it.id == dto.groupId }
            
            // دریافت آخرین پیام‌های گروه برای نمایش تاریخچه در نوتیفیکیشن
            val lastEntities = groupMessageDao.getLastMessages(currentUsername ?: "", dto.groupId, 5).reversed()
            
            // لود کردن نام فرستنده‌های تمام پیام‌های اخیر
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
                    partnerUsername = sender, // اینجا در گروه، پارتنر اصلی همون فرستنده پیام آخره
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
        val request = ImageRequest.Builder(appContext)
            .data(url)
            .allowHardware(false)
            .build()
        
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
        val me = currentUsername ?: return flowOf(emptyList())
        return groupMessageDao.getGroupMessagesFlow(me, groupId).map { list ->
            list.map { entity ->
                GroupMessage(
                    id = entity.id,
                    groupId = entity.groupId,
                    sender = entity.sender,
                    content = entity.content,
                    timestamp = entity.timestamp,
                    messageType = runCatching { MessageType.valueOf(entity.messageType) }.getOrDefault(MessageType.TEXT),
                    fileUrl = entity.fileUrl,
                    status = runCatching { com.example.veiltalk.common.model.MessageStatus.valueOf(entity.status) }.getOrDefault(com.example.veiltalk.common.model.MessageStatus.SENT),
                    isPinned = entity.isPinned
                )
            }
        }
    }

    suspend fun deleteMessages(messageIds: List<String>) {
        val me = currentUsername ?: return
        groupMessageDao.deleteMessages(messageIds, me)
    }

    suspend fun togglePin(messageId: String, currentPinned: Boolean) {
        val me = currentUsername ?: return
        groupMessageDao.updatePinStatus(messageId, me, !currentPinned)
    }

    // لیست خلاصه‌ی پیام‌های گروه‌ها
    fun groupConversationSummariesFlow(): Flow<Map<Long, GroupSummary>> {
        val me = currentUsername ?: return flowOf(emptyMap())
        return groupMessageDao.getAllForOwnerFlow(me).map { messages ->
            val map = mutableMapOf<Long, GroupSummary>()
            for (m in messages) {
                val existing = map[m.groupId]
                
                val isUnread = m.sender != me && m.status != "READ"
                
                if (existing == null || (m.timestamp != null && (existing.timestamp == null || m.timestamp > existing.timestamp))) {
                    map[m.groupId] = GroupSummary(
                        lastMessage = m.content,
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

data class GroupSummary(
    val lastMessage: String,
    val timestamp: String?,
    val unreadCount: Int
)

    suspend fun sendGroupMessage(
        groupId: Long, 
        content: String, 
        messageType: MessageType = MessageType.TEXT, 
        fileUrl: String? = null
    ) {
        val me = currentUsername ?: return
        val id = generateId()
        val nowIso = Instant.now().toString()

        // نمایش فوری برای خود فرستنده
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
                status = "SENT"
            )
        )

        val dto = GroupChatMessageDto(
            id = id, 
            groupId = groupId, 
            content = content,
            messageType = messageType.name,
            fileUrl = fileUrl
        )
        stompManager.publish("/app/group/chat", json.encodeToString(GroupChatMessageDto.serializer(), dto))
    }

    suspend fun markGroupAsRead(groupId: Long) {
        val me = currentUsername ?: return
        
        // آپدیت دیتابیس محلی
        groupMessageDao.markGroupAsRead(me, groupId)
        
        // اطلاع به سرور برای آپدیت وضعیت برای بقیه
        val request = GroupReadRequestDto(groupId = groupId)
        stompManager.publish("/app/group/read", json.encodeToString(GroupReadRequestDto.serializer(), request))
    }

    suspend fun refreshMyGroups() {
        val membershipsResp = runCatching { api.getMyGroups() }.getOrNull()
        if (membershipsResp == null || !membershipsResp.isSuccessful) return
        val memberships = membershipsResp.body().orEmpty()

        val groups = memberships.map { member ->
            val detailResp = runCatching { api.getGroupById(member.groupId) }.getOrNull()
            val detail = detailResp?.takeIf { it.isSuccessful }?.body()
            GroupInfo(
                id = member.groupId,
                name = detail?.name ?: "گروه #${member.groupId}",
                role = member.role,
                imageUrl = detail?.imageUrl
            )
        }
        _myGroups.value = groups
    }

    suspend fun createGroup(name: String): Result<GroupInfo> {
        return try {
            val response = api.createGroup(CreateGroupRequestDto(name))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val info = GroupInfo(id = body.id, name = body.name, role = "ADMIN", imageUrl = body.imageUrl)
                _myGroups.value = listOf(info) + _myGroups.value
                Result.success(info)
            } else {
                Result.failure(Exception("خطا در ساخت گروه"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        _myGroups.value = _myGroups.value.map {
            if (it.id == updated.id) it.copy(name = updated.name, imageUrl = updated.imageUrl) else it
        }
    }

    suspend fun ensureUsernameLoaded() {
        if (currentUsername == null) currentUsername = sessionManager.usernameFlow.first()
    }
}