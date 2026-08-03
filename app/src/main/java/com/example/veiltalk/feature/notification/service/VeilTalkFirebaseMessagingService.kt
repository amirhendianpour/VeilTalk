package com.example.veiltalk.feature.notification.service

import android.Manifest
import android.R
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.veiltalk.MainActivity
import com.example.veiltalk.feature.notification.data.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class VeilTalkFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { fcmTokenRepository.onTokenRefreshed(token) }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "VeilTalk"
        val body = message.notification?.body ?: ""
        val type = message.data["type"] // PRIVATE_MESSAGE یا GROUP_MESSAGE
        val senderUsername = message.data["senderUsername"]

        showNotification(title, body, type, senderUsername)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, body: String, type: String?, senderUsername: String?) {
        val channelId = "veiltalk_messages_channel"
        createMessageChannelIfNeeded(channelId)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("notification_sender", senderUsername)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(Random.nextInt(), notification)
    }

    private fun createMessageChannelIfNeeded(channelId: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "پیام‌های جدید",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نوتیفیکیشن پیام‌های خصوصی و گروهی"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}