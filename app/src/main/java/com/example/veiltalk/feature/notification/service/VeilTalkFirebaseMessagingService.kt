package com.example.veiltalk.feature.notification.service

import android.Manifest
import android.R
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.RequiresPermission
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.veiltalk.core.database.dao.GroupMessageDao
import com.example.veiltalk.core.database.dao.MessageDao
import com.example.veiltalk.core.service.NotificationHelper
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.notification.data.FcmTokenRepository
import com.example.veiltalk.feature.user.data.UserDirectoryRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class VeilTalkFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository
    @Inject lateinit var userDirectory: UserDirectoryRepository
    @Inject lateinit var messageDao: MessageDao
    @Inject lateinit var groupMessageDao: GroupMessageDao
    @Inject lateinit var sessionManager: SessionManager

    private val scope = CoroutineScope(Dispatchers.IO)

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { fcmTokenRepository.onTokenRefreshed(token) }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val messageId = remoteMessage.data["id"] ?: return
        val senderUsername = remoteMessage.data["senderUsername"] ?: return
        val content = remoteMessage.data["content"] ?: remoteMessage.notification?.body ?: "پیام جدید"
        val type = remoteMessage.data["type"] // PRIVATE_MESSAGE یا GROUP_MESSAGE
        val groupName = remoteMessage.data["groupName"]
        val groupId = remoteMessage.data["groupId"]?.toLongOrNull()

        scope.launch {
            val me = sessionManager.currentUsername ?: return@launch
            
            // چک کردن اینکه آیا پیام قبلاً توسط وب‌سوکت دریافت شده است یا خیر
            val alreadyExists = if (type == "GROUP_MESSAGE" && groupId != null) {
                groupMessageDao.getMessageById(messageId, me) != null
            } else {
                messageDao.getMessageById(messageId, me) != null
            }

            if (alreadyExists) return@launch

            // اطمینان از لود شدن اطلاعات کاربر
            userDirectory.ensureLoaded(listOf(senderUsername))
            val displayName = userDirectory.getDisplayName(senderUsername)
            val avatarUrl = userDirectory.getProfilePicture(senderUsername)
            
            val bitmap = avatarUrl?.let { loadAvatar(it) }

            withContext(Dispatchers.Main) {
                val messages = listOf(
                    NotificationHelper.NotificationMessage(
                        senderUsername = senderUsername,
                        senderName = displayName,
                        content = content,
                        timestamp = System.currentTimeMillis()
                    )
                )

                NotificationHelper.showMessageNotification(
                    context = this@VeilTalkFirebaseMessagingService,
                    partnerUsername = senderUsername,
                    partnerDisplayName = displayName,
                    messages = messages,
                    avatarBitmap = bitmap,
                    isGroup = type == "GROUP_MESSAGE",
                    groupId = groupId,
                    groupName = groupName
                )
            }
        }
    }

    private suspend fun loadAvatar(url: String): Bitmap? {
        val loader = ImageLoader(this)
        val request = ImageRequest.Builder(this)
            .data(url)
            .allowHardware(false) // برای نوتیفیکیشن نباید هاردویر بیت‌مپ باشه
            .build()
        
        return when (val result = loader.execute(request)) {
            is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
            else -> null
        }
    }
}
