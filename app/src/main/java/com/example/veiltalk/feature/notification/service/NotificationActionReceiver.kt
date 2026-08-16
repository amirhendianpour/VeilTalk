package com.example.veiltalk.feature.notification.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.veiltalk.feature.chat.data.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var groupRepository: com.example.veiltalk.feature.group.data.GroupRepository
    @Inject lateinit var callRepository: com.example.veiltalk.feature.call.data.CallRepository
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_MARK_AS_READ = "com.example.veiltalk.ACTION_MARK_AS_READ"
        const val ACTION_REPLY = "com.example.veiltalk.ACTION_REPLY"
        const val ACTION_ACCEPT_CALL = "com.example.veiltalk.ACTION_ACCEPT_CALL"
        const val ACTION_REJECT_CALL = "com.example.veiltalk.ACTION_REJECT_CALL"
        const val ACTION_HANGUP_CALL = "com.example.veiltalk.ACTION_HANGUP_CALL"
        
        const val EXTRA_PARTNER = "extra_partner"
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val partner = intent.getStringExtra(EXTRA_PARTNER)
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L).takeIf { it != -1L }
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_ACCEPT_CALL -> {
                callRepository.acceptCall()
            }
            ACTION_REJECT_CALL -> {
                callRepository.rejectCall()
            }
            ACTION_HANGUP_CALL -> {
                callRepository.endCall()
            }
            ACTION_MARK_AS_READ -> {
                scope.launch {
                    if (partner != null) chatRepository.markAsRead(partner)
                    // در حال حاضر متد markAsRead برای گروه‌ها نداریم، اما می‌توان نوتیف را حذف کرد
                    if (notificationId != -1) {
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                }
            }
            ACTION_REPLY -> {
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()
                
                if (!replyText.isNullOrBlank()) {
                    scope.launch {
                        if (groupId != null) {
                            groupRepository.sendGroupMessage(groupId, replyText)
                        } else if (partner != null) {
                            chatRepository.sendMessage(partner, replyText, com.example.veiltalk.common.model.MessageType.TEXT)
                            chatRepository.markAsRead(partner)
                        }
                        
                        if (notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                    }
                }
            }
        }
    }
}
