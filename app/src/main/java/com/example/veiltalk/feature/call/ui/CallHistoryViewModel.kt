package com.example.veiltalk.feature.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.call.data.dao.CallLogDao
import com.example.veiltalk.feature.call.data.entity.CallLogEntity
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallHistoryUiState(
    val logs: List<CallLogItem> = emptyList(),
    val isLoading: Boolean = false
)

data class CallLogItem(
    val id: Long,
    val remoteUsername: String,
    val remoteDisplayName: String,
    val remoteProfilePicture: String?,
    val callType: String,
    val direction: String,
    val status: String,
    val startTime: Long,
    val duration: Long
)

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    private val callLogDao: CallLogDao,
    private val userDirectory: UserDirectoryRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val uiState: StateFlow<CallHistoryUiState> = sessionManager.usernameFlow.flatMapLatest { owner ->
        if (owner == null) flowOf(emptyList<CallLogEntity>())
        else callLogDao.getAllLogsFlow(owner)
    }.combine(userDirectory.directory) { logs, directory ->
        val items = logs.map { log ->
            val info = directory[log.remoteUser]
            CallLogItem(
                id = log.id,
                remoteUsername = log.remoteUser,
                remoteDisplayName = if (info != null) "${info.firstName} ${info.lastName}".trim().ifBlank { log.remoteUser } else log.remoteUser,
                remoteProfilePicture = info?.profilePictureUrl,
                callType = log.callType,
                direction = log.direction,
                status = log.status,
                startTime = log.startTime,
                duration = log.duration
            )
        }
        CallHistoryUiState(logs = items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallHistoryUiState(isLoading = true))

    init {
        viewModelScope.launch {
            sessionManager.usernameFlow.collect { owner ->
                if (owner != null) {
                    callLogDao.getAllLogsFlow(owner).first().let { logs ->
                        userDirectory.ensureLoaded(logs.map { it.remoteUser }.distinct())
                    }
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val owner = sessionManager.usernameFlow.first() ?: return@launch
            callLogDao.clearLogs(owner)
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            callLogDao.deleteLog(id)
        }
    }
}
