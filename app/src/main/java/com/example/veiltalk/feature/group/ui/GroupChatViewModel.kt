package com.example.veiltalk.feature.group.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.GroupMessage
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.group.data.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupChatUiState(
    val messages: List<GroupMessage> = emptyList(),
    val groupName: String = "",
    val groupImageUrl: String? = null,
    val myUsername: String = "",
    val editingMessage: GroupMessage? = null,
    val replyingMessage: GroupMessage? = null,
    val searchQuery: String = "",
    val allDestinations: List<com.example.veiltalk.feature.chat.ui.HomeListItem> = emptyList(),
    val isRecording: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val chatRepository: com.example.veiltalk.feature.chat.data.ChatRepository, // اضافه شد
    private val userDirectory: com.example.veiltalk.feature.user.data.UserDirectoryRepository,
    private val mediaRepository: com.example.veiltalk.feature.chat.data.MediaRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val groupId: Long = checkNotNull(savedStateHandle["groupId"])

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _editingMessage = MutableStateFlow<GroupMessage?>(null)
    private val _replyingMessage = MutableStateFlow<GroupMessage?>(null)
    private val _searchQuery = MutableStateFlow("")

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<GroupChatUiState> = combine(
        groupRepository.groupMessagesFlow(groupId),
        groupRepository.myGroups,
        sessionManager.usernameFlow,
        _editingMessage,
        _searchQuery,
        chatRepository.conversationSummariesFlow(),
        userDirectory.directory,
        _replyingMessage,
        _isUploading,
        _uploadError
    ) { args ->
        val messages = args[0] as List<GroupMessage>
        val groups = args[1] as List<com.example.veiltalk.common.model.GroupInfo>
        val username = args[2] as String?
        val editingMessage = args[3] as GroupMessage?
        val query = args[4] as String
        val summaries = args[5] as List<com.example.veiltalk.feature.chat.data.ChatRepository.ConversationSummary>
        val directory = args[6] as Map<String, com.example.veiltalk.feature.user.data.dto.UserInfoDto>
        val replyingMessage = args[7] as GroupMessage?
        val isUploading = args[8] as Boolean
        val uploadError = args[9] as String?

        val filteredMessages = if (query.isBlank()) messages else {
            messages.filter { it.content.contains(query, ignoreCase = true) }
        }

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

        val info = groups.find { it.id == groupId }
        GroupChatUiState(
            messages = filteredMessages,
            groupName = info?.name ?: "گروه",
            groupImageUrl = info?.imageUrl,
            myUsername = username ?: "",
            editingMessage = editingMessage,
            replyingMessage = replyingMessage,
            searchQuery = query,
            allDestinations = destinations,
            isRecording = _isRecording.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupChatUiState())

    init {
        groupRepository.setActiveGroupId(groupId)
        viewModelScope.launch { 
            groupRepository.ensureUsernameLoaded()
            groupRepository.markGroupAsRead(groupId)
        }
    }

    fun onInputChange(text: String) { _inputText.value = text }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            val editingMsg = _editingMessage.value
            val replyingMsg = _replyingMessage.value
            
            if (editingMsg != null) {
                groupRepository.editGroupMessage(groupId, editingMsg.id, text)
                _editingMessage.value = null
            } else {
                groupRepository.sendGroupMessage(groupId, text, replyToId = replyingMsg?.id)
                _replyingMessage.value = null
            }
        }
        _inputText.value = ""
    }

    fun sendImage(uri: android.net.Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            groupRepository.sendImageMessage(groupId, uri)
                .onFailure { e -> _uploadError.value = e.message ?: "خطا در آپلود عکس" }
            _isUploading.value = false
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
                groupRepository.sendVoiceMessage(groupId, file)
                    .onFailure { e -> _uploadError.value = "خطا در ارسال ویس: ${e.message}" }
                _isUploading.value = false
            }
        } else {
            file?.delete()
        }
    }

    fun sendFile(uri: android.net.Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            groupRepository.sendFileMessage(groupId, uri)
                .onFailure { e -> _uploadError.value = e.message ?: "خطا در آپلود فایل" }
            _isUploading.value = false
        }
    }

    fun clearUploadError() { _uploadError.value = null }

    fun startReplying(message: GroupMessage) {
        _replyingMessage.value = message
        _editingMessage.value = null
    }

    fun cancelReplying() {
        _replyingMessage.value = null
    }

    fun startEditing(message: GroupMessage) {
        _editingMessage.value = message
        _inputText.value = message.content
    }

    fun cancelEditing() {
        _editingMessage.value = null
        _inputText.value = ""
    }

    fun sendSticker(url: String) {
        viewModelScope.launch {
            groupRepository.sendGroupMessage(groupId, "", MessageType.STICKER, url)
        }
    }

    fun sendGif(url: String) {
        viewModelScope.launch {
            groupRepository.sendGroupMessage(groupId, "", MessageType.GIF, url)
        }
    }

    fun deleteMessages(messageIds: List<String>) {
        viewModelScope.launch {
            groupRepository.deleteMessages(messageIds)
        }
    }

    fun deleteMessagesForEveryone(messageIds: List<String>) {
        viewModelScope.launch {
            groupRepository.deleteMessagesForEveryone(groupId, messageIds)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun forwardMessages(targetUsername: String, messageIds: List<String>) {
        viewModelScope.launch {
            val msgs = uiState.value.messages.filter { it.id in messageIds }
            chatRepository.forwardMessages(targetUsername, msgs.map { 
                com.example.veiltalk.common.model.ChatMessage(
                    id = it.id, sender = it.sender ?: "", recipient = targetUsername, content = it.content,
                    messageType = it.messageType, fileUrl = it.fileUrl, timestamp = it.timestamp, status = com.example.veiltalk.common.model.MessageStatus.SENT
                )
            })
        }
    }

    fun forwardMessagesToGroup(targetGroupId: Long, messageIds: List<String>) {
        viewModelScope.launch {
            val msgs = uiState.value.messages.filter { it.id in messageIds }
            groupRepository.forwardGroupMessagesToGroup(targetGroupId, msgs)
        }
    }

    fun togglePin(messageId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            groupRepository.togglePin(messageId, currentPinned)
        }
    }

    override fun onCleared() {
        groupRepository.setActiveGroupId(null)
        super.onCleared()
    }
}
