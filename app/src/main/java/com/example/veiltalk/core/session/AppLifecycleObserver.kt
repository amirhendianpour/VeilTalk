package com.example.veiltalk.core.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.veiltalk.feature.chat.data.ChatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager
) : DefaultLifecycleObserver {

    private var isAppInForeground = false

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // اپلیکیشن به Foreground آمد
        isAppInForeground = true
        updatePresence(true)
    }

    override fun onStop(owner: LifecycleOwner) {
        // اپلیکیشن به Background رفت
        isAppInForeground = false
        // یک تاخیر کوچک برای اطمینان از اینکه کاربر واقعاً از اپ خارج شده (نه فقط چرخش صفحه)
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(1000)
            if (!isAppInForeground) {
                updatePresence(false)
            }
        }
    }

    private fun updatePresence(online: Boolean) {
        // فقط اگر کاربر لاگین کرده باشد وضعیت را بفرست
        val username = sessionManager.currentUsername
        if (username != null) {
            chatRepository.sendManualPresence(online)
        }
    }
}
