package com.example.veiltalk.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.core.websocket.StompManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatConnectionService : Service() {

    @Inject lateinit var stompManager: StompManager
    @Inject lateinit var sessionManager: SessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForeground(NotificationHelper.CONNECTION_NOTIFICATION_ID, NotificationHelper.buildConnectionNotification(this))

        serviceScope.launch {
            val token = sessionManager.getToken()
            if (token != null) {
                stompManager.connect(token)
            } else {
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val showNotification = intent?.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true) ?: true
        
        if (showNotification) {
            startForeground(NotificationHelper.CONNECTION_NOTIFICATION_ID, NotificationHelper.buildConnectionNotification(this))
        } else {
            // مخفی کردن نوتیفیکیشن بدون متوقف کردن سرویس
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        }
        
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // وقتی کاربر اپلیکیشن را از لیست Recent به بیرون می‌کشد (Swipe)
        // در واتساپ این کار باعث بستن کامل سرویس نمی‌شود.
        // ما اینجا تلاش می‌کنیم سرویس را زنده نگه داریم یا دوباره لانچ کنیم.
        val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
            setPackage(packageName)
            putExtra(EXTRA_SHOW_NOTIFICATION, true)
        }
        
        // اگر سیستم اجازه دهد، سرویس را ری‌استارت می‌کنیم
        val pendingIntent = android.app.PendingIntent.getService(
            this, 1, restartServiceIntent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent)
        
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stompManager.disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_SHOW_NOTIFICATION = "extra_show_notification"

        fun start(context: Context) {
            val intent = Intent(context, ChatConnectionService::class.java).apply {
                putExtra(EXTRA_SHOW_NOTIFICATION, true)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun updateNotificationVisibility(context: Context, isVisible: Boolean) {
            val intent = Intent(context, ChatConnectionService::class.java).apply {
                putExtra(EXTRA_SHOW_NOTIFICATION, isVisible)
            }
            // چون سرویس از قبل شروع شده، فقط استارت معمولی می‌زنیم تا onStartCommand اجرا شود
            context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChatConnectionService::class.java))
        }
    }
}