package com.example.veiltalk.feature.call.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.veiltalk.core.service.NotificationHelper
import com.example.veiltalk.feature.call.data.CallRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class CallForegroundService : Service() {

    @Inject lateinit var callRepository: CallRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        
        // مشاهده تغییرات وضعیت تماس و بروزرسانی نوتیفیکیشن
        callRepository.uiState
            .onEach { state ->
                if (state.status != com.example.veiltalk.common.model.CallStatus.IDLE) {
                    updateNotification(state)
                }
            }
            .launchIn(serviceScope)
    }

    private fun updateNotification(state: com.example.veiltalk.feature.call.data.CallUiSnapshot) {
        val remoteUser = state.remoteUser ?: "کاربر"
        val isVideo = state.callType == com.example.veiltalk.common.model.CallKind.VIDEO
        
        val notification = NotificationHelper.buildCallNotification(
            this, 
            remoteUser, 
            state.status.name, 
            isVideo
        )
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NotificationHelper.CALL_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val remoteUser = intent?.getStringExtra(EXTRA_REMOTE_USER) ?: "کاربر"
        val status = intent?.getStringExtra(EXTRA_STATUS) ?: "RINGING"
        val isVideo = intent?.getBooleanExtra(EXTRA_IS_VIDEO, false) ?: false
        
        val notification = NotificationHelper.buildCallNotification(this, remoteUser, status, isVideo)
        
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

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_REMOTE_USER = "extra_remote_user"
        private const val EXTRA_STATUS = "extra_status"
        private const val EXTRA_IS_VIDEO = "extra_is_video"

        fun start(context: Context, remoteUser: String, status: String = "RINGING", isVideo: Boolean = false) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                putExtra(EXTRA_REMOTE_USER, remoteUser)
                putExtra(EXTRA_STATUS, status)
                putExtra(EXTRA_IS_VIDEO, isVideo)
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