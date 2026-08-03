package com.example.veiltalk.feature.notification.data

import android.util.Log
import com.example.veiltalk.core.session.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val api: FcmApi,
    private val sessionManager: SessionManager
) {
    // بعد از لاگین موفق صدا زده می‌شود — معادل مرحله‌ای که در وب لازم نبود چون فقط با WebSocket زنده کار می‌کرد
    suspend fun registerCurrentDeviceToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            api.registerToken(FcmTokenRequestDto(token))
            sessionManager.saveFcmToken(token)
        } catch (e: Exception) {
            Log.w("FcmTokenRepository", "ثبت توکن FCM ناموفق بود: ${e.message}")
        }
    }

    // وقتی توکن توسط سیستم اندروید رفرش می‌شود (در FirebaseMessagingService.onNewToken)
    suspend fun onTokenRefreshed(newToken: String) {
        try {
            api.registerToken(FcmTokenRequestDto(newToken))
            sessionManager.saveFcmToken(newToken)
        } catch (e: Exception) {
            Log.w("FcmTokenRepository", "به‌روزرسانی توکن FCM ناموفق بود: ${e.message}")
        }
    }

    // هنگام لاگ‌اوت
    suspend fun unregisterCurrentDeviceToken() {
        try {
            val token = sessionManager.getFcmToken() ?: return
            api.unregisterToken(FcmTokenRequestDto(token))
        } catch (e: Exception) {
            Log.w("FcmTokenRepository", "حذف توکن FCM ناموفق بود: ${e.message}")
        }
    }
}