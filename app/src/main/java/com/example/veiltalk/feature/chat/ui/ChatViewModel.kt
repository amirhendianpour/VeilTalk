package com.example.veiltalk.feature.chat.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.ChatMessage
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.feature.chat.data.ChatRepository
import com.example.veiltalk.feature.group.data.GroupRepository
import com.example.veiltalk.feature.chat.data.MediaRepository
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val partnerDisplayName: String = "",
    val partnerProfilePicture: String? = null,
    val isPartnerTyping: Boolean = false,
    val isPartnerOnline: Boolean = false,
    val partnerLastSeen: String? = null,
    val editingMessage: ChatMessage? = null,
    val replyingMessage: ChatMessage? = null,
    val searchQuery: String = "",
    val allDestinations: List<com.example.veiltalk.feature.chat.ui.HomeListItem> = emptyList(),
    val isRecording: Boolean = false,
    val pinnedMessages: List<ChatMessage> = emptyList()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository, // اضافه شد
    private val userDirectory: UserDirectoryRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val partner: String = checkNotNull(savedStateHandle["username"])

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    private val _replyingMessage = MutableStateFlow<ChatMessage?>(null)
    private val _searchQuery = MutableStateFlow("")

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.conversationFlow(partner),
        userDirectory.directory,
        chatRepository.typingUsers,
        userDirectory.onlineStatus,
        userDirectory.lastSeen,
        _editingMessage,
        _searchQuery,
        chatRepository.conversationSummariesFlow(), // جدید
        groupRepository.myGroups, // جدید
        _replyingMessage
    ) { args ->
        val messages = args[0] as List<ChatMessage>
        val directory = args[1] as Map<String, com.example.veiltalk.feature.user.data.dto.UserInfoDto>
        val typing = args[2] as Set<String>
        val onlineStatus = args[3] as Map<String, Boolean>
        val lastSeen = args[4] as Map<String, String?>
        val editingMessage = args[5] as ChatMessage?
        val query = args[6] as String
        val summaries = args[7] as List<com.example.veiltalk.feature.chat.data.ChatRepository.ConversationSummary>
        val groups = args[8] as List<com.example.veiltalk.common.model.GroupInfo>
        val replyingMessage = args[9] as ChatMessage?

        val filteredMessages = if (query.isBlank()) messages else {
            messages.filter { it.content.contains(query, ignoreCase = true) }
        }

        // ساخت لیست مقصدهای فوروارد
        val destinations = summaries.map { s ->
            val info = directory[s.partner]
            com.example.veiltalk.feature.chat.ui.HomeListItem.ChatItem(
                username = s.partner,
                displayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { s.partner } else s.partner,
                profilePictureUrl = info?.profilePictureUrl,
                time = s.timestamp ?: "",
                lastMessage = s.lastMessage,
                unreadCount = s.unreadCount
            )
        } + groups.map { g ->
            com.example.veiltalk.feature.chat.ui.HomeListItem.GroupItem(group = g, time = "", lastMessage = "", unreadCount = 0)
        }

        val info = directory[partner]
        ChatUiState(
            messages = filteredMessages,
            partnerDisplayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { partner } else partner,
            partnerProfilePicture = info?.profilePictureUrl,
            isPartnerTyping = typing.contains(partner),
            isPartnerOnline = onlineStatus[partner] ?: false,
            partnerLastSeen = lastSeen[partner],
            editingMessage = editingMessage,
            replyingMessage = replyingMessage,
            searchQuery = query,
            allDestinations = destinations,
            isRecording = _isRecording.value,
            pinnedMessages = messages.filter { it.isPinned }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    private var typingResetJob: kotlinx.coroutines.Job? = null

    init {
        chatRepository.setActiveChatPartner(partner)
        viewModelScope.launch {
            chatRepository.ensureUsernameLoaded()
            userDirectory.ensureLoaded(listOf(partner))
            chatRepository.markAsRead(partner)
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
        viewModelScope.launch {
            chatRepository.sendTyping(partner, true)
        }
        typingResetJob?.cancel()
        typingResetJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            chatRepository.sendTyping(partner, false)
        }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        typingResetJob?.cancel()
        viewModelScope.launch {
            chatRepository.sendTyping(partner, false)
        }
        
        viewModelScope.launch {
            val editingMsg = _editingMessage.value
            val replyingMsg = _replyingMessage.value
            
            if (editingMsg != null) {
                chatRepository.editMessage(editingMsg.id, partner, text)
                _editingMessage.value = null
            } else {
                chatRepository.sendMessage(partner, text, MessageType.TEXT, replyToId = replyingMsg?.id)
                _replyingMessage.value = null
            }
        }
        _inputText.value = ""
    }

    fun startReplying(message: ChatMessage) {
        _replyingMessage.value = message
        _editingMessage.value = null
    }

    fun cancelReplying() {
        _replyingMessage.value = null
    }

    fun startEditing(message: ChatMessage) {
        _editingMessage.value = message
        _inputText.value = message.content
    }

    fun cancelEditing() {
        _editingMessage.value = null
        _inputText.value = ""
    }

    fun sendSticker(url: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(partner, "", MessageType.STICKER, url)
        }
    }

    fun sendGif(url: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(partner, "", MessageType.GIF, url)
        }
    }

    fun startRecording() {
        _isRecording.value = true
    }

    fun stopRecording(file: java.io.File?) {
        _isRecording.value = false
        if (file != null && file.exists() && file.length() > 100) {
            viewModelScope.launch {
                _isUploading.value = true
                chatRepository.sendVoiceMessage(partner, file)
                    .onFailure { e -> _uploadError.value = "خطا در ارسال ویس: ${e.message}" }
                _isUploading.value = false
            }
        } else {
            file?.delete()
        }
    }

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            chatRepository.sendImageMessage(partner, uri)
                .onFailure { e -> _uploadError.value = e.message ?: "خطا در آپلود عکس" }
            _isUploading.value = false
        }
    }

    fun sendFile(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            chatRepository.sendFileMessage(partner, uri)
                .onFailure { e -> _uploadError.value = e.message ?: "خطا در آپلود فایل" }
            _isUploading.value = false
        }
    }

    fun clearUploadError() {
        _uploadError.value = null
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteMessages(messageIds: List<String>) {
        viewModelScope.launch {
            chatRepository.deleteMessages(messageIds)
        }
    }

    fun deleteMessagesForEveryone(messageIds: List<String>) {
        viewModelScope.launch {
            chatRepository.deleteMessagesForEveryone(partner, messageIds)
        }
    }

    fun forwardMessages(targetUsername: String, messageIds: List<String>) {
        viewModelScope.launch {
            val msgs = uiState.value.messages.filter { it.id in messageIds }
            chatRepository.forwardMessages(targetUsername, msgs)
        }
    }

    fun forwardMessagesToGroup(targetGroupId: Long, messageIds: List<String>) {
        viewModelScope.launch {
            val msgs = uiState.value.messages.filter { it.id in messageIds }
            groupRepository.forwardMessagesToGroup(targetGroupId, msgs)
        }
    }

    fun togglePin(messageId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            chatRepository.togglePin(messageId, currentPinned)
        }
    }

    fun sendReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            chatRepository.sendReaction(partner, messageId, emoji)
        }
    }

    override fun onCleared() {
        chatRepository.setActiveChatPartner(null)
        viewModelScope.launch {
            chatRepository.sendTyping(partner, false)
        }
        super.onCleared()
    }
}
