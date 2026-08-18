package com.example.veiltalk.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.example.veiltalk.MainActivity
import com.example.veiltalk.feature.notification.service.NotificationActionReceiver

object NotificationHelper {
    const val CONNECTION_CHANNEL_ID = "veiltalk_connection_channel"
    const val CALL_CHANNEL_ID = "veiltalk_call_channel"
    const val MESSAGE_CHANNEL_ID = "veiltalk_messages_channel"
    
    const val CONNECTION_NOTIFICATION_ID = 1001
    const val CALL_NOTIFICATION_ID = 1002
    const val MESSAGE_GROUP_ID = "com.example.veiltalk.MESSAGE_GROUP"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            // کانال اتصال (Low importance)
            val connectionChannel = NotificationChannel(
                CONNECTION_CHANNEL_ID,
                "وضعیت اتصال",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش زنده بودن برنامه در پس‌زمینه"
                setShowBadge(false)
            }
            manager.createNotificationChannel(connectionChannel)

            // کانال پیام‌ها (High importance)
            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "پیام‌های جدید",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "دریافت پیام‌های چت خصوصی و گروهی"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(messageChannel)

            // کانال تماس (High importance + sound/vibration)
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "تماس صوتی و تصویری",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نمایش تماس‌های ورودی و فعال"
                enableLights(true)
                enableVibration(true)
                setShowBadge(false)
            }
            manager.createNotificationChannel(callChannel)
        }
    }

    fun buildConnectionNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CONNECTION_CHANNEL_ID)
            .setContentTitle("VeilTalk")
            .setContentText("در حال دریافت پیام‌ها...")
            .setSmallIcon(com.example.veiltalk.R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun showMessageNotification(
        context: Context,
        partnerUsername: String,
        partnerDisplayName: String,
        messages: List<NotificationMessage>,
        avatarBitmap: Bitmap? = null,
        isGroup: Boolean = false,
        groupId: Long? = null,
        groupName: String? = null
    ) {
        val notificationId = if (isGroup) groupId?.hashCode() ?: partnerUsername.hashCode() else partnerUsername.hashCode()
        
        val userPerson = Person.Builder().setName("Me").build()
        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(if (isGroup) groupName else null)
            .setGroupConversation(isGroup)

        // اضافه کردن تمام پیام‌های اخیر به استایل
        messages.forEach { msg ->
            val personBuilder = Person.Builder()
                .setName(msg.senderName)
                .setKey(msg.senderUsername)
            
            // اگر فرستنده خود مخاطب اصلی است و آواتار داریم
            if (msg.senderUsername == partnerUsername && avatarBitmap != null) {
                personBuilder.setIcon(IconCompat.createWithBitmap(avatarBitmap.toCircleBitmap()))
            }
            
            messagingStyle.addMessage(
                msg.content,
                msg.timestamp,
                personBuilder.build()
            )
        }

        // اینتنت باز کردن چت
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (isGroup && groupId != null) {
                putExtra("group_id", groupId)
            } else {
                putExtra("chat_username", partnerUsername)
            }
        }
        val pendingContent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // اکشن پاسخ سریع (Reply)
        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
            .setLabel(if (isGroup) "پاسخ به گروه..." else "پاسخ به $partnerDisplayName...")
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REPLY
            if (isGroup && groupId != null) {
                putExtra(NotificationActionReceiver.EXTRA_GROUP_ID, groupId)
            } else {
                putExtra(NotificationActionReceiver.EXTRA_PARTNER, partnerUsername)
            }
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingReply = PendingIntent.getBroadcast(
            context, notificationId, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "پاسخ",
            pendingReply
        ).addRemoteInput(remoteInput).build()

        // اکشن خوانده شده (Mark as Read)
        val markAsReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_AS_READ
            if (!isGroup) putExtra(NotificationActionReceiver.EXTRA_PARTNER, partnerUsername)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingMarkAsRead = PendingIntent.getBroadcast(
            context, notificationId + 1, markAsReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markAsReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.checkbox_on_background,
            "خواندن",
            pendingMarkAsRead
        ).build()

        // ساخت نهایی نوتیفیکیشن
        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(com.example.veiltalk.R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .setContentIntent(pendingContent)
            .addAction(replyAction)
            .addAction(markAsReadAction)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setGroup(MESSAGE_GROUP_ID)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)

        updateSummaryNotification(context)
    }

    data class NotificationMessage(
        val senderUsername: String,
        val senderName: String,
        val content: String,
        val timestamp: Long
    )

    private fun updateSummaryNotification(context: Context) {
        val summary = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(com.example.veiltalk.R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("پیام‌های VeilTalk"))
            .setGroup(MESSAGE_GROUP_ID)
            .setGroupSummary(true)
            .build()
        
        context.getSystemService(NotificationManager::class.java).notify(999, summary)
    }

    fun buildCallNotification(context: Context, remoteUser: String, status: String, isVideo: Boolean): Notification {
        val notificationId = CALL_NOTIFICATION_ID
        
        // اینتنت باز کردن برنامه (صفحه تماس)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingContent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(com.example.veiltalk.R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(pendingContent)
            .setAutoCancel(false)

        when (status) {
            "RINGING" -> {
                // تماس ورودی برای دریافت‌کننده
                builder.setContentTitle("تماس ورودی")
                builder.setContentText("$remoteUser - ${if (isVideo) "تصویری" else "صوتی"}")
                
                // دکمه پاسخ
                val acceptIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_ACCEPT_CALL
                }
                val pendingAccept = PendingIntent.getBroadcast(
                    context, notificationId + 2, acceptIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_call, "پاسخ", pendingAccept)

                // دکمه رد تماس
                val rejectIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_REJECT_CALL
                }
                val pendingReject = PendingIntent.getBroadcast(
                    context, notificationId + 3, rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "رد تماس", pendingReject)
                
                builder.setFullScreenIntent(pendingContent, true)
            }
            "CALLING" -> {
                // تماس خروجی برای تماس‌گیرنده
                builder.setContentTitle("در حال تماس...")
                builder.setContentText("با $remoteUser")
                
                // دکمه پایان تماس (برای تماس‌گیرنده قبل از اتصال)
                val hangupIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_HANGUP_CALL
                }
                val pendingHangup = PendingIntent.getBroadcast(
                    context, notificationId + 4, hangupIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "پایان تماس", pendingHangup)
            }
            "CONNECTED" -> {
                // تماس فعال برای هر دو طرف
                builder.setContentTitle("تماس فعال")
                builder.setContentText("$remoteUser - ${if (isVideo) "تصویری" else "صوتی"}")
                
                val hangupIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_HANGUP_CALL
                }
                val pendingHangup = PendingIntent.getBroadcast(
                    context, notificationId + 4, hangupIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "پایان تماس", pendingHangup)
            }
            else -> {
                builder.setContentTitle("VeilTalk")
                builder.setContentText("تماس...")
            }
        }

        return builder.build()
    }

    // متد کمکی برای گرد کردن تصویر آواتار
    private fun Bitmap.toCircleBitmap(): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, width, height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, rect, rect, paint)
        return output
    }
}
