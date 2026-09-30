package com.example.veiltalk.core.session

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.veiltalk.core.service.ChatConnectionService
import com.example.veiltalk.core.websocket.StompManager
import com.example.veiltalk.feature.chat.data.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager,
    private val stompManager: StompManager
) : DefaultLifecycleObserver {

    private var isAppInForeground = false

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // اپلیکیشن به Foreground آمد
        isAppInForeground = true
        updatePresence(true)
        
        // اطمینان از زنده بودن سرویس و وصل مجدد وب‌سوکت در صورت قطعی
        ChatConnectionService.start(context)
        stompManager.reconnectIfDisconnected()
        
        // واکشی تاریخچه پیام‌هایی که احتمالاً در زمان حضور در پس‌زمینه ارسال شده‌اند
        chatRepository.fetchHistory()
    }

    override fun onStop(owner: LifecycleOwner) {
        // اپلیکیشن به Background رفت
        isAppInForeground = false
        // یک تاخیر کوچک برای اطمینان از اینکه کاربر واقعاً از اپ خارج شده (نه فقط چرخش صفحه)
        MainScope().launch {
            delay(1000)
            if (!isAppInForeground) {
                updatePresence(false)
                ChatConnectionService.start(context)
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
