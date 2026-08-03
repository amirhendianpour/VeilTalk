package com.example.veiltalk.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "veiltalk_session")

@Singleton
class SessionManager @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val TOKEN = stringPreferencesKey("chat_token")
        val USERNAME = stringPreferencesKey("chat_username")
        val DISPLAY_NAME = stringPreferencesKey("chat_display_name")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USERNAME] }
    val displayNameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.DISPLAY_NAME] }

    suspend fun getToken(): String? = tokenFlow.first()
    suspend fun getUsername(): String? = usernameFlow.first()

    suspend fun saveSession(token: String, username: String, displayName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USERNAME] = username
            prefs[Keys.DISPLAY_NAME] = displayName
        }
    }

    suspend fun updateDisplayName(displayName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = displayName
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = getToken() != null

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { prefs -> prefs[Keys.FCM_TOKEN] = token }
    }

    suspend fun getFcmToken(): String? = context.dataStore.data.map { it[Keys.FCM_TOKEN] }.first()
}