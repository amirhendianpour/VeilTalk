package com.example.veiltalk.feature.group.data

import android.content.Context
import com.example.veiltalk.common.model.GroupInfo
import com.example.veiltalk.common.model.GroupMemberInfo
import com.example.veiltalk.common.model.GroupMessage
import com.example.veiltalk.common.model.GroupUpdateEvent
import com.example.veiltalk.common.util.generateId
import com.example.veiltalk.common.util.uriToMultipart
import com.example.veiltalk.core.database.dao.GroupMessageDao
import com.example.veiltalk.core.database.entity.GroupMessageEntity
import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.core.websocket.StompManager
import com.example.veiltalk.feature.group.data.dto.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val json: Json,
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var currentUsername: String? = null

    private val _myGroups = MutableStateFlow<List<GroupInfo>>(emptyList())
    val myGroups: StateFlow<List<GroupInfo>> = _myGroups.asStateFlow()

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
    }

    private suspend fun handleIncomingGroupMessage(rawBody: String) {
        val dto = runCatching { json.decodeFromString<GroupChatMessageDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        groupMessageDao.upsert(
            GroupMessageEntity(
                id = dto.id,
                ownerUsername = me,
                groupId = dto.groupId,
                sender = dto.sender,
                content = dto.content,
                timestamp = dto.timestamp
            )
        )
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
            list.map { GroupMessage(it.id, it.groupId, it.sender, it.content, it.timestamp) }
        }
    }

    // آخرین زمان پیام هر گروه — برای مرتب‌سازی در تب "همه" (معادل getLastGroupMessageTime در Sidebar.tsx)
    fun latestGroupMessageTimesFlow(): Flow<Map<Long, String?>> {
        val me = currentUsername ?: return flowOf(emptyMap())
        return groupMessageDao.getAllForOwnerFlow(me).map { messages ->
            val map = mutableMapOf<Long, String?>()
            for (m in messages) {
                val existing = map[m.groupId]
                if (existing == null || (m.timestamp != null && m.timestamp > existing)) {
                    map[m.groupId] = m.timestamp
                }
            }
            map
        }
    }

    suspend fun sendGroupMessage(groupId: Long, content: String) {
        val me = currentUsername ?: return
        val id = generateId()
        val nowIso = Instant.now().toString()

        // نمایش فوری برای خود فرستنده — چون بک‌اند پیام رو به خود فرستنده برنمی‌گردونه
        groupMessageDao.upsert(GroupMessageEntity(id, me, groupId, me, content, nowIso))

        val dto = GroupChatMessageDto(id = id, groupId = groupId, content = content)
        stompManager.publish("/app/group/chat", json.encodeToString(GroupChatMessageDto.serializer(), dto))
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

    private fun updateLocalGroup(updated: GroupInfo) {
        _myGroups.value = _myGroups.value.map {
            if (it.id == updated.id) it.copy(name = updated.name, imageUrl = updated.imageUrl) else it
        }
    }

    suspend fun ensureUsernameLoaded() {
        if (currentUsername == null) currentUsername = sessionManager.usernameFlow.first()
    }
}