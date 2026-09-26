package com.example.veiltalk.core.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import android.content.Context
import com.example.veiltalk.core.service.ChatConnectionService
import com.example.veiltalk.feature.chat.data.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context,
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
        
        // مخفی کردن نوتیفیکیشن ثابت (چون کاربر داخل اپ است)
        ChatConnectionService.updateNotificationVisibility(context, false)
        
        // واکشی پیام‌هایی که احتمالاً در زمان حضور در پس‌زمینه ارسال شده‌اند
        chatRepository.fetchHistory()
    }

    override fun onStop(owner: LifecycleOwner) {
        // اپلیکیشن به Background رفت
        isAppInForeground = false
        // یک تاخیر کوچک برای اطمینان از اینکه کاربر واقعاً از اپ خارج شده (نه فقط چرخش صفحه)
        kotlinx.coroutines.MainScope().launch {
            kotlinx.coroutines.delay(1000)
            if (!isAppInForeground) {
                updatePresence(false)
                // نمایش مجدد نوتیفیکیشن (برای جلوگیری از بسته شدن سرویس توسط اندروید)
                ChatConnectionService.updateNotificationVisibility(context, true)
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
