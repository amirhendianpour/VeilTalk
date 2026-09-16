package com.example.veiltalk.feature.chat.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.veiltalk.MainActivity
import com.example.veiltalk.R
import com.example.veiltalk.common.model.MessageType
import com.example.veiltalk.feature.chat.data.ChatRepository
import com.example.veiltalk.feature.group.data.GroupRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import javax.inject.Inject

@AndroidEntryPoint
class LiveLocationService : Service() {

    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var groupRepository: GroupRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationManager: LocationManager? = null
    
    private var chatId: String? = null
    private var isGroup: Boolean = false
    private var currentMessageId: String? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateBackend(location)
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        chatId = intent?.getStringExtra("chatId")
        isGroup = intent?.getBooleanExtra("isGroup", false) ?: false

        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                if (locationManager?.isProviderEnabled(provider) == true) {
                    locationManager?.requestLocationUpdates(
                        provider,
                        10000L, // 10 seconds
                        5f,     // 5 meters
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBackend(location: Location) {
        val target = chatId ?: return
        serviceScope.launch {
            val content = "${location.latitude},${location.longitude}"
            if (isGroup) {
                target.toLongOrNull()?.let { gid ->
                    if (currentMessageId == null) {
                        currentMessageId = groupRepository.sendGroupMessage(gid, content, MessageType.LIVE_LOCATION)
                    } else {
                        groupRepository.editGroupMessage(gid, currentMessageId!!, content, MessageType.LIVE_LOCATION)
                    }
                }
            } else {
                if (currentMessageId == null) {
                    currentMessageId = chatRepository.sendMessage(target, content, MessageType.LIVE_LOCATION)
                } else {
                    chatRepository.editMessage(currentMessageId!!, target, content, MessageType.LIVE_LOCATION)
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "live_location_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Live Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VeilTalk")
            .setContentText("در حال اشتراک‌گذاری مکان زنده...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        val mid = currentMessageId
        val target = chatId
        if (mid != null && target != null) {
            // استفاده از GlobalScope برای اطمینان از ارسال پیام نهایی قبل از بسته شدن کامل
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    if (isGroup) {
                        target.toLongOrNull()?.let { gid ->
                            groupRepository.editGroupMessage(gid, mid, "اشتراک‌گذاری متوقف شد", MessageType.LOCATION)
                        }
                    } else {
                        chatRepository.editMessage(mid, target, "اشتراک‌گذاری متوقف شد", MessageType.LOCATION)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            android.widget.Toast.makeText(applicationContext, "اشتراک‌گذاری مکان متوقف شد", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        locationManager?.removeUpdates(locationListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, chatId: String, isGroup: Boolean) {
            val intent = Intent(context, LiveLocationService::class.java).apply {
                putExtra("chatId", chatId)
                putExtra("isGroup", isGroup)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveLocationService::class.java))
        }
    }
}
