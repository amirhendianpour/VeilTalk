package com.example.veiltalk.feature.group.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.GroupMemberInfo
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.group.data.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupInfoUiState(
    val groupName: String = "",
    val groupImageUrl: String? = null,
    val members: List<GroupMemberInfo> = emptyList(),
    val isLoadingMembers: Boolean = true,
    val isAdmin: Boolean = false,
    val error: String? = null,
    val busyUsername: String? = null,
    val isDeleted: Boolean = false
)

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val groupId: Long = checkNotNull(savedStateHandle["groupId"])

    private val _uiState = MutableStateFlow(GroupInfoUiState())
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private var myUsername: String? = null

    init {
        viewModelScope.launch {
            myUsername = sessionManager.usernameFlow.first()
            observeGroupBaseInfo()
            loadMembers()
        }
        viewModelScope.launch {
            groupRepository.groupUpdateEvent.collect { event ->
                if (event != null && event.groupId == groupId) {
                    when (event.type) {
                        "ADDED", "MEMBER_REMOVED", "ROLE_UPDATED" -> loadMembers()
                        "DELETED", "REMOVED" -> _uiState.value = _uiState.value.copy(isDeleted = true)
                        "NAME_UPDATED", "IMAGE_UPDATED" -> observeGroupBaseInfoOnce()
                    }
                }
            }
        }
    }

    private fun observeGroupBaseInfo() {
        viewModelScope.launch {
            groupRepository.myGroups.collect { groups ->
                val info = groups.find { it.id == groupId } ?: return@collect
                _uiState.value = _uiState.value.copy(
                    groupName = info.name,
                    groupImageUrl = info.imageUrl,
                    isAdmin = info.role == "ADMIN"
                )
            }
        }
    }

    private fun observeGroupBaseInfoOnce() {
        val info = groupRepository.myGroups.value.find { it.id == groupId } ?: return
        _uiState.value = _uiState.value.copy(groupName = info.name, groupImageUrl = info.imageUrl)
    }

    fun loadMembers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMembers = true)
            val result = groupRepository.getGroupMembersInfo(groupId)
            result.onSuccess { members ->
                _uiState.value = _uiState.value.copy(members = members, isLoadingMembers = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoadingMembers = false, error = e.message)
            }
        }
    }

    fun updateGroupName(newName: String) {
        viewModelScope.launch {
            groupRepository.updateGroupName(groupId, newName)
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            groupRepository.uploadGroupImage(groupId, uri)
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
        }
    }

    fun addMember(identifier: String, lookup: suspend (String) -> Result<String>) {
        viewModelScope.launch {
            val usernameResult = lookup(identifier)
            usernameResult.onSuccess { username ->
                groupRepository.addMember(groupId, username)
                    .onSuccess { loadMembers() }
                    .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun toggleAdmin(member: GroupMemberInfo) {
        val newRole = if (member.role == "ADMIN") "MEMBER" else "ADMIN"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyUsername = member.username)
            groupRepository.updateMemberRole(groupId, member.username, newRole)
                .onSuccess { loadMembers() }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            _uiState.value = _uiState.value.copy(busyUsername = null)
        }
    }

    fun removeMember(member: GroupMemberInfo) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyUsername = member.username)
            groupRepository.removeMember(groupId, member.username)
                .onSuccess { loadMembers() }
                .onFailure { e -> _uiState.value = _uiState.value.copy(error = e.message) }
            _uiState.value = _uiState.value.copy(busyUsername = null)
        }
    }

    fun deleteGroup(onDone: () -> Unit) {
        viewModelScope.launch {
            groupRepository.deleteGroup(groupId).onSuccess { onDone() }
        }
    }

    fun isMe(username: String): Boolean = username == myUsername
}