package com.example.veiltalk.common.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object MediaCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    data class EncryptedResult(val encryptedData: ByteArray, val mediaKey: String)

    fun encrypt(data: ByteArray): EncryptedResult {
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH, iv))
        
        val ciphertext = cipher.doFinal(data)
        
        // ترکیب IV و کلید در یک رشته Base64 برای ارسال راحت‌تر
        // [Key(32 bytes)][IV(12 bytes)]
        val combinedKey = key + iv
        val mediaKeyBase64 = Base64.encodeToString(combinedKey, Base64.NO_WRAP)
        
        return EncryptedResult(ciphertext, mediaKeyBase64)
    }

    fun decrypt(encryptedData: ByteArray, mediaKeyBase64: String): ByteArray {
        val combinedKey = Base64.decode(mediaKeyBase64, Base64.NO_WRAP)
        val key = combinedKey.copyOfRange(0, 32)
        val iv = combinedKey.copyOfRange(32, combinedKey.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH, iv))
        
        return cipher.doFinal(encryptedData)
    }
}
