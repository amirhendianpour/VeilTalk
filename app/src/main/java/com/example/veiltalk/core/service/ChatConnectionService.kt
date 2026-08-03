package com.example.veiltalk.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
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
        startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildConnectionNotification(this))

        serviceScope.launch {
            val token = sessionManager.getToken()
            if (token != null) {
                stompManager.connect(token)
            } else {
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        stompManager.disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ChatConnectionService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ChatConnectionService::class.java))
        }
    }
}