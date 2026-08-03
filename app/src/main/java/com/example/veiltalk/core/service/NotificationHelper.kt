package com.example.veiltalk.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "veiltalk_connection_channel"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "اتصال پیام‌رسان",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "نگه‌داشتن اتصال زنده برای دریافت آنی پیام‌ها"
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildConnectionNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VeilTalk")
            .setContentText("در حال دریافت پیام‌ها")
            .setSmallIcon(android.R.drawable.stat_notify_chat) // بعداً آیکون اختصاصی جایگزین می‌شه
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
}