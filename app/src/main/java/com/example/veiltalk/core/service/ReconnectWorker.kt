package com.example.veiltalk.core.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReconnectWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ChatConnectionService.start(applicationContext)
        return Result.success()
    }
}
