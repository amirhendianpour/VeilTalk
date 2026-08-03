package com.example.veiltalk.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.util.ApiResult
import com.example.veiltalk.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun loginWithPassword(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "لطفاً همه فیلدها را پر کنید.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.loginWithPassword(identifier, password)) {
                is ApiResult.Success -> {
                    repository.persistSession(result.data)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.Authenticated(result.data))
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun requestOtpForLogin(identifier: String) {
        if (identifier.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "ایمیل یا شماره موبایل را وارد کنید.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.requestOtp(identifier)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.OtpRequested(identifier))
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String?,
        phoneNumber: String?,
        password: String
    ) {
        if (firstName.isBlank() || lastName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "نام و نام‌خانوادگی الزامی است.")
            return
        }
        if (email.isNullOrBlank() && phoneNumber.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "وارد کردن حداقل ایمیل یا شماره موبایل الزامی است.")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "رمز عبور باید حداقل ۶ کاراکتر باشد.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.register(firstName, lastName, email, phoneNumber, password)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.OtpRequested(result.data.identifier))
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun verifyOtp(identifier: String, code: String) {
        if (code.trim().length != 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "کد باید ۶ رقم باشد.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.verifyOtp(identifier, code.trim())) {
                is ApiResult.Success -> {
                    repository.persistSession(result.data)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(AuthEvent.Authenticated(result.data))
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun resendOtp(identifier: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = null, resendMessage = null)
            when (val result = repository.requestOtp(identifier)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(resendMessage = "کد جدید ارسال شد.")
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
            }
        }
    }
}