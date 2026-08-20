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
    val editingMessage: GroupMessage? = null
)

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val groupId: Long = checkNotNull(savedStateHandle["groupId"])

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _editingMessage = MutableStateFlow<GroupMessage?>(null)

    val uiState: StateFlow<GroupChatUiState> = combine(
        groupRepository.groupMessagesFlow(groupId),
        groupRepository.myGroups,
        sessionManager.usernameFlow,
        _editingMessage
    ) { messages, groups, username, editingMessage ->
        val info = groups.find { it.id == groupId }
        GroupChatUiState(
            messages = messages,
            groupName = info?.name ?: "گروه",
            groupImageUrl = info?.imageUrl,
            myUsername = username ?: "",
            editingMessage = editingMessage
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
            if (editingMsg != null) {
                groupRepository.editGroupMessage(groupId, editingMsg.id, text)
                _editingMessage.value = null
            } else {
                groupRepository.sendGroupMessage(groupId, text)
            }
        }
        _inputText.value = ""
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
