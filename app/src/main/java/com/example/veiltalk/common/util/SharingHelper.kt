package com.example.veiltalk.common.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object SharingHelper {

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, text: String = "") {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "$fileName.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                if (text.isNotEmpty()) {
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری کد QR"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
