package com.example.veiltalk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionCheckViewModel @Inject constructor(
    val sessionManager: SessionManager
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    // این رو در MainActivity برای استارت/استاپ سرویس اتصال observe می‌کنیم
    val tokenFlow = sessionManager.tokenFlow

    init {
        viewModelScope.launch {
            val loggedIn = sessionManager.getToken() != null
            _startDestination.value = if (loggedIn) Routes.HOME else Routes.LOGIN
        }
    }
}