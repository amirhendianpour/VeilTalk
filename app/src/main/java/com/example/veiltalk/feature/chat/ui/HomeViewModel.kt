package com.example.veiltalk.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.GroupInfo
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.chat.data.ChatRepository
import com.example.veiltalk.feature.group.data.GroupRepository
import com.example.veiltalk.feature.notification.data.FcmTokenRepository
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab { ALL, CHATS, GROUPS }

sealed class HomeListItem {
    abstract val key: String
    abstract val time: String
    abstract val lastMessage: String
    abstract val unreadCount: Int

    data class ChatItem(
        val username: String,
        val displayName: String,
        val profilePictureUrl: String?,
        override val time: String,
        override val lastMessage: String,
        override val unreadCount: Int
    ) : HomeListItem() {
        override val key = "chat-$username"
    }

    data class GroupItem(
        val group: GroupInfo,
        override val time: String,
        override val lastMessage: String,
        override val unreadCount: Int
    ) : HomeListItem() {
        override val key = "group-${group.id}"
    }
}

data class HomeUiState(
    val myUsername: String = "",
    val myDisplayName: String = "",
    val myProfilePictureUrl: String? = null,
    val allItems: List<HomeListItem> = emptyList(),
    val chatItems: List<HomeListItem.ChatItem> = emptyList(),
    val groups: List<GroupInfo> = emptyList(),
    val lookupError: String? = null,
    val isLookingUp: Boolean = false,
    val isDarkMode: Boolean? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val userDirectory: UserDirectoryRepository,
    private val sessionManager: SessionManager,
    private val fcmTokenRepository: FcmTokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.ensureUsernameLoaded()
            groupRepository.ensureUsernameLoaded()
            groupRepository.refreshMyGroups()
        }

        viewModelScope.launch {
            sessionManager.darkModeFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isDarkMode = enabled)
            }
        }

        viewModelScope.launch {
            combine(
                sessionManager.usernameFlow,
                sessionManager.displayNameFlow,
                userDirectory.directory
            ) { username, displayName, directory ->
                val myUsername = username ?: ""
                val info = directory[myUsername]
                if (info == null && myUsername.isNotBlank()) {
                    userDirectory.ensureLoaded(listOf(myUsername))
                }
                
                Triple(myUsername, displayName ?: "", info?.profilePictureUrl)
            }.collect { (username, displayName, photo) ->
                _uiState.value = _uiState.value.copy(
                    myUsername = username,
                    myDisplayName = displayName,
                    myProfilePictureUrl = photo
                )
            }
        }

        viewModelScope.launch {
            sessionManager.darkModeFlow.collect { enabled ->
                _uiState.value = _uiState.value.copy(isDarkMode = enabled)
            }
        }

        viewModelScope.launch {
            combine(
                chatRepository.conversationSummariesFlow(),
                groupRepository.myGroups,
                groupRepository.groupConversationSummariesFlow(),
                userDirectory.directory
            ) { summaries, groups, groupSummaries, directory ->
                userDirectory.ensureLoaded(summaries.map { it.partner })

                val chatItems = summaries.map { summary ->
                    val info = directory[summary.partner]
                    HomeListItem.ChatItem(
                        username = summary.partner,
                        displayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { summary.partner } else summary.partner,
                        profilePictureUrl = info?.profilePictureUrl,
                        time = summary.timestamp ?: "",
                        lastMessage = summary.lastMessage,
                        unreadCount = summary.unreadCount
                    )
                }

                val groupItems = groups.map { group ->
                    val summary = groupSummaries[group.id]
                    HomeListItem.GroupItem(
                        group = group,
                        time = summary?.timestamp ?: "",
                        lastMessage = summary?.lastMessage ?: "",
                        unreadCount = summary?.unreadCount ?: 0
                    )
                }

                val combined = (chatItems + groupItems).sortedByDescending { it.time }

                Triple(combined, chatItems, groups)
            }.collect { (combined, chatItems, groups) ->
                _uiState.value = _uiState.value.copy(
                    allItems = combined,
                    chatItems = chatItems,
                    groups = groups
                )
            }
        }
    }

    fun startNewChat(identifier: String, onSuccess: (username: String) -> Unit) {
        if (identifier.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLookingUp = true, lookupError = null)
            userDirectory.lookupUser(identifier.trim())
                .onSuccess { info ->
                    _uiState.value = _uiState.value.copy(isLookingUp = false)
                    onSuccess(info.username)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLookingUp = false, lookupError = e.message)
                }
        }
    }

    fun createGroup(name: String, onSuccess: (groupId: Long) -> Unit) {
        viewModelScope.launch {
            groupRepository.createGroup(name).onSuccess { onSuccess(it.id) }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDarkMode(enabled)
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            fcmTokenRepository.unregisterCurrentDeviceToken()
            sessionManager.clearSession()
            onDone()
        }
    }
}