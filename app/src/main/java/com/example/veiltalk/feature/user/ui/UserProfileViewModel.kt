package com.example.veiltalk.feature.user.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import com.example.veiltalk.feature.user.data.dto.UserInfoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // ابتدا از دیتای کش شده در ریپازیتوری نشان می‌دهیم
            val cachedInfo = repository.directory.value[username]
            if (cachedInfo != null) {
                _uiState.value = _uiState.value.copy(userInfo = cachedInfo, isLoading = false)
            }

            // همیشه درخواست لود مجدد از سرور می‌دهیم تا اطلاعات کامل (ایمیل/شماره) اگر مجاز بودیم دریافت شود
            repository.lookupUser(username)
                .onSuccess { info ->
                    _uiState.value = _uiState.value.copy(userInfo = info, isLoading = false)
                }
                .onFailure { e ->
                    if (_uiState.value.userInfo == null) {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
        }
    }
}
