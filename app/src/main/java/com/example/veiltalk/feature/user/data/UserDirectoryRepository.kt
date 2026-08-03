package com.example.veiltalk.feature.user.data

import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.feature.user.data.dto.BatchInfoRequestDto
import com.example.veiltalk.feature.user.data.dto.UserInfoDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDirectoryRepository @Inject constructor(
    private val api: UserApi,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _directory = MutableStateFlow<Map<String, UserInfoDto>>(emptyMap())
    val directory: StateFlow<Map<String, UserInfoDto>> = _directory.asStateFlow()

    private val pending = mutableSetOf<String>()
    private val mutex = Mutex()
    private var flushJob: kotlinx.coroutines.Job? = null

    fun getDisplayName(username: String): String {
        val info = _directory.value[username] ?: return username
        val full = "${info.firstName} ${info.lastName}".trim()
        return full.ifBlank { username }
    }

    fun getProfilePicture(username: String): String? = _directory.value[username]?.profilePictureUrl

    fun setUserInfo(info: UserInfoDto) {
        _directory.value = _directory.value + (info.username to info)
    }

    fun ensureLoaded(usernames: List<String>) {
        scope.launch {
            var added = false
            mutex.withLock {
                usernames.forEach { u ->
                    if (u.isNotBlank() && !_directory.value.containsKey(u) && !pending.contains(u)) {
                        pending.add(u)
                        added = true
                    }
                }
            }
            if (added) {
                flushJob?.cancel()
                flushJob = scope.launch {
                    delay(50) // batch کردن چند درخواست پشت‌سرهم — معادل setTimeout(flush, 50) در وب
                    flush()
                }
            }
        }
    }

    suspend fun lookupUser(identifier: String): Result<UserInfoDto> {
        return try {
            val response = api.lookupUser(identifier)
            if (response.isSuccessful && response.body() != null) {
                val info = response.body()!!
                setUserInfo(info)
                Result.success(info)
            } else {
                Result.failure(Exception("کاربری با این مشخصات یافت نشد."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun flush() {
        val usernames: List<String>
        mutex.withLock {
            usernames = pending.toList()
            pending.clear()
        }
        if (usernames.isEmpty()) return
        try {
            val response = api.batchInfo(BatchInfoRequestDto(usernames))
            if (response.isSuccessful) {
                val results = response.body().orEmpty()
                _directory.value = _directory.value + results.associateBy { it.username }
            }
        } catch (e: Exception) {
            // اگه fail شد، دفعه بعد که ensureLoaded صدا زده بشه دوباره تلاش می‌شه
        }
    }
}