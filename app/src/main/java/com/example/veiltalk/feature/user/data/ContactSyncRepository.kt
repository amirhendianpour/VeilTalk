package com.example.veiltalk.feature.user.data

import android.content.Context
import android.provider.ContactsContract
import com.example.veiltalk.core.database.dao.ContactDao
import com.example.veiltalk.core.database.entity.ContactEntity
import com.example.veiltalk.core.session.SessionManager
import com.example.veiltalk.feature.user.data.dto.ContactSyncRequestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userApi: UserApi,
    private val contactDao: ContactDao,
    private val userDirectory: UserDirectoryRepository, // اضافه شد
    private val sessionManager: SessionManager
) {
    suspend fun syncContacts(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val me = sessionManager.usernameFlow.first() ?: return@withContext Result.failure(Exception("Not logged in"))
            
            // ۱. خواندن شماره تلفن‌ها از گوشی
            val phoneNumbers = fetchPhoneNumbers()
            if (phoneNumbers.isEmpty()) return@withContext Result.success(0)

            // ۲. ارسال به سرور برای یافتن اعضا
            val response = userApi.syncContacts(ContactSyncRequestDto(phoneNumbers))
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Server error: ${response.code()}"))
            }

            val matchedContacts = response.body().orEmpty()

            // ۳. ذخیره در دیتابیس محلی
            matchedContacts.forEach { dto ->
                val entity = ContactEntity(
                    username = dto.username,
                    ownerUsername = me,
                    firstName = dto.firstName,
                    lastName = dto.lastName,
                    profilePictureUrl = null,
                    phoneNumber = dto.phoneNumber
                )
                contactDao.upsert(entity)
                
                // همچنین بلافاصله دایرکتوری در حافظه موقت را هم آپدیت کن
                userDirectory.setUserInfo(
                    com.example.veiltalk.feature.user.data.dto.UserInfoDto(
                        username = dto.username,
                        firstName = dto.firstName,
                        lastName = dto.lastName,
                        phoneNumber = dto.phoneNumber
                    )
                )
            }

            Result.success(matchedContacts.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class PhoneContact(val name: String, val phoneNumber: String)

    suspend fun fetchAllPhoneContacts(): List<PhoneContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<PhoneContact>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: ""
                val rawNumber = it.getString(numberIndex) ?: ""
                val cleanNumber = sanitizePhoneNumber(rawNumber)
                if (cleanNumber.isNotBlank()) {
                    contacts.add(PhoneContact(name, cleanNumber))
                }
            }
        }
        // Group by number to avoid duplicates if one person has multiple entries
        contacts.distinctBy { it.phoneNumber }
    }

    private fun fetchPhoneNumbers(): List<String> {
        // ... (Keep existing implementation for internal sync use, or just map fetchAllPhoneContacts)
        val numbers = mutableSetOf<String>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val rawNumber = it.getString(numberIndex)
                val cleanNumber = sanitizePhoneNumber(rawNumber)
                if (cleanNumber.isNotBlank()) {
                    numbers.add(cleanNumber)
                }
            }
        }
        return numbers.toList()
    }

    private fun sanitizePhoneNumber(number: String): String {
        // حذف تمام کاراکترهای غیر عددی به جز + در ابتدا
        var cleaned = number.replace(Regex("[^0-9+]"), "")
        
        // اگر شماره با 0 شروع شده، فرض می‌کنیم ایران است (اگر پیش‌فرض سیستم شما این است)
        // در اپلیکیشن‌های پیشرفته‌تر، کد کشور فعلی سیم‌کارت را می‌گیرند
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2)
        } else if (cleaned.startsWith("0")) {
            cleaned = "+98" + cleaned.substring(1)
        }
        
        return cleaned
    }
}
