package com.example.loophabit.ui

import com.example.loophabit.data.FocusSession
import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.LoopPreferences
import com.example.loophabit.data.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common ViewModel interface that both Android and iOS implement.
 * Screens should depend on this interface, not the concrete class.
 */
interface LoopHabitViewModel : InsightsViewModel {
    // User state
    val currentUserId: StateFlow<Long>
    val todayDate: String

    // Habits
    val incompleteHabits: StateFlow<List<Habit>>
    val completedHabits: StateFlow<List<Habit>>
    val loopIndex: StateFlow<Int>
    val currentHabit: StateFlow<Habit?>

    // Sync
    val syncState: StateFlow<SyncState>

    // Focus
    val focusHabitId: StateFlow<Long?>
    val focusState: StateFlow<LoopPreferences.FocusState>

    // Backup
    val autoBackupInterval: StateFlow<Int>
    val autoBackupUri: StateFlow<String?>

    // Habit operations
    fun addHabit(
        title: String,
        colorHex: String,
        targetDaysPerWeek: Int = 7,
        isNumerical: Boolean = false,
        numericalGoal: Double = 0.0,
        numericalUnit: String = "",
        daysOfWeekPattern: String = "1111111"
    )

    fun deleteHabit(habit: Habit)
    fun completeHabit(habitId: Long)
    fun uncompleteHabit(habitId: Long)
    fun nextHabit()
    fun prevHabit()
    fun setIndex(index: Int)

    // Focus
    fun setFocusHabitId(id: Long?)
    fun updateFocusState(state: LoopPreferences.FocusState)
    fun logFocusSession(habitId: Long?, durationSeconds: Int, details: String?)

    // Completion with notes
    fun toggleHabitCompletionForDate(
        habitId: Long,
        dateStr: String,
        wasCompleted: Boolean,
        notes: String? = null,
        value: Double = 0.0
    )

    fun completeHabitNumerical(habitId: Long, value: Double, notes: String? = null)
    fun saveCompletionNote(habitId: Long, dateStr: String, notes: String?, value: Double = 0.0)

    // Completions queries
    fun getCompletionDates(habitId: Long): Flow<List<String>>
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    // Authentication
    fun login(usernameOrEmail: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun register(
        username: String,
        email: String,
        password: String,
        securityQuestion: String,
        securityAnswer: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    fun getSecurityQuestion(email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit)
    fun resetPassword(email: String, answer: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun logout()

    // Backup
    fun setAutoBackupInterval(intervalHours: Int)
    fun setAutoBackupUri(uriString: String?)
    fun exportData()
    fun importData(jsonString: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {})
    fun resetAllData(onSuccess: () -> Unit = {})
}
