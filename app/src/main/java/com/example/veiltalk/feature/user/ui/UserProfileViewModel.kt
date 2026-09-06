package com.example.veiltalk.feature.user.ui

import androidx.lifecycle.SavedStateHandle
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

data class UserProfileUiState(
    val userInfo: UserInfoDto? = null,
    val isBlocked: Boolean = false,
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserDirectoryRepository,
    private val blockRepository: BlockRepository,
    private val mediaRepository: com.example.veiltalk.feature.chat.data.MediaRepository
) : ViewModel() {

    private val username: String = checkNotNull(savedStateHandle["username"])

    private val _isLoading = MutableStateFlow(true)
    private val _isActionLoading = MutableStateFlow(false)
    private val _isBlocked = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    val uiState: StateFlow<UserProfileUiState> = combine(
        repository.directory,
        _isBlocked,
        _isLoading,
        _isActionLoading,
        _error
    ) { directory, isBlocked, isLoading, actionLoading, error ->
        UserProfileUiState(
            userInfo = directory[username],
            isBlocked = isBlocked,
            isLoading = isLoading,
            isActionLoading = actionLoading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileUiState())

    init {
        loadUserProfile()
        checkBlockStatus()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.ensureLoaded(listOf(username))

            repository.lookupUser(username)
                .onFailure { e ->
                    if (uiState.value.userInfo == null) {
                        _error.value = e.message
                    }
                }
            
            _isLoading.value = false
        }
    }

    private fun checkBlockStatus() {
        viewModelScope.launch {
            when (val result = blockRepository.isBlocked(username)) {
                is ApiResult.Success -> {
                    _isBlocked.value = result.data["isBlocked"] ?: false
                }
                else -> {}
            }
        }
    }

    fun toggleBlock() {
        viewModelScope.launch {
            _isActionLoading.value = true
            val currentStatus = _isBlocked.value
            val result = if (currentStatus) {
                blockRepository.unblockUser(username)
            } else {
                blockRepository.blockUser(username)
            }

            when (result) {
                is ApiResult.Success -> {
                    _isBlocked.value = !currentStatus
                    _uiEvent.emit(if (currentStatus) "کاربر از لیست بلاک خارج شد" else "کاربر بلاک شد")
                }
                is ApiResult.Error -> {
                    _uiEvent.emit("خطا: ${result.message}")
                }
            }
            _isActionLoading.value = false
        }
    }

    fun saveProfilePicture() {
        val url = uiState.value.userInfo?.profilePictureUrl ?: return
        val username = uiState.value.userInfo?.username ?: "user"
        viewModelScope.launch {
            mediaRepository.saveToPublicStorage(url, null, "Avatar_$username.jpg")
                .onSuccess { _uiEvent.emit("عکس پروفایل ذخیره شد") }
                .onFailure { e -> _uiEvent.emit("خطا: ${e.message}") }
        }
    }
}
