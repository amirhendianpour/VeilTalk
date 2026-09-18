package com.example.veiltalk.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.veiltalk.core.session.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionManager: SessionManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                // فقط اگر کاربر قبلاً لاگین کرده باشد، سرویس را استارت می‌زنیم
                if (sessionManager.currentUsername != null) {
                    ChatConnectionService.start(context)
                }
            }
        }
    }
}
