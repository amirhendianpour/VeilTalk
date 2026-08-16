package com.example.veiltalk

import android.app.Application
import com.example.veiltalk.core.service.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VeilTalkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}