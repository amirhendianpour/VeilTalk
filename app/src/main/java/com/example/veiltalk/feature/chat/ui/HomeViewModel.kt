package com.example.veiltalk.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.GroupInfo
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.chat.data.ChatRepository
import com.example.veiltalk.feature.group.data.GroupRepository
import com.example.veiltalk.feature.notification.data.FcmTokenRepository
import com.example.veiltalk.feature.user.data.ContactSyncRepository
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val isDarkMode: Boolean? = null,
    val selectedKeys: Set<String> = emptySet(),
    val isSyncingContacts: Boolean = false,
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val userDirectory: UserDirectoryRepository,
    private val contactSyncRepository: ContactSyncRepository,
    private val contactDao: com.example.veiltalk.core.database.dao.ContactDao,
    private val sessionManager: SessionManager,
    private val fcmTokenRepository: FcmTokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _contacts = MutableStateFlow<List<HomeListItem.ChatItem>>(emptyList())
    val contacts: StateFlow<List<HomeListItem.ChatItem>> = _contacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            chatRepository.ensureUsernameLoaded()
            groupRepository.ensureUsernameLoaded()
            groupRepository.refreshMyGroups()
        }

        viewModelScope.launch {
            sessionManager.usernameFlow.flatMapLatest { me ->
                if (me != null) contactDao.getContactsFlow(me) else flowOf(emptyList())
            }.collect { entities ->
                _contacts.value = entities.map { e ->
                    HomeListItem.ChatItem(
                        username = e.username,
                        displayName = "${e.firstName} ${e.lastName}".trim().ifBlank { e.username },
                        profilePictureUrl = e.profilePictureUrl,
                        time = "", 
                        lastMessage = "", 
                        unreadCount = 0
                    )
                }
            }
        }

        viewModelScope.launch {
            @Suppress("UNCHECKED_CAST")
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
            @Suppress("UNCHECKED_CAST")
            combine(
                chatRepository.conversationSummariesFlow(),
                groupRepository.myGroups,
                groupRepository.groupConversationSummariesFlow(),
                userDirectory.directory,
                _searchQuery,
                sessionManager.darkModeFlow,
                sessionManager.usernameFlow
            ) { args ->
                val summaries = args[0] as List<com.example.veiltalk.feature.chat.data.ChatRepository.ConversationSummary>
                val groupsData = args[1] as List<GroupInfo>
                val groupSummaries = args[2] as Map<Long, com.example.veiltalk.feature.group.data.GroupRepository.GroupSummary>
                val directory = args[3] as Map<String, com.example.veiltalk.feature.user.data.dto.UserInfoDto>
                val query = args[4] as String
                val isDark = args[5] as Boolean?
                val myUsername = args[6] as String?

                userDirectory.ensureLoaded(summaries.map { it.partner })

                val chatItems = summaries.map { summary ->
                    val info = directory[summary.partner]
                    val isSavedMessages = summary.partner == myUsername
                    HomeListItem.ChatItem(
                        username = summary.partner,
                        displayName = if (isSavedMessages) "پیام‌های ذخیره شده" 
                                     else if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { summary.partner } 
                                     else summary.partner,
                        profilePictureUrl = if (isSavedMessages) "special://saved_messages" else info?.profilePictureUrl,
                        time = summary.timestamp ?: "",
                        lastMessage = summary.lastMessage,
                        unreadCount = summary.unreadCount
                    )
                }

                val groupItems = groupsData.map { group ->
                    val summary = groupSummaries[group.id]
                    HomeListItem.GroupItem(
                        group = group,
                        time = summary?.timestamp ?: "",
                        lastMessage = summary?.lastMessage ?: "",
                        unreadCount = summary?.unreadCount ?: 0
                    )
                }

                val filteredChatItems = if (query.isBlank()) chatItems else {
                    chatItems.filter { it.displayName.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true) }
                }
                val filteredGroups = if (query.isBlank()) groupsData else {
                    groupsData.filter { it.name.contains(query, ignoreCase = true) }
                }
                val filteredGroupItems = if (query.isBlank()) groupItems else {
                    groupItems.filter { it.group.name.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true) }
                }

                val combined = (filteredChatItems + filteredGroupItems).sortedByDescending { it.time }

                HomeUiState(
                    allItems = combined,
                    chatItems = filteredChatItems,
                    groups = filteredGroups,
                    searchQuery = query,
                    isDarkMode = isDark
                )
            }.collect { state ->
                _uiState.value = _uiState.value.copy(
                    allItems = state.allItems,
                    chatItems = state.chatItems,
                    groups = state.groups,
                    searchQuery = state.searchQuery,
                    isDarkMode = state.isDarkMode
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun syncContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncingContacts = true)
            contactSyncRepository.syncContacts()
            _uiState.value = _uiState.value.copy(isSyncingContacts = false)
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

    fun toggleSelection(key: String) {
        val current = _uiState.value.selectedKeys
        _uiState.value = _uiState.value.copy(
            selectedKeys = if (current.contains(key)) current - key else current + key
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedKeys = emptySet())
    }

    fun deleteSelectedChats() {
        val keys = _uiState.value.selectedKeys
        viewModelScope.launch {
            keys.forEach { key ->
                if (key.startsWith("chat-")) {
                    val username = key.removePrefix("chat-")
                    chatRepository.deleteConversation(username)
                } else if (key.startsWith("group-")) {
                    val groupId = key.removePrefix("group-").toLongOrNull()
                    if (groupId != null) {
                        groupRepository.deleteConversation(groupId)
                    }
                }
            }
            clearSelection()
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
