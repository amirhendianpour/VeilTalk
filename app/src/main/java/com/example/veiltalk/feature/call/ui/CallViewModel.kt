package com.example.veiltalk.feature.call.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.common.model.CallKind
import com.example.veiltalk.common.util.RingtonePlayer
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.call.data.CallRepository
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    val callRepository: CallRepository,
    private val userDirectory: UserDirectoryRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val uiState = callRepository.uiState
    val localVideoTrack = callRepository.localVideoTrack
    val remoteVideoTrack = callRepository.remoteVideoTrack

    private val ringtone = RingtonePlayer()

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                state.remoteUser?.let { userDirectory.ensureLoaded(listOf(it)) }
                if (state.status.name == "RINGING") {
                    ringtone.start(viewModelScope)
                } else {
                    ringtone.stop()
                }
            }
        }
    }

    fun remoteDisplayName(): String {
        val remote = uiState.value.remoteUser ?: return ""
        return userDirectory.getDisplayName(remote)
    }

    fun startCall(recipient: String, kind: CallKind) = callRepository.startCall(recipient, kind)
    fun acceptCall() = callRepository.acceptCall()
    fun rejectCall() = callRepository.rejectCall()
    fun endCall() = callRepository.endCall()
    fun toggleMute() = callRepository.toggleMute()
    fun toggleCamera() = callRepository.toggleCamera()

    override fun onCleared() {
        ringtone.stop()
        super.onCleared()
    }
}