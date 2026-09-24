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
    @Inject lateinit var messageApi: com.example.veiltalk.feature.chat.data.MessageApi
    @Inject lateinit var callRepository: com.example.veiltalk.feature.call.data.CallRepository

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
        val type = remoteMessage.data["type"] // PRIVATE_MESSAGE یا GROUP_MESSAGE
        val groupName = remoteMessage.data["groupName"]
        val groupId = remoteMessage.data["groupId"]?.toLongOrNull()
        
        // استخراج عنوان و متن از دیتا (برای Data-only messages اولویت بالا)
        val title = remoteMessage.data["title"] ?: "VeilTalk"
        val content = remoteMessage.data["body"] ?: remoteMessage.data["content"] ?: "پیام جدید"

        if (type == "CALL") {
            handleCallPush(remoteMessage.data)
            return
        }

        scope.launch {
            val me = sessionManager.currentUsername ?: return@launch
            
            // ۱. ارسال رسید تحویل بلافاصله (Delivery Receipt) مشابه واتساپ
            // این کار باعث می‌شود فرستنده متوجه شود پیام به گوشی رسیده حتی اگر اپ بسته باشد
            // بیدار کردن سرویس اتصال برای دریافت پیام از وب‌سوکت
            com.example.veiltalk.core.service.ChatConnectionService.start(this@VeilTalkFirebaseMessagingService)

            try {
                messageApi.postReceipt(
                    com.example.veiltalk.feature.chat.data.dto.ReceiptDto(
                        messageId = messageId,
                        recipient = senderUsername,
                        status = "DELIVERED",
                        groupId = groupId
                    )
                )
            } catch (e: Exception) {
                // Ignore network errors for receipts
            }

            // ۲. چک کردن اینکه آیا پیام قبلاً توسط وب‌سوکت دریافت شده است یا خیر
            val alreadyExists = if (type == "GROUP_MESSAGE" && groupId != null) {
                groupMessageDao.getMessageById(messageId, me) != null
            } else {
                messageDao.getMessageById(messageId, me) != null
            }

            if (alreadyExists) return@launch

            // ۳. نمایش نوتیفیکیشن
            userDirectory.ensureLoaded(listOf(senderUsername))
            val displayName = userDirectory.getDisplayName(senderUsername)
            val avatarUrl = userDirectory.getProfilePicture(senderUsername)
            
            val bitmap = avatarUrl?.let { loadAvatar(it) }

            withContext(Dispatchers.Main) {
                // پارسر گرافیکی نوتیفیکیشن برای مدیریت پیام‌های تصویر، فایل و ریپلای‌های پیشرفته استوری کلاینت
                val displayContent = when {
                    content.startsWith("[STORY_MEDIA:") -> {
                        val closeIndex = content.indexOf("]")
                        if (closeIndex != -1) {
                            val rawText = content.substring(closeIndex + 1)
                            val cleanText = rawText
                                .replace("🎬 پاسخ به استوری شما", "")
                                .replace("✨ واکنش به استوری شما", "")
                                .replace("💬", "")
                                .trim()
                            if (content.contains("واکنش")) "✨ واکنش به استوری: $cleanText" else "🎬 پاسخ به استوری: $cleanText"
                        } else "🎬 پاسخ به استوری"
                    }
                    remoteMessage.data["messageType"] == "IMAGE" -> "📷 تصویر"
                    remoteMessage.data["messageType"] == "FILE" -> "📁 فایل"
                    remoteMessage.data["messageType"] == "VOICE" -> "🎤 پیام صوتی"
                    remoteMessage.data["messageType"] == "STICKER" -> "🏷️ استیکر"
                    remoteMessage.data["messageType"] == "GIF" -> "🎬 گیف"
                    else -> content
                }

                val messages = listOf(
                    NotificationHelper.NotificationMessage(
                        senderUsername = senderUsername,
                        senderName = displayName,
                        content = displayContent,
                        timestamp = System.currentTimeMillis()
                    )
                )

                NotificationHelper.showMessageNotification(
                    context = this@VeilTalkFirebaseMessagingService,
                    partnerUsername = senderUsername,
                    partnerDisplayName = if (type == "GROUP_MESSAGE") groupName ?: title else displayName,
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

    private fun handleCallPush(data: Map<String, String>) {
        val from = data["senderUsername"] ?: return
        val callId = data["callId"] ?: return
        val callType = data["callType"] ?: "AUDIO"
        val sdp = data["sdp"]

        scope.launch {
            val me = sessionManager.getUsername() ?: return@launch
            
            // فرار از کاراکترهای کوتیشن برای جلوگیری از خرابی ساختار JSON
            val escapedSdp = sdp?.replace("\"", "\\\"") ?: "null"
            val sdpValue = if (sdp.isNullOrBlank()) "null" else "\"$escapedSdp\""

            // شبیه‌سازی سیگنال OFFER برای CallRepository
            val signalJson = """
                {
                    "type": "OFFER",
                    "from": "$from",
                    "to": "$me",
                    "sdp": $sdpValue,
                    "callId": "$callId",
                    "callType": "$callType"
                }
            """.trimIndent()

            // اطمینان از اینکه سرویس اتصال در پس‌زمینه در حال اجراست تا وب‌سوکت وصل شود
            com.example.veiltalk.core.service.ChatConnectionService.start(this@VeilTalkFirebaseMessagingService)
            
            withContext(Dispatchers.Main) {
                callRepository.handleSignalFromPush(signalJson)
            }
        }
    }
}
