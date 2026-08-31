package com.example.veiltalk.common.util

import android.content.Context
import androidx.core.content.FileProvider
import com.example.veiltalk.common.di.SessionManagerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Helper برای دانلود موقت فایل‌ها و باز کردن آن‌ها.
 * منطق اصلی جابجایی به ریپازیتوری منتقل شده است.
 */
object FileDownloader {
    
    suspend fun downloadAndDecrypt(
        context: Context,
        url: String,
        mediaKey: String?,
        fileName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            // این تابع برای کارهای سریع یا موقت باقی مانده است.
            // در چت‌ها از MediaRepository استفاده می‌شود.
            val hashedUrl = hashString(url)
            val outputDir = File(context.cacheDir, "downloads").apply { mkdirs() }
            val outputFile = File(outputDir, "${hashedUrl}_$fileName")

            if (outputFile.exists()) return@withContext outputFile

            val sessionManager = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SessionManagerEntryPoint::class.java
            ).sessionManager()
            val token = sessionManager.getToken()

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val rawBytes = response.body?.bytes() ?: return@withContext null
            
            val finalBytes = if (mediaKey != null) {
                MediaCrypto.decrypt(rawBytes, mediaKey)
            } else {
                rawBytes
            }

            FileOutputStream(outputFile).use { it.write(finalBytes) }
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
