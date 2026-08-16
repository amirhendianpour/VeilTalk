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
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val partnerDisplayName: String = "",
    val partnerProfilePicture: String? = null,
    val isPartnerTyping: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val userDirectory: UserDirectoryRepository,
    private val mediaRepository: MediaRepository // جدید
) : ViewModel() {

    val partner: String = checkNotNull(savedStateHandle["username"])

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.conversationFlow(partner),
        userDirectory.directory,
        chatRepository.typingUsers
    ) { messages, directory, typing ->
        val info = directory[partner]
        ChatUiState(
            messages = messages,
            partnerDisplayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { partner } else partner,
            partnerProfilePicture = info?.profilePictureUrl,
            isPartnerTyping = typing.contains(partner)
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
            chatRepository.sendMessage(partner, text, MessageType.TEXT)
        }
        _inputText.value = ""
    }

    // ارسال عکس — معادل بخش accept="image/*" در MessageInput.tsx
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

    // ارسال فایل عمومی — قابلیت جدید که در نسخه وب هنوز پیاده نشده
    fun sendFile(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            mediaRepository.uploadFile(uri)
                .onSuccess { uploaded ->
                    // نام فایل در content ذخیره می‌شود تا در صورت باز شدن پیام در وب هم حداقل نام فایل خوانا باشد
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