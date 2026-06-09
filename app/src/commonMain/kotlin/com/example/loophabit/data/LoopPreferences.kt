package com.example.loophabit.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoopPreferences(private val settings: Settings) {
    companion object {
        const val KEY_LOOP_INDEX = "loop_index"
        const val KEY_CURRENT_USER_ID = "current_user_id"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_AUTO_BACKUP_INTERVAL = "auto_backup_interval"
        const val KEY_AUTO_BACKUP_URI = "auto_backup_uri"
        const val KEY_FOCUS_IS_RUNNING = "focus_is_running"
        const val KEY_FOCUS_MODE = "focus_mode"
        const val KEY_FOCUS_HABIT_ID = "focus_habit_id"
        const val KEY_FOCUS_HABIT_TITLE = "focus_habit_title"
        const val KEY_FOCUS_TASK_DETAILS = "focus_task_details"
        const val KEY_FOCUS_INITIAL_DURATION = "focus_initial_duration_minutes"
        const val KEY_FOCUS_PAUSED_SECONDS = "focus_paused_seconds"
        const val KEY_FOCUS_BASE_TIMESTAMP = "focus_base_timestamp"
    }

    // Loop Index
    private val _loopIndex = MutableStateFlow(settings.getInt(KEY_LOOP_INDEX, 0))
    val loopIndexFlow: Flow<Int> = _loopIndex.asStateFlow()
    val loopIndex: Int get() = settings.getInt(KEY_LOOP_INDEX, 0)

    suspend fun setLoopIndex(index: Int) {
        settings.putInt(KEY_LOOP_INDEX, index)
        _loopIndex.value = index
    }

    // Current User ID
    private val _currentUserId = MutableStateFlow(settings.getLong(KEY_CURRENT_USER_ID, 0L))
    val currentUserIdFlow: Flow<Long> = _currentUserId.asStateFlow()
    val currentUserId: Long get() = settings.getLong(KEY_CURRENT_USER_ID, 0L)

    suspend fun setCurrentUserId(userId: Long) {
        settings.putLong(KEY_CURRENT_USER_ID, userId)
        _currentUserId.value = userId
    }

    // Dark Mode
    private val _darkModeEnabled = MutableStateFlow(settings.getBoolean(KEY_DARK_MODE, false))
    val darkModeEnabledFlow: Flow<Boolean> = _darkModeEnabled.asStateFlow()
    val darkModeEnabled: Boolean get() = settings.getBoolean(KEY_DARK_MODE, false)

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_DARK_MODE, enabled)
        _darkModeEnabled.value = enabled
    }

    // Auto Backup Interval
    private val _autoBackupInterval = MutableStateFlow(settings.getInt(KEY_AUTO_BACKUP_INTERVAL, 0))
    val autoBackupIntervalFlow: Flow<Int> = _autoBackupInterval.asStateFlow()
    val autoBackupInterval: Int get() = settings.getInt(KEY_AUTO_BACKUP_INTERVAL, 0)

    suspend fun setAutoBackupInterval(interval: Int) {
        settings.putInt(KEY_AUTO_BACKUP_INTERVAL, interval)
        _autoBackupInterval.value = interval
    }

    // Auto Backup URI
    private val _autoBackupUri = MutableStateFlow(settings.getString(KEY_AUTO_BACKUP_URI, ""))
    val autoBackupUriFlow: Flow<String?> = _autoBackupUri.asStateFlow()
    val autoBackupUri: String get() = settings.getString(KEY_AUTO_BACKUP_URI, "")

    suspend fun setAutoBackupUri(uri: String?) {
        val safeUri = uri ?: ""
        settings.putString(KEY_AUTO_BACKUP_URI, safeUri)
        _autoBackupUri.value = safeUri
    }

    // Focus State
    data class FocusState(
        val isRunning: Boolean = false,
        val mode: String = "timer",
        val habitId: Long = 0L,
        val habitTitle: String = "",
        val taskDetails: String = "",
        val initialDurationMinutes: Int = 0,
        val pausedSeconds: Int = 0,
        val baseTimestamp: Long = 0L
    )

    val focusState: FocusState
        get() = FocusState(
            isRunning = settings.getBoolean(KEY_FOCUS_IS_RUNNING, false),
            mode = settings.getString(KEY_FOCUS_MODE, "timer"),
            habitId = settings.getLong(KEY_FOCUS_HABIT_ID, 0L),
            habitTitle = settings.getString(KEY_FOCUS_HABIT_TITLE, ""),
            taskDetails = settings.getString(KEY_FOCUS_TASK_DETAILS, ""),
            initialDurationMinutes = settings.getInt(KEY_FOCUS_INITIAL_DURATION, 0),
            pausedSeconds = settings.getInt(KEY_FOCUS_PAUSED_SECONDS, 0),
            baseTimestamp = settings.getLong(KEY_FOCUS_BASE_TIMESTAMP, 0L)
        )

    private val _focusState = MutableStateFlow(focusState)
    val focusStateFlow: Flow<FocusState> = _focusState.asStateFlow()

    suspend fun saveFocusState(state: FocusState) {
        updateFocusState(
            isRunning = state.isRunning,
            mode = state.mode,
            habitId = state.habitId,
            habitTitle = state.habitTitle,
            taskDetails = state.taskDetails,
            initialDurationMinutes = state.initialDurationMinutes,
            pausedSeconds = state.pausedSeconds,
            baseTimestamp = state.baseTimestamp
        )
    }

    suspend fun updateFocusState(
        isRunning: Boolean = focusState.isRunning,
        mode: String = focusState.mode,
        habitId: Long = focusState.habitId,
        habitTitle: String = focusState.habitTitle,
        taskDetails: String = focusState.taskDetails,
        initialDurationMinutes: Int = focusState.initialDurationMinutes,
        pausedSeconds: Int = focusState.pausedSeconds,
        baseTimestamp: Long = focusState.baseTimestamp
    ) {
        settings.putBoolean(KEY_FOCUS_IS_RUNNING, isRunning)
        settings.putString(KEY_FOCUS_MODE, mode)
        settings.putLong(KEY_FOCUS_HABIT_ID, habitId)
        settings.putString(KEY_FOCUS_HABIT_TITLE, habitTitle)
        settings.putString(KEY_FOCUS_TASK_DETAILS, taskDetails)
        settings.putInt(KEY_FOCUS_INITIAL_DURATION, initialDurationMinutes)
        settings.putInt(KEY_FOCUS_PAUSED_SECONDS, pausedSeconds)
        settings.putLong(KEY_FOCUS_BASE_TIMESTAMP, baseTimestamp)
        _focusState.value = FocusState(isRunning, mode, habitId, habitTitle, taskDetails, initialDurationMinutes, pausedSeconds, baseTimestamp)
    }

    suspend fun clearFocusState() {
        updateFocusState(
            isRunning = false,
            mode = "timer",
            habitId = 0L,
            habitTitle = "",
            taskDetails = "",
            initialDurationMinutes = 0,
            pausedSeconds = 0,
            baseTimestamp = 0L
        )
    }
}
