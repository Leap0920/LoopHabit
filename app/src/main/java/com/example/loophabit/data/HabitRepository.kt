package com.example.loophabit.data

import com.example.loophabit.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HabitRepository(
    private val habitDao: HabitDao,
    private val todoDao: TodoDao,
    private val userDao: UserDao,
    private val loopPreferences: LoopPreferences,
    private val syncManager: SyncManager? = null
) {
    val loopIndexFlow: Flow<Int> = loopPreferences.loopIndexFlow
    val currentUserIdFlow: Flow<Long> = loopPreferences.currentUserIdFlow
    val autoBackupIntervalFlow: Flow<Int> = loopPreferences.autoBackupIntervalFlow
    val autoBackupUriFlow: Flow<String?> = loopPreferences.autoBackupUriFlow

    suspend fun setAutoBackupInterval(intervalHours: Int) {
        loopPreferences.setAutoBackupInterval(intervalHours)
    }

    suspend fun setAutoBackupUri(uriString: String?) {
        loopPreferences.setAutoBackupUri(uriString)
    }

    // Sync state exposed to UI
    val syncState: Flow<com.example.loophabit.data.sync.SyncState> = syncManager?.syncState ?: MutableStateFlow(com.example.loophabit.data.sync.SyncState.Idle).asStateFlow()

    fun getAllHabits(userId: Long): Flow<List<Habit>> = habitDao.getAllHabits(userId)

    fun getTodosForUser(userId: Long): Flow<List<TodoItem>> = todoDao.getTodosForUser(userId)

    fun getIncompleteHabitsOfToday(userId: Long, date: String): Flow<List<Habit>> =
        habitDao.getIncompleteHabits(userId, date)

    fun getCompletedHabitsOfToday(userId: Long, date: String): Flow<List<Habit>> =
        habitDao.getCompletedHabits(userId, date)

    suspend fun addHabit(
        userId: Long,
        title: String,
        colorHex: String,
        targetDaysPerWeek: Int,
        date: String,
        isNumerical: Boolean = false,
        numericalGoal: Double = 0.0,
        numericalUnit: String = "",
        daysOfWeekPattern: String = "1111111"
    ) {
        val habit = Habit(
            userId = userId,
            title = title,
            colorHex = colorHex,
            targetDaysPerWeek = targetDaysPerWeek,
            isNumerical = isNumerical,
            numericalGoal = numericalGoal,
            numericalUnit = numericalUnit,
            daysOfWeekPattern = daysOfWeekPattern
        )
        habitDao.insertHabit(habit)
        validateIndex(userId, date)
    }

    suspend fun deleteHabit(habit: Habit, date: String) {
        habitDao.deleteHabit(habit)
        validateIndex(habit.userId, date)
    }

    suspend fun completeHabit(userId: Long, habitId: Long, date: String) {
        habitDao.insertCompletion(HabitCompletion(habitId = habitId, date = date))
        validateIndex(userId, date)
    }

    suspend fun uncompleteHabit(userId: Long, habitId: Long, date: String) {
        habitDao.deleteCompletion(habitId, date)
        validateIndex(userId, date)
    }

    suspend fun cycleIndex(userId: Long, direction: Int, date: String) {
        val incomplete = habitDao.getIncompleteHabits(userId, date).first()
        val size = incomplete.size
        if (size == 0) {
            loopPreferences.setLoopIndex(0)
            return
        }
        val currentIndex = loopPreferences.loopIndexFlow.first()
        val nextIndex = ((currentIndex + direction) % size + size) % size
        loopPreferences.setLoopIndex(nextIndex)
    }

    suspend fun setLoopIndex(index: Int) {
        loopPreferences.setLoopIndex(index)
    }

    suspend fun validateIndex(userId: Long, date: String) {
        val incomplete = habitDao.getIncompleteHabits(userId, date).first()
        val size = incomplete.size
        val currentIndex = loopPreferences.loopIndexFlow.first()
        if (size == 0) {
            loopPreferences.setLoopIndex(0)
        } else if (currentIndex < 0 || currentIndex >= size) {
            loopPreferences.setLoopIndex(0)
        }
    }

    // Authentication Operations
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)

    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)

    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)

    suspend fun registerUser(user: User): Long = userDao.insertUser(user)

    suspend fun updateUser(user: User): Int = userDao.updateUser(user)

    suspend fun setCurrentUserId(userId: Long) {
        loopPreferences.setCurrentUserId(userId)
    }

    fun getCompletionDates(habitId: Long): Flow<List<String>> = habitDao.getCompletionDates(habitId)

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> =
        habitDao.getCompletionsForHabit(habitId)

    fun getAllCompletionsForUser(userId: Long): Flow<List<HabitCompletion>> =
        habitDao.getAllCompletionsForUser(userId)

    suspend fun completeHabitWithNote(userId: Long, habitId: Long, date: String, notes: String?, value: Double = 0.0) {
        val existing = habitDao.getCompletion(habitId, date)
        if (existing != null) {
            habitDao.updateCompletion(existing.copy(notes = notes, value = value))
        } else {
            habitDao.insertCompletion(HabitCompletion(habitId = habitId, date = date, notes = notes, value = value))
        }
        validateIndex(userId, date)
    }

    suspend fun updateCompletionNotes(habitId: Long, date: String, notes: String?) {
        val completion = habitDao.getCompletion(habitId, date)
        if (completion != null) {
            habitDao.updateCompletion(completion.copy(notes = notes))
        }
    }

    fun getAllFocusSessions(userId: Long): Flow<List<FocusSession>> =
        habitDao.getAllFocusSessions(userId)

    suspend fun logFocusSession(userId: Long, habitId: Long?, durationSeconds: Int, details: String?) {
        habitDao.insertFocusSession(
            FocusSession(
                userId = userId,
                habitId = habitId,
                durationSeconds = durationSeconds,
                details = details
            )
        )
    }

    suspend fun addTodo(userId: Long, title: String, notes: String?) {
        todoDao.insertTodo(
            TodoItem(
                userId = userId,
                title = title.trim(),
                notes = notes?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun setManualFocusMinutes(
        userId: Long,
        habitId: Long,
        minutes: Int,
        details: String,
        timestamp: Long
    ) {
        val existing = habitDao.getFocusSessionByDetails(userId, habitId, details)
        if (minutes <= 0) {
            if (existing != null) {
                habitDao.deleteFocusSessionById(existing.id)
            }
            return
        }

        val durationSeconds = minutes * 60
        if (existing != null) {
            habitDao.updateFocusSession(
                existing.copy(
                    durationSeconds = durationSeconds,
                    timestamp = timestamp
                )
            )
        } else {
            habitDao.insertFocusSession(
                FocusSession(
                    userId = userId,
                    habitId = habitId,
                    durationSeconds = durationSeconds,
                    details = details,
                    timestamp = timestamp
                )
            )
        }
    }

    suspend fun updateTodo(todo: TodoItem, title: String, notes: String?) {
        todoDao.updateTodo(
            todo.copy(
                title = title.trim(),
                notes = notes?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun toggleTodo(todo: TodoItem) {
        val nowCompleted = !todo.isCompleted
        todoDao.updateTodo(
            todo.copy(
                isCompleted = nowCompleted,
                completedAt = if (nowCompleted) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun deleteTodo(todo: TodoItem) {
        todoDao.deleteTodo(todo)
    }

    val focusStateFlow: Flow<FocusState> = loopPreferences.focusStateFlow

    suspend fun saveFocusState(state: FocusState) {
        loopPreferences.saveFocusState(state)
    }

    suspend fun clearUserData(userId: Long) {
        habitDao.clearCompletionsForUser(userId)
        habitDao.clearFocusSessionsForUser(userId)
        todoDao.clearTodosForUser(userId)
        habitDao.clearHabitsForUser(userId)
    }

    suspend fun insertTodoDirect(todo: TodoItem) {
        todoDao.insertTodo(todo)
    }

    suspend fun insertHabitDirect(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun insertCompletionDirect(completion: HabitCompletion) {
        habitDao.insertCompletion(completion)
    }

    suspend fun insertFocusSessionDirect(session: FocusSession) {
        habitDao.insertFocusSession(session)
    }
}
