package com.example.veiltalk.common.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start() {
        val file = File(context.cacheDir, "voice_record_${System.currentTimeMillis()}.m4a")
        currentFile = file
        
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    fun stop(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return currentFile
    }

    fun cancel() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        currentFile?.delete()
        currentFile = null
    }
}
