package com.example.veiltalk.feature.chat.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.veiltalk.common.model.ChatMessage
import com.example.veiltalk.common.model.MessageStatus
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.common.util.generateId
import com.example.veiltalk.core.database.dao.MessageDao
import com.example.veiltalk.core.database.entity.PrivateMessageEntity
import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.core.service.NotificationHelper
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.core.websocket.StompManager
import com.example.veiltalk.feature.chat.data.dto.ChatMessageDto
import com.example.veiltalk.feature.chat.data.dto.ReceiptDto
import com.example.veiltalk.feature.chat.data.dto.TypingEventDto
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val stompManager: StompManager,
    private val messageDao: MessageDao,
    private val sessionManager: SessionManager,
    private val userDirectory: UserDirectoryRepository,
    private val json: Json,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var currentUsername: String? = null
    private var activeChatPartner: String? = null

    private val _typingUsers = MutableStateFlow<Set<String>>(emptySet())
    val typingUsers: StateFlow<Set<String>> = _typingUsers.asStateFlow()

    fun setActiveChatPartner(partner: String?) {
        activeChatPartner = partner
    }

    init {
        // نگه‌داشتن username جاری برای برچسب‌زدن رکوردهای محلی
        sessionManager.usernameFlow
            .distinctUntilChanged()
            .onEach { currentUsername = it }
            .launchIn(scope)

        // گوش دادن دائمی به فریم‌های سوکت — مستقل از باز/بسته بودن صفحه چت (معادل mount شدن WebSocketProvider در ریشه)
        stompManager.framesForDestination("/user/queue/messages")
            .onEach { frame -> handleIncomingMessage(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/receipts")
            .onEach { frame -> handleReceipt(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/typing")
            .onEach { frame -> handleTyping(frame.body) }
            .launchIn(scope)
    }

    private suspend fun handleIncomingMessage(rawBody: String) {
        val dto = runCatching { json.decodeFromString<ChatMessageDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return

        messageDao.upsert(dto.toEntity(ownerUsername = me, status = "DELIVERED"))

        // ارسال رسید تحویل
        if (dto.sender != null && dto.sender != me) {
            val receipt = ReceiptDto(
                messageId = dto.id,
                recipient = dto.sender,
                status = "DELIVERED"
            )
            stompManager.publish("/app/chat/receipt", json.encodeToString(ReceiptDto.serializer(), receipt))
            
            // اگر کاربر در صفحه چتِ این شخص نیست، نوتیفیکیشن نشان بده
            if (activeChatPartner != dto.sender) {
                showNotificationForMessage(dto)
            }
        }
    }

    private fun showNotificationForMessage(dto: ChatMessageDto) {
        val sender = dto.sender ?: return
        scope.launch {
            // دریافت اطلاعات فرستنده (نام و آواتار)
            userDirectory.ensureLoaded(listOf(sender))
            val displayName = userDirectory.getDisplayName(sender)
            val avatarUrl = userDirectory.getProfilePicture(sender)
            
            // دریافت آخرین پیام‌های این گفتگو برای نمایش در نوتیفیکیشن
            val lastMessages = messageDao.getLastMessages(currentUsername ?: "", sender, 5)
                .reversed()
                .map { entity ->
                    NotificationHelper.NotificationMessage(
                        senderUsername = entity.sender,
                        senderName = if (entity.sender == sender) displayName else "Me",
                        content = entity.content,
                        timestamp = runCatching { Instant.parse(entity.timestamp).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
                    )
                }
            
            val bitmap = avatarUrl?.let { loadAvatar(it) }

            withContext(Dispatchers.Main) {
                NotificationHelper.showMessageNotification(
                    context = context,
                    partnerUsername = sender,
                    partnerDisplayName = displayName,
                    messages = lastMessages,
                    avatarBitmap = bitmap
                )
            }
        }
    }

    private suspend fun loadAvatar(url: String): Bitmap? {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        
        return when (val result = loader.execute(request)) {
            is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
            else -> null
        }
    }

    private suspend fun handleReceipt(rawBody: String) {
        val dto = runCatching { json.decodeFromString<ReceiptDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        messageDao.updateStatus(dto.messageId, me, dto.status)
    }

    private fun handleTyping(rawBody: String) {
        val dto = runCatching { json.decodeFromString<TypingEventDto>(rawBody) }.getOrNull() ?: return
        val sender = dto.sender ?: return
        _typingUsers.value = if (dto.typing) {
            _typingUsers.value + sender
        } else {
            _typingUsers.value - sender
        }
    }

    fun conversationFlow(partner: String): Flow<List<ChatMessage>> {
        val me = currentUsername ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return messageDao.getConversationFlow(me, partner).map { list -> list.map { it.toDomain() } }
    }

    // لیست مخاطبین چت + جزئیات آخرین پیام و تعداد پیام‌های نخوانده
    fun conversationSummariesFlow(): Flow<List<ConversationSummary>> {
        val me = currentUsername ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return messageDao.getAllForOwnerFlow(me).map { messages ->
            val summaries = mutableMapOf<String, ConversationSummary>()
            for (m in messages) {
                val partner = if (m.sender == me) m.recipient else m.sender
                val existing = summaries[partner]
                
                val isUnread = m.recipient == me && m.status != "READ"
                
                if (existing == null || (m.timestamp != null && (existing.timestamp == null || m.timestamp > existing.timestamp))) {
                    summaries[partner] = ConversationSummary(
                        partner = partner,
                        lastMessage = m.content,
                        timestamp = m.timestamp,
                        unreadCount = (existing?.unreadCount ?: 0) + (if (isUnread) 1 else 0)
                    )
                } else if (isUnread) {
                    summaries[partner] = existing.copy(unreadCount = existing.unreadCount + 1)
                }
            }
            summaries.values.sortedByDescending { it.timestamp ?: "" }
        }
    }

data class ConversationSummary(
    val partner: String,
    val lastMessage: String,
    val timestamp: String?,
    val unreadCount: Int
)

    suspend fun sendMessage(recipient: String, content: String, messageType: MessageType, fileUrl: String? = null) {
        val me = currentUsername ?: return
        val id = generateId()
        val nowIso = Instant.now().toString()

        // ذخیره محلی همچنان با timestamp خودمان — فقط برای نمایش فوری در UI
        val entity = PrivateMessageEntity(
            id = id,
            ownerUsername = me,
            sender = me,
            recipient = recipient,
            content = content,
            messageType = messageType.name,
            fileUrl = fileUrl,
            timestamp = nowIso,
            status = "SENT"
        )
        messageDao.upsert(entity)

        // timestamp را در پیام ارسالی به سرور نمی‌فرستیم — بک‌اند خودش با Instant.now() تنظیمش می‌کند
        // و فرمت متفاوت Instant.toString() اندروید با الگوی @JsonFormat سمت سرور ناسازگار بود و باعث fail شدن deserialization می‌شد
        val dto = ChatMessageDto(
            id = id,
            sender = me,
            recipient = recipient,
            content = content,
            messageType = messageType.name,
            fileUrl = fileUrl,
            timestamp = null
        )
        stompManager.publish("/app/chat", json.encodeToString(ChatMessageDto.serializer(), dto))
    }

    fun sendTyping(recipient: String, isTyping: Boolean) {
        val dto = TypingEventDto(recipient = recipient, typing = isTyping)
        stompManager.publish("/app/chat/typing", json.encodeToString(TypingEventDto.serializer(), dto))
    }

    suspend fun markAsRead(partner: String) {
        val me = currentUsername ?: return
        messageDao.markConversationAsRead(me, partner)
    }

    suspend fun deleteMessages(messageIds: List<String>) {
        val me = currentUsername ?: return
        messageDao.deleteMessages(messageIds, me)
    }

    suspend fun togglePin(messageId: String, currentPinned: Boolean) {
        val me = currentUsername ?: return
        messageDao.updatePinStatus(messageId, me, !currentPinned)
    }

    suspend fun ensureUsernameLoaded() {
        if (currentUsername == null) {
            currentUsername = sessionManager.usernameFlow.first()
        }
    }
}

private fun ChatMessageDto.toEntity(ownerUsername: String, status: String): PrivateMessageEntity {
    return PrivateMessageEntity(
        id = id,
        ownerUsername = ownerUsername,
        sender = sender ?: "",
        recipient = recipient,
        content = content,
        messageType = messageType,
        fileUrl = fileUrl,
        timestamp = timestamp,
        status = status,
        isPinned = false
    )
}

private fun PrivateMessageEntity.toDomain(): ChatMessage {
    return ChatMessage(
        id = id,
        sender = sender,
        recipient = recipient,
        content = content,
        messageType = runCatching { MessageType.valueOf(messageType) }.getOrDefault(MessageType.TEXT),
        fileUrl = fileUrl,
        timestamp = timestamp,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
        isPinned = isPinned
    )
}