package com.example.loophabit.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loop_preferences")

class LoopPreferences(private val context: Context) {
    companion object {
        val LOOP_INDEX_KEY = intPreferencesKey("loop_index")
        val CURRENT_USER_ID_KEY = longPreferencesKey("current_user_id")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
    }

    val loopIndexFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[LOOP_INDEX_KEY] ?: 0
        }

    val currentUserIdFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[CURRENT_USER_ID_KEY] ?: 0L
        }

    val darkModeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    suspend fun setLoopIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[LOOP_INDEX_KEY] = index
        }
    }

    suspend fun setCurrentUserId(userId: Long) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID_KEY] = userId
        }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
}
