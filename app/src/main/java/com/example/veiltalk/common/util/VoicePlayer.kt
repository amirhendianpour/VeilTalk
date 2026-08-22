package com.example.veiltalk.common.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

class VoicePlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun play(url: String, onComplete: () -> Unit) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(context, Uri.parse(url))
            setOnCompletionListener {
                onComplete()
                stop()
            }
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

    fun stop() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
