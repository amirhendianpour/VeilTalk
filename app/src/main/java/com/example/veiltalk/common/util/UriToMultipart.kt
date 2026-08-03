package com.example.veiltalk.common.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

data class PickedFile(
    val part: MultipartBody.Part,
    val displayName: String,
    val sizeBytes: Long
)

// نسخه‌ی قدیمی — مخصوص عکس پروفایل/گروه، دست‌نخورده برای حفظ سازگاری با کدهای قبلی
fun uriToMultipart(context: Context, uri: Uri, partName: String = "file"): MultipartBody.Part? {
    return try {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = if (mimeType.contains("png")) "png" else "jpg"
        val tempFile = File.createTempFile("upload_", ".$extension", context.cacheDir)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }

        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
        MultipartBody.Part.createFormData(partName, tempFile.name, requestBody)
    } catch (e: Exception) {
        null
    }
}

// نسخه‌ی عمومی برای پیوست چت (عکس یا هر فایل دیگر) — نام واقعی و پسوند اصلی فایل را حفظ می‌کند
// (نه فقط jpg/png فرضی نسخه بالا)، و اندازه‌ی فایل را هم برمی‌گرداند تا در UI نمایش داده شود.
fun uriToChatFilePart(context: Context, uri: Uri, partName: String = "file"): PickedFile? {
    return try {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"

        var displayName = "file"
        var size = 0L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }

        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
        val tempFile = File.createTempFile(
            "chatfile_",
            if (extension.isNotBlank()) ".$extension" else "",
            context.cacheDir
        )

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        if (size <= 0L) size = tempFile.length()

        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData(partName, displayName, requestBody)
        PickedFile(part, displayName, size)
    } catch (e: Exception) {
        null
    }
}