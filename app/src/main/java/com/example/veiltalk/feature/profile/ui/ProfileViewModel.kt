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
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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

    fun showFullScreenAvatar() {
        _uiState.value = _uiState.value.copy(showFullScreenAvatar = true)
    }

    fun hideFullScreenAvatar() {
        _uiState.value = _uiState.value.copy(showFullScreenAvatar = false)
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
}