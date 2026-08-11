package com.example.veiltalk.feature.call.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.veiltalk.core.service.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val remoteUser = intent?.getStringExtra(EXTRA_REMOTE_USER) ?: "کاربر"
        
        val notification = NotificationHelper.buildCallNotification(this, remoteUser)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.CALL_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NotificationHelper.CALL_NOTIFICATION_ID, notification)
        }
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_REMOTE_USER = "extra_remote_user"

        fun start(context: Context, remoteUser: String) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                putExtra(EXTRA_REMOTE_USER, remoteUser)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallForegroundService::class.java))
        }
    }
}