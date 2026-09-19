package com.example.veiltalk.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.feature.notification.data.ActiveSessionDto
import com.example.veiltalk.feature.notification.data.FcmTokenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveSessionsUiState(
    val sessions: List<ActiveSessionDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ActiveSessionsViewModel @Inject constructor(
    private val tokenRepository: FcmTokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveSessionsUiState())
    val uiState: StateFlow<ActiveSessionsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        refreshAndLoadSessions()
    }

    fun refreshAndLoadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // ابتدا توکن دستگاه فعلی را ثبت/به‌روزرسانی می‌کنیم تا مشخصات واقعی آن در لیست بیاید
            tokenRepository.registerCurrentDeviceToken()
            
            // سپس لیست سشن‌ها را لود می‌کنیم
            tokenRepository.getActiveSessions()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(sessions = list, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun loadSessions() {
        viewModelScope.launch {
            tokenRepository.getActiveSessions()
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(sessions = list)
                }
        }
    }

    fun terminateSession(id: Long) {
        viewModelScope.launch {
            tokenRepository.terminateSession(id)
                .onSuccess {
                    _uiEvent.emit("نشست با موفقیت خاتمه یافت.")
                    loadSessions()
                }
                .onFailure { e ->
                    _uiEvent.emit(e.message ?: "خطا در خاتمه دادن به نشست")
                }
        }
    }

    fun terminateOtherSessions() {
        viewModelScope.launch {
            tokenRepository.terminateOtherSessions()
                .onSuccess {
                    _uiEvent.emit("سایر نشست‌ها با موفقیت خاتمه یافتند.")
                    loadSessions()
                }
                .onFailure { e ->
                    _uiEvent.emit(e.message ?: "خطا در خاتمه دادن به سایر نشست‌ها")
                }
        }
    }
}