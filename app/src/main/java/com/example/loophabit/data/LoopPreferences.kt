package com.example.loophabit.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loop_preferences")

class LoopPreferences(private val context: Context) {
    companion object {
        val LOOP_INDEX_KEY = intPreferencesKey("loop_index")
        val CURRENT_USER_ID_KEY = longPreferencesKey("current_user_id")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
        val AUTO_BACKUP_INTERVAL_KEY = intPreferencesKey("auto_backup_interval")
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

    val autoBackupIntervalFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_BACKUP_INTERVAL_KEY] ?: 0 // Default to 0 (Disabled)
        }

    val focusStateFlow: Flow<FocusState> = context.dataStore.data
        .map { preferences ->
            FocusState(
                isRunning = preferences[booleanPreferencesKey("focus_is_running")] ?: false,
                mode = preferences[stringPreferencesKey("focus_mode")] ?: "TIMER",
                habitId = preferences[longPreferencesKey("focus_habit_id")] ?: 0L,
                habitTitle = preferences[stringPreferencesKey("focus_habit_title")] ?: "",
                taskDetails = preferences[stringPreferencesKey("focus_task_details")] ?: "",
                initialDurationMinutes = preferences[intPreferencesKey("focus_initial_duration_minutes")] ?: 25,
                pausedSeconds = preferences[intPreferencesKey("focus_paused_seconds")] ?: (25 * 60),
                baseTimestamp = preferences[longPreferencesKey("focus_base_timestamp")] ?: 0L
            )
        }

    suspend fun saveFocusState(state: FocusState) {
        context.dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("focus_is_running")] = state.isRunning
            preferences[stringPreferencesKey("focus_mode")] = state.mode
            preferences[longPreferencesKey("focus_habit_id")] = state.habitId
            preferences[stringPreferencesKey("focus_habit_title")] = state.habitTitle
            preferences[stringPreferencesKey("focus_task_details")] = state.taskDetails
            preferences[intPreferencesKey("focus_initial_duration_minutes")] = state.initialDurationMinutes
            preferences[intPreferencesKey("focus_paused_seconds")] = state.pausedSeconds
            preferences[longPreferencesKey("focus_base_timestamp")] = state.baseTimestamp
        }
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

    suspend fun setAutoBackupInterval(intervalHours: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_INTERVAL_KEY] = intervalHours
        }
    }
}

data class FocusState(
    val isRunning: Boolean = false,
    val mode: String = "TIMER",
    val habitId: Long = 0L,
    val habitTitle: String = "",
    val taskDetails: String = "",
    val initialDurationMinutes: Int = 25,
    val pausedSeconds: Int = 25 * 60,
    val baseTimestamp: Long = 0L
)
