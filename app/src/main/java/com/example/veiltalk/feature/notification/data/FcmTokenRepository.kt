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
    private fun buildRequestDto(token: String): FcmTokenRequestDto {
        return FcmTokenRequestDto(
            token = token,
            deviceName = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            deviceModel = android.os.Build.MODEL,
            osVersion = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
        )
    }

    // بعد از لاگین موفق صدا زده می‌شود — معادل مرحله‌ای که در وب لازم نبود چون فقط با WebSocket زنده کار می‌کرد
    suspend fun registerCurrentDeviceToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            api.registerToken(buildRequestDto(token))
            sessionManager.saveFcmToken(token)
        } catch (e: Exception) {
            Log.w("FcmTokenRepository", "ثبت توکن FCM ناموفق بود: ${e.message}")
        }
    }

    // وقتی توکن توسط سیستم اندروید رفرش می‌شود (در FirebaseMessagingService.onNewToken)
    suspend fun onTokenRefreshed(newToken: String) {
        try {
            api.registerToken(buildRequestDto(newToken))
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

    suspend fun getActiveSessions(): Result<List<ActiveSessionDto>> {
        return try {
            val response = api.getActiveSessions()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("خطا در دریافت سشن‌ها"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun terminateSession(id: Long): Result<Unit> {
        return try {
            val response = api.terminateSession(id)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("خطا در حذف سشن"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun terminateOtherSessions(): Result<Unit> {
        return try {
            val token = sessionManager.getFcmToken() ?: ""
            val response = api.terminateOtherSessions(token)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("خطا در حذف سایر سشن‌ها"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}