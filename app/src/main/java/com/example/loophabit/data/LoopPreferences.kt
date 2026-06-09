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
        val AUTO_BACKUP_URI_KEY = stringPreferencesKey("auto_backup_uri")

        // Focus state keys (avoid creating new key objects on every read/write)
        val FOCUS_IS_RUNNING_KEY = booleanPreferencesKey("focus_is_running")
        val FOCUS_MODE_KEY = stringPreferencesKey("focus_mode")
        val FOCUS_HABIT_ID_KEY = longPreferencesKey("focus_habit_id")
        val FOCUS_HABIT_TITLE_KEY = stringPreferencesKey("focus_habit_title")
        val FOCUS_TASK_DETAILS_KEY = stringPreferencesKey("focus_task_details")
        val FOCUS_INITIAL_DURATION_KEY = intPreferencesKey("focus_initial_duration_minutes")
        val FOCUS_PAUSED_SECONDS_KEY = intPreferencesKey("focus_paused_seconds")
        val FOCUS_BASE_TIMESTAMP_KEY = longPreferencesKey("focus_base_timestamp")
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

    val autoBackupUriFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_BACKUP_URI_KEY]
        }

    val focusStateFlow: Flow<FocusState> = context.dataStore.data
        .map { preferences ->
            FocusState(
                isRunning = preferences[FOCUS_IS_RUNNING_KEY] ?: false,
                mode = preferences[FOCUS_MODE_KEY] ?: "TIMER",
                habitId = preferences[FOCUS_HABIT_ID_KEY] ?: 0L,
                habitTitle = preferences[FOCUS_HABIT_TITLE_KEY] ?: "",
                taskDetails = preferences[FOCUS_TASK_DETAILS_KEY] ?: "",
                initialDurationMinutes = preferences[FOCUS_INITIAL_DURATION_KEY] ?: 25,
                pausedSeconds = preferences[FOCUS_PAUSED_SECONDS_KEY] ?: (25 * 60),
                baseTimestamp = preferences[FOCUS_BASE_TIMESTAMP_KEY] ?: 0L
            )
        }

    suspend fun saveFocusState(state: FocusState) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_IS_RUNNING_KEY] = state.isRunning
            preferences[FOCUS_MODE_KEY] = state.mode
            preferences[FOCUS_HABIT_ID_KEY] = state.habitId
            preferences[FOCUS_HABIT_TITLE_KEY] = state.habitTitle
            preferences[FOCUS_TASK_DETAILS_KEY] = state.taskDetails
            preferences[FOCUS_INITIAL_DURATION_KEY] = state.initialDurationMinutes
            preferences[FOCUS_PAUSED_SECONDS_KEY] = state.pausedSeconds
            preferences[FOCUS_BASE_TIMESTAMP_KEY] = state.baseTimestamp
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

    suspend fun setAutoBackupUri(uriString: String?) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[AUTO_BACKUP_URI_KEY] = uriString
            } else {
                preferences.remove(AUTO_BACKUP_URI_KEY)
            }
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
