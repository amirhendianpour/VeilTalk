package com.example.veiltalk.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.chat.data.ChatRepository
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListItem(val username: String, val displayName: String, val profilePictureUrl: String?)

data class ChatListUiState(
    val items: List<ChatListItem> = emptyList(),
    val myDisplayName: String = "",
    val lookupError: String? = null,
    val isLookingUp: Boolean = false
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userDirectory: UserDirectoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.ensureUsernameLoaded()
            sessionManager.displayNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(myDisplayName = name ?: "")
            }
        }

        viewModelScope.launch {
            combine(
                chatRepository.chatPartnersFlow(),
                userDirectory.directory
            ) { partners, directory ->
                val usernames = partners.map { it.first }
                userDirectory.ensureLoaded(usernames)
                usernames.map { username ->
                    val info = directory[username]
                    ChatListItem(
                        username = username,
                        displayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { username } else username,
                        profilePictureUrl = info?.profilePictureUrl
                    )
                }
            }.collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
    }

    fun startNewChat(identifier: String, onSuccess: (username: String) -> Unit) {
        if (identifier.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLookingUp = true, lookupError = null)
            val result = userDirectory.lookupUser(identifier.trim())
            result.onSuccess { info ->
                _uiState.value = _uiState.value.copy(isLookingUp = false)
                onSuccess(info.username)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLookingUp = false, lookupError = e.message)
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionManager.clearSession()
            onDone()
        }
    }
}