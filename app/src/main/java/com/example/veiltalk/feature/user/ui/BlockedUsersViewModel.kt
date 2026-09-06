package com.example.veiltalk.feature.user.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.util.ApiResult
import com.example.veiltalk.feature.user.data.BlockRepository
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import com.example.veiltalk.feature.user.data.dto.UserInfoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
    val blockedUsers: List<UserInfoDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val blockRepository: BlockRepository,
    private val userDirectory: UserDirectoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _blockedUsernames = MutableStateFlow<List<String>>(emptyList())
    
    val uiState: StateFlow<BlockedUsersUiState> = combine(
        _blockedUsernames,
        userDirectory.directory,
        _isLoading,
        _error
    ) { usernames, directory, loading, error ->
        BlockedUsersUiState(
            blockedUsers = usernames.map { username ->
                directory[username] ?: UserInfoDto(username, username, "", null, null, null)
            },
            isLoading = loading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlockedUsersUiState())

    init {
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            when (val result = blockRepository.getBlockedUsers()) {
                is ApiResult.Success -> {
                    _blockedUsernames.value = result.data
                    userDirectory.ensureLoaded(result.data)
                }
                is ApiResult.Error -> {
                    _error.value = result.message
                }
            }
            _isLoading.value = false
        }
    }

    fun unblockUser(username: String) {
        viewModelScope.launch {
            when (val result = blockRepository.unblockUser(username)) {
                is ApiResult.Success -> {
                    _blockedUsernames.value = _blockedUsernames.value - username
                }
                is ApiResult.Error -> {
                    // Could emit an error event if needed
                }
            }
        }
    }
}
