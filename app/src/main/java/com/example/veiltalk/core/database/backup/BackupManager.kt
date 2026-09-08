package com.example.veiltalk.core.database.backup

import android.content.Context
import android.net.Uri
import com.example.veiltalk.core.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    private val DB_NAME = "veiltalk.db"

    /**
     * تولید کد ۳۰ رقمی مشابه سیگنال
     */
    fun generateRecoveryCode(): String {
        val random = SecureRandom()
        return (1..30).map { random.nextInt(10).toString() }.joinToString("")
            .chunked(5).joinToString("-") // فرمت: XXXXX-XXXXX-...
    }

    /**
     * ایجاد فایل بک‌آپ رمزنگاری شده
     */
    suspend fun createBackup(targetUri: Uri, recoveryCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanCode = recoveryCode.replace("-", "")
            val dbFile = context.getDatabasePath(DB_NAME)
            
            // ۱. بستن موقت دیتابیس برای جلوگیری از خرابی (Corruption)
            database.close()

            val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
            val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(cleanCode, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                // ذخیره Salt و IV در ابتدای فایل
                outputStream.write(salt)
                outputStream.write(iv)
                
                FileInputStream(dbFile).use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) outputStream.write(output)
                    }
                    val finalOutput = cipher.doFinal()
                    if (finalOutput != null) outputStream.write(finalOutput)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * بازگردانی دیتابیس از فایل
     */
    suspend fun restoreBackup(sourceUri: Uri, recoveryCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanCode = recoveryCode.replace("-", "")
            val dbFile = context.getDatabasePath(DB_NAME)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val salt = ByteArray(16)
                val iv = ByteArray(12)
                inputStream.read(salt)
                inputStream.read(iv)

                val secretKey = deriveKey(cleanCode, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

                database.close()

                FileOutputStream(dbFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val output = cipher.update(buffer, 0, bytesRead)
                        if (output != null) outputStream.write(output)
                    }
                    val finalOutput = cipher.doFinal()
                    if (finalOutput != null) outputStream.write(finalOutput)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
