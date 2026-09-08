package com.example.veiltalk.feature.profile.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.core.database.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isLoading: Boolean = false,
    val recoveryCode: String? = null,
    val showRecoveryCodeDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun startBackupFlow() {
        val code = backupManager.generateRecoveryCode()
        _uiState.value = _uiState.value.copy(recoveryCode = code, showRecoveryCodeDialog = true)
    }

    fun dismissRecoveryDialog() {
        _uiState.value = _uiState.value.copy(showRecoveryCodeDialog = false)
    }

    fun executeBackup(targetUri: Uri) {
        val code = _uiState.value.recoveryCode ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            backupManager.createBackup(targetUri, code)
                .onSuccess {
                    _uiEvent.emit("بک‌آپ با موفقیت ذخیره شد.")
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun executeRestore(sourceUri: Uri, recoveryCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            backupManager.restoreBackup(sourceUri, recoveryCode)
                .onSuccess {
                    _uiEvent.emit("اطلاعات با موفقیت بازیابی شد. اپلیکیشن را دوباره باز کنید.")
                    // در دنیای واقعی اینجا باید اپلیکیشن را ریستارت کرد
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = "کد نامعتبر است یا فایل بک‌آپ مشکل دارد.")
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
