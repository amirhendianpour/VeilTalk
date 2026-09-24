package com.example.veiltalk.core.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReconnectScheduler {
    fun schedule(context: Context) {
        val work = PeriodicWorkRequestBuilder<ReconnectWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "reconnect_worker",
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
    }
}
