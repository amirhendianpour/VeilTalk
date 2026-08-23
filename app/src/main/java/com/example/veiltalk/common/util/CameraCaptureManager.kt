package com.example.veiltalk.common.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CameraCaptureManager {
    fun createTempImageUri(context: Context): Uri {
        val tempFile = File.createTempFile("captured_image_", ".jpg", context.cacheDir).apply {
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    fun createTempVideoUri(context: Context): Uri {
        val tempFile = File.createTempFile("captured_video_", ".mp4", context.cacheDir).apply {
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
}
