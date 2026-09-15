package com.example.veiltalk.common.util

import android.graphics.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrCodeHelper {

    fun generateQrCode(text: String, size: Int = 512): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createShareCard(
        displayName: String,
        username: String,
        qrBitmap: Bitmap,
        avatarBitmap: Bitmap? = null
    ): Bitmap {
        val width = 720
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        canvas.drawColor(Color.WHITE)

        // Top Header / Border
        paint.color = Color.parseColor("#E53935") // A shade of red
        canvas.drawRect(0f, 0f, width.toFloat(), 100f, paint)

        // Avatar
        val avatarSize = 160f
        val avatarX = (width - avatarSize) / 2
        val avatarY = 150f
        
        if (avatarBitmap != null) {
            val scaledAvatar = Bitmap.createScaledBitmap(avatarBitmap, avatarSize.toInt(), avatarSize.toInt(), true)
            val circlePath = Path().apply {
                addCircle(avatarX + avatarSize / 2, avatarY + avatarSize / 2, avatarSize / 2, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(circlePath)
            canvas.drawBitmap(scaledAvatar, avatarX, avatarY, paint)
            canvas.restore()
        } else {
            // Draw placeholder circle
            paint.color = Color.LTGRAY
            canvas.drawCircle(avatarX + avatarSize / 2, avatarY + avatarSize / 2, avatarSize / 2, paint)
            paint.color = Color.WHITE
            paint.textSize = 60f
            paint.textAlign = Paint.Align.CENTER
            val firstChar = if (displayName.isNotEmpty()) displayName[0].toString() else "?"
            canvas.drawText(firstChar, avatarX + avatarSize / 2, avatarY + avatarSize / 2 + 20, paint)
        }

        // Display Name
        paint.color = Color.BLACK
        paint.textSize = 48f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText(displayName, (width / 2).toFloat(), 380f, paint)

        // Username
        paint.color = Color.GRAY
        paint.textSize = 32f
        paint.isFakeBoldText = false
        canvas.drawText("@$username", (width / 2).toFloat(), 430f, paint)

        // QR Code
        val qrSize = 480
        val qrX = (width - qrSize) / 2f
        val qrY = 480f
        val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true)
        canvas.drawBitmap(scaledQr, qrX, qrY, paint)

        // Footer
        paint.color = Color.parseColor("#E53935")
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("VeilTalk", (width / 2).toFloat(), 1000f, paint)
        
        paint.color = Color.GRAY
        paint.textSize = 24f
        canvas.drawText("امنیت و حریم خصوصی در دستان شما", (width / 2).toFloat(), 1040f, paint)

        return bitmap
    }

    fun decodeQrCode(bitmap: Bitmap): String? {
        return try {
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source = com.google.zxing.RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            val reader = com.google.zxing.MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
