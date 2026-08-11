package com.example.veiltalk.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "veiltalk_connection_channel"
    const val CALL_CHANNEL_ID = "veiltalk_call_channel"
    const val NOTIFICATION_ID = 1001
    const val CALL_NOTIFICATION_ID = 1002

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val connectionChannel = NotificationChannel(
                CHANNEL_ID,
                "اتصال پیام‌رسان",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "نگه‌داشتن اتصال زنده برای دریافت آنی پیام‌ها"
                setShowBadge(false)
            }
            manager.createNotificationChannel(connectionChannel)

            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "تماس صوتی و تصویری",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نمایش وضعیت تماس فعال"
            }
            manager.createNotificationChannel(callChannel)
        }
    }

    fun buildConnectionNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("VeilTalk")
            .setContentText("در حال دریافت پیام‌ها")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    fun buildCallNotification(context: Context, remoteUser: String): Notification {
        return NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setContentTitle("تماس فعال")
            .setContentText("در حال گفتگو با $remoteUser")
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()
    }
}