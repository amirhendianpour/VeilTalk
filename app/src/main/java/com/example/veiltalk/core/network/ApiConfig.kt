package com.example.veiltalk.core.network

import com.example.veiltalk.BuildConfig

object ApiConfig {
    // آدرس پایه بک‌اند — از BuildConfig خونده می‌شه (معادل apiConfig.ts در نسخه وب)
    const val BASE_URL: String = BuildConfig.BASE_URL

    // آدرس وب‌سوکت از همون آدرس پایه ساخته می‌شه
    val WS_URL: String get() = "$BASE_URL/ws-chat"
}