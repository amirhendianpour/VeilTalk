package com.example.veiltalk.feature.chat.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.ChatMessage
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.feature.chat.data.ChatRepository
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
    val editingMessage: ChatMessage? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val userDirectory: UserDirectoryRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val partner: String = checkNotNull(savedStateHandle["username"])

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.conversationFlow(partner),
        userDirectory.directory,
        chatRepository.typingUsers,
        userDirectory.onlineStatus,
        userDirectory.lastSeen,
        _editingMessage
    ) { args ->
        val messages = args[0] as List<ChatMessage>
        val directory = args[1] as Map<String, com.example.veiltalk.feature.user.data.dto.UserInfoDto>
        val typing = args[2] as Set<String>
        val onlineStatus = args[3] as Map<String, Boolean>
        val lastSeen = args[4] as Map<String, String?>
        val editingMessage = args[5] as ChatMessage?

        val info = directory[partner]
        ChatUiState(
            messages = messages,
            partnerDisplayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { partner } else partner,
            partnerProfilePicture = info?.profilePictureUrl,
            isPartnerTyping = typing.contains(partner),
            isPartnerOnline = onlineStatus[partner] ?: false,
            partnerLastSeen = lastSeen[partner],
            editingMessage = editingMessage
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
        chatRepository.sendTyping(partner, true)
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
        chatRepository.sendTyping(partner, false)
        
        viewModelScope.launch {
            val editingMsg = _editingMessage.value
            if (editingMsg != null) {
                chatRepository.editMessage(editingMsg.id, partner, text)
                _editingMessage.value = null
            } else {
                chatRepository.sendMessage(partner, text, MessageType.TEXT)
            }
        }
        _inputText.value = ""
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

    fun sendImage(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            mediaRepository.uploadFile(uri)
                .onSuccess { uploaded ->
                    chatRepository.sendMessage(partner, "", MessageType.IMAGE, uploaded.fileUrl)
                }
                .onFailure { e -> _uploadError.value = e.message ?: "خطا در آپلود عکس" }
            _isUploading.value = false
        }
    }

    fun sendFile(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            mediaRepository.uploadFile(uri)
                .onSuccess { uploaded ->
                    chatRepository.sendMessage(partner, uploaded.displayName, MessageType.FILE, uploaded.fileUrl)
                }
                .onFailure { e -> _uploadError.value = e.message ?: "خطا در آپلود فایل" }
            _isUploading.value = false
        }
    }

    fun clearUploadError() {
        _uploadError.value = null
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

    fun togglePin(messageId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            chatRepository.togglePin(messageId, currentPinned)
        }
    }

    override fun onCleared() {
        chatRepository.setActiveChatPartner(null)
        chatRepository.sendTyping(partner, false)
        super.onCleared()
    }
}
