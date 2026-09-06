package com.example.veiltalk.common.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object InvitationHelper {
    private const val INVITE_MESSAGE = "سلام! من از VeilTalk استفاده می‌کنم، یک پیام‌رسان امن و سریع. خوشحال میشم تو هم به من ملحق بشی:\nhttps://veiltalk.app/download"

    fun inviteViaSms(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", INVITE_MESSAGE)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: simple view intent
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$phoneNumber?body=${Uri.encode(INVITE_MESSAGE)}")
            }
            context.startActivity(fallbackIntent)
        }
    }
}
