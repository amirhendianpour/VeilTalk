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

    data class ChatItem(val username: String, val displayName: String, val profilePictureUrl: String?, override val time: String) : HomeListItem() {
        override val key = "chat-$username"
    }
    data class GroupItem(val group: GroupInfo, override val time: String) : HomeListItem() {
        override val key = "group-${group.id}"
    }
}

data class HomeUiState(
    val myDisplayName: String = "",
    val allItems: List<HomeListItem> = emptyList(),
    val chatItems: List<HomeListItem.ChatItem> = emptyList(),
    val groups: List<GroupInfo> = emptyList(),
    val lookupError: String? = null,
    val isLookingUp: Boolean = false
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
            sessionManager.displayNameFlow.collect { name ->
                _uiState.value = _uiState.value.copy(myDisplayName = name ?: "")
            }
        }

        viewModelScope.launch {
            combine(
                chatRepository.chatPartnersFlow(),
                groupRepository.myGroups,
                groupRepository.latestGroupMessageTimesFlow(),
                userDirectory.directory
            ) { partners, groups, groupTimes, directory ->
                userDirectory.ensureLoaded(partners.map { it.first })

                val chatItems = partners.map { (username, time) ->
                    val info = directory[username]
                    HomeListItem.ChatItem(
                        username = username,
                        displayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { username } else username,
                        profilePictureUrl = info?.profilePictureUrl,
                        time = time ?: ""
                    )
                }

                val groupItems = groups.map { group ->
                    HomeListItem.GroupItem(group = group, time = groupTimes[group.id] ?: "")
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

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            fcmTokenRepository.unregisterCurrentDeviceToken()
            sessionManager.clearSession()
            onDone()
        }
    }
}