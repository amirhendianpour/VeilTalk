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
import com.example.veiltalk.feature.chat.data.dto.*
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
class ChatRepository @Inject constructor(
    private val stompManager: StompManager,
    private val messageDao: MessageDao,
    private val contactDao: com.example.veiltalk.core.database.dao.ContactDao,
    private val sessionManager: SessionManager,
    private val userDirectory: UserDirectoryRepository,
    private val mediaRepository: MediaRepository,
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
        sessionManager.usernameFlow
            .distinctUntilChanged()
            .onEach { currentUsername = it }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/messages")
            .onEach { frame -> handleIncomingMessage(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/receipts")
            .onEach { frame -> handleReceipt(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/typing")
            .onEach { frame -> handleTyping(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/topic/user-status")
            .onEach { frame -> handleUserStatus(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/messages/delete")
            .onEach { frame -> handleRemoteDeletion(frame.body) }
            .launchIn(scope)

        stompManager.framesForDestination("/user/queue/reactions")
            .onEach { frame -> handleReaction(frame.body) }
            .launchIn(scope)
    }

    private suspend fun handleIncomingMessage(rawBody: String) {
        val dto = runCatching { json.decodeFromString<ChatMessageDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        
        val isChatOpen = activeChatPartner == dto.sender
        val status = if (dto.sender == me) "SENT" else if (isChatOpen) "READ" else "DELIVERED"
        
        messageDao.upsert(
            PrivateMessageEntity(
                id = dto.id,
                ownerUsername = me,
                sender = dto.sender ?: "",
                recipient = dto.recipient,
                content = dto.content,
                timestamp = dto.timestamp,
                messageType = dto.messageType,
                fileUrl = dto.fileUrl,
                status = status,
                replyToId = dto.replyToId,
                mediaKey = dto.mediaKey
            )
        )

        if (dto.sender != null && dto.sender != me) {
            val receipt = ReceiptDto(
                messageId = dto.id,
                recipient = dto.sender,
                status = if (isChatOpen) "READ" else "DELIVERED"
            )
            stompManager.publish("/app/chat/receipt", json.encodeToString(receipt))
        }

        if (dto.sender != me && !isChatOpen) {
            showNotificationForMessage(dto)
        }
    }

    private fun showNotificationForMessage(dto: ChatMessageDto) {
        val sender = dto.sender ?: return
        scope.launch {
            userDirectory.ensureLoaded(listOf(sender))
            val lastEntities = messageDao.getLastMessages(currentUsername ?: "", sender, 5).reversed()
            val notificationMessages = lastEntities.map { entity ->
                NotificationHelper.NotificationMessage(
                    senderUsername = entity.sender,
                    senderName = userDirectory.getDisplayName(entity.sender),
                    content = entity.content,
                    timestamp = runCatching { Instant.parse(entity.timestamp).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
                )
            }
            
            val partnerAvatarUrl = userDirectory.getProfilePicture(sender)
            val bitmap = partnerAvatarUrl?.let { loadAvatar(it) }

            withContext(Dispatchers.Main) {
                NotificationHelper.showMessageNotification(
                    context = context,
                    partnerUsername = sender,
                    partnerDisplayName = userDirectory.getDisplayName(sender),
                    messages = notificationMessages,
                    avatarBitmap = bitmap
                )
            }
        }
    }

    private suspend fun loadAvatar(url: String): Bitmap? {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
        return when (val result = loader.execute(request)) {
            is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
            else -> null
        }
    }

    private suspend fun handleReceipt(rawBody: String) {
        val dto = runCatching { json.decodeFromString<ReceiptDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        messageDao.updateStatusIfHigher(dto.messageId ?: return, me, dto.status)
    }

    private fun handleTyping(rawBody: String) {
        val dto = runCatching { json.decodeFromString<TypingEventDto>(rawBody) }.getOrNull() ?: return
        if (dto.typing) _typingUsers.value += (dto.sender ?: return)
        else _typingUsers.value -= (dto.sender ?: return)
    }

    private fun handleUserStatus(rawBody: String) {
        val dto = runCatching { json.decodeFromString<UserStatusDto>(rawBody) }.getOrNull() ?: return
        scope.launch { userDirectory.updateStatus(dto.username, dto.online, dto.lastSeen) }
    }

    private suspend fun handleRemoteDeletion(rawBody: String) {
        val dto = runCatching { json.decodeFromString<MessageDeleteDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        messageDao.deleteMessages(dto.messageIds, me)
    }

    private suspend fun handleReaction(rawBody: String) {
        val dto = runCatching { json.decodeFromString<ReactionDto>(rawBody) }.getOrNull() ?: return
        val me = currentUsername ?: return
        
        val message = messageDao.getMessageById(dto.messageId, me) ?: return
        val currentReactions = if (!message.reactionsJson.isNullOrBlank()) {
            runCatching { json.decodeFromString<Map<String, String>>(message.reactionsJson) }.getOrDefault(emptyMap())
        } else emptyMap()
        
        val newReactions = currentReactions + (dto.sender!! to dto.emoji)
        messageDao.updateReactions(dto.messageId, me, json.encodeToString(newReactions))
    }

    fun conversationFlow(partner: String): Flow<List<ChatMessage>> {
        return sessionManager.usernameFlow.flatMapLatest { me ->
            if (me == null) flowOf(emptyList())
            else messageDao.getConversationFlow(me, partner).map { list ->
                list.map { it.toDomain(json) }
            }
        }
    }

    fun conversationSummariesFlow(): Flow<List<ConversationSummary>> {
        return sessionManager.usernameFlow.flatMapLatest { me ->
            if (me == null) flowOf(emptyList())
            else messageDao.getAllForOwnerFlow(me).map { messages ->
                val summaries = mutableMapOf<String, ConversationSummary>()
                for (m in messages) {
                    val partner = if (m.sender == me) m.recipient else m.sender
                    val existing = summaries[partner]
                    val isUnread = m.sender != me && m.status != "READ"
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
                summaries.values.toList().sortedByDescending { it.timestamp }
            }
        }
    }

    data class ConversationSummary(val partner: String, val lastMessage: String, val timestamp: String?, val unreadCount: Int)

    suspend fun sendMessage(recipient: String, content: String, messageType: MessageType = MessageType.TEXT, fileUrl: String? = null, replyToId: String? = null, mediaKey: String? = null) {
        val me = currentUsername ?: return
        val id = generateId()
        val nowIso = Instant.now().toString()
        
        messageDao.upsert(
            PrivateMessageEntity(
                id = id,
                ownerUsername = me,
                sender = me,
                recipient = recipient,
                content = content,
                timestamp = nowIso,
                messageType = messageType.name,
                fileUrl = fileUrl,
                status = "SENT",
                replyToId = replyToId,
                mediaKey = mediaKey
            )
        )

        val dto = ChatMessageDto(id, me, recipient, content, messageType.name, fileUrl, nowIso, replyToId, mediaKey)
        stompManager.publish("/app/chat", json.encodeToString(dto))
    }

    suspend fun editMessage(messageId: String, recipient: String, newContent: String) {
        val me = currentUsername ?: return
        val dto = ChatMessageDto(
            id = messageId, 
            recipient = recipient, 
            content = newContent, 
            messageType = MessageType.TEXT.name,
            timestamp = Instant.now().toString()
        )
        stompManager.publish("/app/chat/edit", json.encodeToString(dto))
        messageDao.updateMessageContent(messageId, me, newContent)
    }

    suspend fun sendTyping(recipient: String, typing: Boolean) {
        val me = currentUsername ?: return
        val dto = TypingEventDto(sender = me, recipient = recipient, typing = typing)
        stompManager.publish("/app/chat/typing", json.encodeToString(dto))
    }

    suspend fun markAsRead(partner: String) {
        val me = currentUsername ?: return
        val unread = messageDao.getUnreadFromSender(me, partner)
        messageDao.markConversationAsRead(me, partner)
        unread.forEach { entity ->
            val receipt = ReceiptDto(
                messageId = entity.id,
                recipient = entity.sender,
                status = "READ"
            )
            stompManager.publish("/app/chat/receipt", json.encodeToString(receipt))
        }
    }

    suspend fun sendImageMessage(recipient: String, uri: android.net.Uri): Result<Unit> {
        return mediaRepository.uploadFile(uri).map { uploaded ->
            sendMessage(recipient, uploaded.thumbnail ?: "", MessageType.IMAGE, uploaded.fileUrl, mediaKey = uploaded.mediaKey)
        }
    }

    suspend fun sendVoiceMessage(recipient: String, file: java.io.File): Result<Unit> {
        val bytes = withContext(Dispatchers.IO) { file.readBytes() }
        val fileName = file.name
        return mediaRepository.uploadBytes(bytes, "audio/m4a", fileName = fileName).map { uploaded ->
            sendMessage(recipient, fileName, MessageType.VOICE, uploaded.fileUrl, mediaKey = uploaded.mediaKey)
            file.delete()
        }
    }

    suspend fun sendFileMessage(recipient: String, uri: android.net.Uri): Result<Unit> {
        return mediaRepository.uploadFile(uri).map { uploaded ->
            sendMessage(recipient, uploaded.displayName, MessageType.FILE, uploaded.fileUrl, mediaKey = uploaded.mediaKey)
        }
    }

    suspend fun forwardMessages(targetRecipient: String, messages: List<ChatMessage>) {
        messages.forEach { msg ->
            sendMessage(targetRecipient, msg.content, msg.messageType, msg.fileUrl, mediaKey = msg.mediaKey)
        }
    }

    suspend fun deleteMessages(messageIds: List<String>) {
        val me = currentUsername ?: return
        messageDao.deleteMessages(messageIds, me)
    }

    suspend fun deleteMessagesForEveryone(recipient: String, messageIds: List<String>) {
        val me = currentUsername ?: return
        messageDao.deleteMessages(messageIds, me)
        val dto = MessageDeleteDto(messageIds = messageIds, recipient = recipient)
        stompManager.publish("/app/chat/delete", json.encodeToString(dto))
    }

    suspend fun deleteConversation(partner: String) {
        val me = currentUsername ?: return
        messageDao.deleteConversation(me, partner)
    }

    suspend fun togglePin(messageId: String, currentPinned: Boolean) {
        val me = currentUsername ?: return
        messageDao.updatePinStatus(messageId, me, !currentPinned)
    }

    suspend fun sendReaction(partner: String, messageId: String, emoji: String) {
        val me = currentUsername ?: return
        val dto = ReactionDto(messageId = messageId, emoji = emoji, recipient = partner)
        stompManager.publish("/app/chat/reaction", json.encodeToString(dto))
        
        val message = messageDao.getMessageById(messageId, me) ?: return
        val currentReactions = if (!message.reactionsJson.isNullOrBlank()) {
            runCatching { json.decodeFromString<Map<String, String>>(message.reactionsJson) }.getOrDefault(emptyMap())
        } else emptyMap()
        
        val newReactions = currentReactions + (me to emoji)
        messageDao.updateReactions(messageId, me, json.encodeToString(newReactions))
    }

    suspend fun saveAsContact(username: String, firstName: String, lastName: String) {
        val me = currentUsername ?: return
        contactDao.upsert(com.example.veiltalk.core.database.entity.ContactEntity(username, me, firstName, lastName))
    }

    suspend fun ensureUsernameLoaded() {
        if (currentUsername == null) currentUsername = sessionManager.usernameFlow.first()
    }
}

private fun PrivateMessageEntity.toDomain(json: Json): ChatMessage {
    val reactionsMap: Map<String, String> = if (!reactionsJson.isNullOrBlank()) {
        runCatching { json.decodeFromString<Map<String, String>>(reactionsJson!!) }.getOrDefault(emptyMap())
    } else emptyMap()

    return ChatMessage(
        id = id,
        sender = sender,
        recipient = recipient,
        content = content,
        messageType = runCatching { MessageType.valueOf(messageType) }.getOrDefault(MessageType.TEXT),
        fileUrl = fileUrl,
        timestamp = timestamp,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT),
        isPinned = isPinned,
        replyToId = replyToId,
        mediaKey = mediaKey,
        reactions = reactionsMap
    )
}
