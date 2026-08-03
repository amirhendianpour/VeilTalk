package com.example.veiltalk.common.util

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RingtonePlayer {
    private var job: Job? = null
    private var toneGenerator: ToneGenerator? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        toneGenerator = ToneGenerator(AudioManager.STREAM_RING, 80)
        job = scope.launch {
            while (isActive) {
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1200)
                delay(2000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        toneGenerator?.release()
        toneGenerator = null
    }
}