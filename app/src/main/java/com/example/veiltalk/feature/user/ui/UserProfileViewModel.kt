package com.example.veiltalk.feature.user.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import com.example.veiltalk.feature.user.data.dto.UserInfoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val userInfo: UserInfoDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: UserDirectoryRepository
) : ViewModel() {

    private val username: String = checkNotNull(savedStateHandle["username"])

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UserProfileUiState> = combine(
        repository.directory,
        _isLoading,
        _error
    ) { directory, isLoading, error ->
        UserProfileUiState(
            userInfo = directory[username],
            isLoading = isLoading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileUiState())

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // ۱. تلاش برای لود از دیتابیس محلی (از طریق ensureLoaded)
            repository.ensureLoaded(listOf(username))

            // ۲. درخواست آپدیت از سرور
            repository.lookupUser(username)
                .onFailure { e ->
                    // فقط اگر کلا دیتایی نداشتیم خطا نشان بده
                    if (uiState.value.userInfo == null) {
                        _error.value = e.message
                    }
                }
            
            _isLoading.value = false
        }
    }
}
