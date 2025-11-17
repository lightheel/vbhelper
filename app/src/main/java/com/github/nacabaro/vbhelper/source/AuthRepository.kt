package com.github.nacabaro.vbhelper.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_ID = longPreferencesKey("user_id")
    }

    val isAuthenticated: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_AUTHENTICATED] ?: false
        }

    val authToken: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[AUTH_TOKEN]
        }

    val userId: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[USER_ID]
        }

    suspend fun setAuthenticated(isAuthenticated: Boolean, token: String? = null, userId: Long? = null) {
        dataStore.edit { preferences ->
            preferences[IS_AUTHENTICATED] = isAuthenticated
            if (token != null) {
                preferences[AUTH_TOKEN] = token
            }
            if (userId != null) {
                preferences[USER_ID] = userId
            }
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences[IS_AUTHENTICATED] = false
            preferences.remove(AUTH_TOKEN)
            preferences.remove(USER_ID)
        }
    }
}

