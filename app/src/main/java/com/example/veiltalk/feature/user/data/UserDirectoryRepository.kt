package com.example.veiltalk.feature.user.data

import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.feature.user.data.dto.BatchInfoRequestDto
import com.example.veiltalk.feature.user.data.dto.UserInfoDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDirectoryRepository @Inject constructor(
    private val api: UserApi,
    private val contactDao: com.example.veiltalk.core.database.dao.ContactDao,
    private val sessionManager: com.example.veiltalk.core.session.SessionManager,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _directory = MutableStateFlow<Map<String, UserInfoDto>>(emptyMap())
    val directory: StateFlow<Map<String, UserInfoDto>> = _directory.asStateFlow()

    private val _onlineStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val onlineStatus: StateFlow<Map<String, Boolean>> = _onlineStatus.asStateFlow()

    private val _lastSeen = MutableStateFlow<Map<String, String?>>(emptyMap())
    val lastSeen: StateFlow<Map<String, String?>> = _lastSeen.asStateFlow()

    fun updateStatus(username: String, online: Boolean, lastSeen: String? = null) {
        _onlineStatus.value = _onlineStatus.value + (username to online)
        if (lastSeen != null) {
            _lastSeen.value = _lastSeen.value + (username to lastSeen)
        }
    }

    fun isOnline(username: String): Boolean = _onlineStatus.value[username] ?: false
    fun getLastSeen(username: String): String? = _lastSeen.value[username]

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
        val current = _directory.value[info.username]
        val merged = if (current != null) {
            // ترکیب اطلاعات: اگر فیلد جدید خالی یا نال بود، از مقدار قبلی استفاده کن
            info.copy(
                phoneNumber = info.phoneNumber?.takeIf { it.isNotBlank() } ?: current.phoneNumber,
                email = info.email?.takeIf { it.isNotBlank() } ?: current.email,
                bio = info.bio?.takeIf { it.isNotBlank() } ?: current.bio,
                profilePictureUrl = info.profilePictureUrl?.takeIf { it.isNotBlank() } ?: current.profilePictureUrl
            )
        } else {
            info
        }
        _directory.value = _directory.value + (info.username to merged)
    }

    fun ensureLoaded(usernames: List<String>) {
        scope.launch {
            val me = sessionManager.usernameFlow.first()
            var added = false
            mutex.withLock {
                usernames.forEach { u ->
                    if (u.isNotBlank() && !_directory.value.containsKey(u) && !pending.contains(u)) {
                        // قبل از درخواست از سرور، چک کن آیا در مخاطبین محلی دیتایی داریم؟
                        if (me != null) {
                            val local = contactDao.getContact(me, u)
                            if (local != null) {
                                setUserInfo(
                                    UserInfoDto(
                                        username = local.username,
                                        firstName = local.firstName,
                                        lastName = local.lastName,
                                        profilePictureUrl = local.profilePictureUrl,
                                        phoneNumber = local.phoneNumber,
                                        email = local.email,
                                        bio = local.bio
                                    )
                                )
                            }
                        }
                        
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
