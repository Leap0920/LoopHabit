package com.example.loophabit.ui

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitRepository
import com.example.loophabit.data.User
import com.example.loophabit.data.sync.SyncManager
import com.example.loophabit.data.sync.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(
    private val repository: HabitRepository,
    private val syncManager: SyncManager,
    private val applicationContext: Context
) : ViewModel() {

    val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val currentUserId: StateFlow<Long> = repository.currentUserIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val allHabits: StateFlow<List<Habit>> = currentUserId
        .flatMapLatest { userId ->
            if (userId == 0L) flowOf(emptyList()) else repository.getAllHabits(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompletions: StateFlow<List<com.example.loophabit.data.HabitCompletion>> = currentUserId
        .flatMapLatest { userId ->
            if (userId == 0L) flowOf(emptyList()) else repository.getAllCompletionsForUser(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFocusSessions: StateFlow<List<com.example.loophabit.data.FocusSession>> = currentUserId
        .flatMapLatest { userId ->
            if (userId == 0L) flowOf(emptyList()) else repository.getAllFocusSessions(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incompleteHabits: StateFlow<List<Habit>> = currentUserId
        .flatMapLatest { userId ->
            if (userId == 0L) flowOf(emptyList()) else repository.getIncompleteHabitsOfToday(userId, todayDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedHabits: StateFlow<List<Habit>> = currentUserId
        .flatMapLatest { userId ->
            if (userId == 0L) flowOf(emptyList()) else repository.getCompletedHabitsOfToday(userId, todayDate)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loopIndex: StateFlow<Int> = repository.loopIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentHabit: StateFlow<Habit?> = combine(
        incompleteHabits,
        repository.loopIndexFlow
    ) { habits, index ->
        if (habits.isEmpty()) return@combine null
        val safeIndex = if (index < 0 || index >= habits.size) 0 else index
        habits[safeIndex]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Sync state exposed to UI
    val syncState: StateFlow<SyncState> = syncManager.syncState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncState.Idle)

    private val _focusHabitId = MutableStateFlow<Long?>(null)
    val focusHabitId = _focusHabitId.asStateFlow()

    fun setFocusHabitId(id: Long?) {
        _focusHabitId.value = id
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                com.example.loophabit.widget.HabitWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addHabit(
        title: String,
        colorHex: String,
        targetDaysPerWeek: Int = 7,
        isNumerical: Boolean = false,
        numericalGoal: Double = 0.0,
        numericalUnit: String = "",
        daysOfWeekPattern: String = "1111111"
    ) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.addHabit(
                    userId = userId,
                    title = title,
                    colorHex = colorHex,
                    targetDaysPerWeek = targetDaysPerWeek,
                    date = todayDate,
                    isNumerical = isNumerical,
                    numericalGoal = numericalGoal,
                    numericalUnit = numericalUnit,
                    daysOfWeekPattern = daysOfWeekPattern
                )
                triggerSync()
                updateWidget()
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit, todayDate)
            triggerSync()
            updateWidget()
        }
    }

    fun completeHabit(habitId: Long) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.completeHabit(userId, habitId, todayDate)
                triggerSync()
                updateWidget()
            }
        }
    }

    fun logFocusSession(habitId: Long?, durationSeconds: Int, details: String?) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.logFocusSession(userId, habitId, durationSeconds, details)
            }
        }
    }

    fun uncompleteHabit(habitId: Long) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.uncompleteHabit(userId, habitId, todayDate)
                triggerSync()
                updateWidget()
            }
        }
    }

    fun nextHabit() {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.cycleIndex(userId, 1, todayDate)
                triggerSync()
                updateWidget()
            }
        }
    }

    fun prevHabit() {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.cycleIndex(userId, -1, todayDate)
                triggerSync()
                updateWidget()
            }
        }
    }

    fun setIndex(index: Int) {
        viewModelScope.launch {
            repository.setLoopIndex(index)
            triggerSync()
            updateWidget()
        }
    }

    // Trigger background sync after data changes
    private fun triggerSync() {
        viewModelScope.launch {
            syncManager.fullSync()
        }
    }

    // Authentication Actions
    fun login(usernameOrEmail: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (usernameOrEmail.isBlank() || password.isBlank()) {
                onError("Please fill in all fields")
                return@launch
            }
            var user = repository.getUserByEmail(usernameOrEmail)
            if (user == null) {
                user = repository.getUserByUsername(usernameOrEmail)
            }
            if (user == null) {
                onError("Account not found")
            } else if (user.password != password) {
                onError("Invalid password")
            } else {
                repository.setCurrentUserId(user.id)
                onSuccess()
                triggerSync()
            }
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        securityQuestion: String,
        securityAnswer: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (username.isBlank() || email.isBlank() || password.isBlank() || securityQuestion.isBlank() || securityAnswer.isBlank()) {
                onError("Please fill in all fields")
                return@launch
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                onError("Invalid email format")
                return@launch
            }
            val existingEmail = repository.getUserByEmail(email)
            if (existingEmail != null) {
                onError("Email already registered")
                return@launch
            }
            val existingUsername = repository.getUserByUsername(username)
            if (existingUsername != null) {
                onError("Username already taken")
                return@launch
            }

            val newUser = User(
                username = username,
                email = email,
                password = password,
                securityQuestion = securityQuestion,
                securityAnswer = securityAnswer
            )
            val insertedId = repository.registerUser(newUser)
            if (insertedId > 0) {
                repository.setCurrentUserId(insertedId)
                onSuccess()
                triggerSync()
            } else {
                onError("Registration failed. Please try again.")
            }
        }
    }

    fun getSecurityQuestion(email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (email.isBlank()) {
                onError("Please enter your email")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user == null) {
                onError("Account not found")
            } else {
                onSuccess(user.securityQuestion)
            }
        }
    }

    fun resetPassword(
        email: String,
        answer: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (email.isBlank() || answer.isBlank() || newPassword.isBlank()) {
                onError("Please fill in all fields")
                return@launch
            }
            val user = repository.getUserByEmail(email)
            if (user == null) {
                onError("Account not found")
                return@launch
            }
            if (!user.securityAnswer.equals(answer, ignoreCase = true)) {
                onError("Incorrect answer to security question")
            } else {
                val updatedUser = user.copy(password = newPassword)
                val rows = repository.updateUser(updatedUser)
                if (rows > 0) {
                    onSuccess()
                } else {
                    onError("Failed to update password. Please try again.")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.setCurrentUserId(0L)
            repository.setLoopIndex(0)
            triggerSync()
        }
    }

    fun getCompletionDates(habitId: Long): kotlinx.coroutines.flow.Flow<List<String>> =
        repository.getCompletionDates(habitId)

    fun getCompletionsForHabit(habitId: Long): kotlinx.coroutines.flow.Flow<List<com.example.loophabit.data.HabitCompletion>> =
        repository.getCompletionsForHabit(habitId)

    fun toggleHabitCompletionForDate(
        habitId: Long,
        dateStr: String,
        wasCompleted: Boolean,
        notes: String? = null,
        value: Double = 0.0
    ) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId == 0L) return@launch
            if (wasCompleted) {
                repository.uncompleteHabit(userId, habitId, dateStr)
            } else {
                repository.completeHabitWithNote(userId, habitId, dateStr, notes, value)
            }
        }
    }

    fun completeHabitNumerical(habitId: Long, value: Double, notes: String? = null) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.completeHabitWithNote(userId, habitId, todayDate, notes, value)
                triggerSync()
                updateWidget()
            }
        }
    }

    fun saveCompletionNote(habitId: Long, dateStr: String, notes: String?, value: Double = 0.0) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId == 0L) return@launch
            repository.completeHabitWithNote(userId, habitId, dateStr, notes, value)
        }
    }

    fun calculateStreaks(dates: List<String>, daysOfWeekPattern: String = "1111111"): Pair<Int, Int> {
        if (dates.isEmpty()) return Pair(0, 0)
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val sortedDates = dates.map { java.time.LocalDate.parse(it, formatter) }.distinct().sorted()

            val safePattern = if (daysOfWeekPattern.length == 7) daysOfWeekPattern else "1111111"

            fun isRestDay(date: java.time.LocalDate): Boolean {
                val dayOfWeekVal = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)
                val idx = dayOfWeekVal - 1
                return if (idx in safePattern.indices) safePattern[idx] == '0' else false
            }

            var bestStreak = 0
            var tempStreak = 0

            val startDate = sortedDates.first()
            val today = java.time.LocalDate.now()
            val endDate = if (today.isAfter(sortedDates.last())) today else sortedDates.last()

            var curr = startDate
            while (!curr.isAfter(endDate)) {
                val isCompleted = sortedDates.contains(curr)
                if (isCompleted) {
                    tempStreak++
                } else {
                    if (isRestDay(curr)) {
                        // Rest day: streak does not break, it freezes/preserves
                    } else {
                        if (curr == today) {
                            // Today is not completed yet, but user still has time
                        } else {
                            // Past active day skipped: streak breaks
                            if (tempStreak > bestStreak) {
                                bestStreak = tempStreak
                            }
                            tempStreak = 0
                        }
                    }
                }
                curr = curr.plusDays(1)
            }

            if (tempStreak > bestStreak) {
                bestStreak = tempStreak
            }

            Pair(tempStreak, bestStreak)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0, 0)
        }
    }
}

class HabitViewModelFactory(
    private val repository: HabitRepository,
    private val syncManager: SyncManager,
    private val applicationContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repository, syncManager, applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

