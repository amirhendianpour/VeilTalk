package com.example.veiltalk.feature.group.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.GroupMessage
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
    val myUsername: String = "" // جدید — برای تشخیص پیام‌های خودم
)

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val sessionManager: SessionManager // جدید
) : ViewModel() {

    val groupId: Long = checkNotNull(savedStateHandle["groupId"])

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    val uiState: StateFlow<GroupChatUiState> = combine(
        groupRepository.groupMessagesFlow(groupId),
        groupRepository.myGroups,
        sessionManager.usernameFlow
    ) { messages, groups, username ->
        val info = groups.find { it.id == groupId }
        GroupChatUiState(
            messages = messages,
            groupName = info?.name ?: "گروه",
            groupImageUrl = info?.imageUrl,
            myUsername = username ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupChatUiState())

    init {
        viewModelScope.launch { groupRepository.ensureUsernameLoaded() }
    }

    fun onInputChange(text: String) { _inputText.value = text }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        viewModelScope.launch { groupRepository.sendGroupMessage(groupId, text) }
        _inputText.value = ""
    }
}