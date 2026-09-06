package com.example.veiltalk.feature.profile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.feature.profile.data.ProfileRepository
import com.example.veiltalk.feature.profile.data.dto.UserProfileResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProfileMode { VIEW, EDIT }

data class ProfileUiState(
    val profile: UserProfileResponseDto? = null,
    val mode: ProfileMode = ProfileMode.VIEW,
    val firstNameInput: String = "",
    val lastNameInput: String = "",
    val usernameInput: String = "",
    val bioInput: String = "",
    val emailInput: String = "",
    val phoneInput: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val showFullScreenAvatar: Boolean = false,
    val oldPasswordInput: String = "",
    val newPasswordInput: String = "",
    val isChangingPassword: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val authRepository: com.example.veiltalk.feature.auth.data.AuthRepository,
    private val mediaRepository: com.example.veiltalk.feature.chat.data.MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMyProfile()
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isLoading = false,
                        firstNameInput = profile.firstName,
                        lastNameInput = profile.lastName,
                        usernameInput = profile.username,
                        bioInput = profile.bio ?: "",
                        emailInput = profile.email ?: "",
                        phoneInput = profile.phoneNumber ?: ""
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
        }
    }

    fun enterEditMode() {
        val profile = _uiState.value.profile ?: return
        _uiState.value = _uiState.value.copy(
            mode = ProfileMode.EDIT,
            firstNameInput = profile.firstName,
            lastNameInput = profile.lastName,
            usernameInput = profile.username,
            bioInput = profile.bio ?: "",
            emailInput = profile.email ?: "",
            phoneInput = profile.phoneNumber ?: "",
            error = null
        )
    }

    fun cancelEdit() {
        val profile = _uiState.value.profile ?: return
        _uiState.value = _uiState.value.copy(
            mode = ProfileMode.VIEW,
            firstNameInput = profile.firstName,
            lastNameInput = profile.lastName,
            usernameInput = profile.username,
            bioInput = profile.bio ?: "",
            emailInput = profile.email ?: "",
            phoneInput = profile.phoneNumber ?: "",
            error = null
        )
    }

    fun onFirstNameChange(value: String) { _uiState.value = _uiState.value.copy(firstNameInput = value) }
    fun onLastNameChange(value: String) { _uiState.value = _uiState.value.copy(lastNameInput = value) }
    fun onUsernameChange(value: String) { _uiState.value = _uiState.value.copy(usernameInput = value) }
    fun onBioChange(value: String) {
        if (value.length <= 150) _uiState.value = _uiState.value.copy(bioInput = value)
    }
    fun onEmailChange(value: String) { _uiState.value = _uiState.value.copy(emailInput = value) }
    fun onPhoneChange(value: String) { _uiState.value = _uiState.value.copy(phoneInput = value) }

    fun onOldPasswordChange(value: String) { _uiState.value = _uiState.value.copy(oldPasswordInput = value) }
    fun onNewPasswordChange(value: String) { _uiState.value = _uiState.value.copy(newPasswordInput = value) }

    fun changePassword() {
        val state = _uiState.value
        if (state.oldPasswordInput.isBlank() || state.newPasswordInput.length < 6) {
            _uiState.value = state.copy(error = "لطفاً رمز فعلی و رمز جدید (حداقل ۶ کاراکتر) را وارد کنید.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangingPassword = true, error = null)
            when (val result = authRepository.changePassword(state.oldPasswordInput, state.newPasswordInput)) {
                is com.example.veiltalk.common.util.ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        oldPasswordInput = "",
                        newPasswordInput = ""
                    )
                    _uiEvent.emit("رمز عبور با موفقیت تغییر کرد.")
                }
                is com.example.veiltalk.common.util.ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(isChangingPassword = false, error = result.message)
                }
            }
        }
    }

    fun showFullScreenAvatar() {
        _uiState.value = _uiState.value.copy(showFullScreenAvatar = true)
    }

    fun hideFullScreenAvatar() {
        _uiState.value = _uiState.value.copy(showFullScreenAvatar = false)
    }

    fun saveProfilePicture() {
        val url = _uiState.value.profile?.profilePictureUrl ?: return
        val username = _uiState.value.profile?.username ?: "user"
        viewModelScope.launch {
            mediaRepository.saveToPublicStorage(url, null, "Avatar_$username.jpg")
                .onSuccess { _uiEvent.emit("عکس پروفایل ذخیره شد") }
                .onFailure { e -> _uiEvent.emit("خطا: ${e.message}") }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.firstNameInput.isBlank() || state.lastNameInput.isBlank() || state.usernameInput.isBlank()) {
            _uiState.value = state.copy(error = "نام، نام‌خانوادگی و آیدی نمی‌تواند خالی باشد.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            repository.updateProfile(
                firstName = state.firstNameInput.trim(),
                lastName = state.lastNameInput.trim(),
                bio = state.bioInput.trim(),
                email = state.emailInput.trim().ifBlank { null },
                phoneNumber = state.phoneInput.trim().ifBlank { null },
                username = state.usernameInput.trim().lowercase()
            ).onSuccess { updated ->
                _uiState.value = _uiState.value.copy(
                    profile = updated,
                    mode = ProfileMode.VIEW,
                    isSaving = false
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingAvatar = true, error = null)
            repository.uploadAvatar(uri)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(profile = updated, isUploadingAvatar = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isUploadingAvatar = false, error = e.message)
                }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAccount = true, error = null)
            repository.deleteAccount()
                .onSuccess { 
                    _uiState.value = _uiState.value.copy(isDeletingAccount = false)
                    _uiEvent.emit("حساب کاربری شما با موفقیت حذف شد.")
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isDeletingAccount = false, error = e.message)
                }
        }
    }
}