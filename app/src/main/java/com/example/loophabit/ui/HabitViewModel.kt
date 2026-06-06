package com.example.loophabit.ui

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
    private val syncManager: SyncManager
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

    fun addHabit(title: String, colorHex: String, targetDaysPerWeek: Int = 7) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.addHabit(userId, title, colorHex, targetDaysPerWeek, todayDate)
                triggerSync()
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit, todayDate)
            triggerSync()
        }
    }

    fun completeHabit(habitId: Long) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.completeHabit(userId, habitId, todayDate)
                triggerSync()
            }
        }
    }

    fun uncompleteHabit(habitId: Long) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.uncompleteHabit(userId, habitId, todayDate)
                triggerSync()
            }
        }
    }

    fun nextHabit() {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.cycleIndex(userId, 1, todayDate)
                triggerSync()
            }
        }
    }

    fun prevHabit() {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId != 0L) {
                repository.cycleIndex(userId, -1, todayDate)
                triggerSync()
            }
        }
    }

    fun setIndex(index: Int) {
        viewModelScope.launch {
            repository.setLoopIndex(index)
            triggerSync()
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

    fun toggleHabitCompletionForDate(habitId: Long, dateStr: String, wasCompleted: Boolean, notes: String? = null) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId == 0L) return@launch
            if (wasCompleted) {
                repository.uncompleteHabit(userId, habitId, dateStr)
            } else {
                repository.completeHabitWithNote(userId, habitId, dateStr, notes)
            }
        }
    }

    fun saveCompletionNote(habitId: Long, dateStr: String, notes: String?) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId == 0L) return@launch
            repository.updateCompletionNotes(habitId, dateStr, notes)
        }
    }

    fun calculateStreaks(dates: List<String>): Pair<Int, Int> {
        if (dates.isEmpty()) return Pair(0, 0)
        return try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val sortedDates = dates.map { java.time.LocalDate.parse(it, formatter) }.distinct().sorted()

            var bestStreak = 0
            var tempStreak = 0
            var lastDate: java.time.LocalDate? = null

            for (date in sortedDates) {
                if (lastDate == null) {
                    tempStreak = 1
                } else {
                    val diff = java.time.temporal.ChronoUnit.DAYS.between(lastDate, date)
                    if (diff == 1L) {
                        tempStreak++
                    } else if (diff > 1L) {
                        if (tempStreak > bestStreak) {
                            bestStreak = tempStreak
                        }
                        tempStreak = 1
                    }
                }
                lastDate = date
            }
            if (tempStreak > bestStreak) {
                bestStreak = tempStreak
            }

            val today = java.time.LocalDate.now()
            val yesterday = today.minusDays(1)
            val hasToday = sortedDates.contains(today)
            val hasYesterday = sortedDates.contains(yesterday)

            val currentStreak = if (hasToday || hasYesterday) {
                var streak = 0
                var checkDate = if (hasToday) today else yesterday
                while (sortedDates.contains(checkDate)) {
                    streak++
                    checkDate = checkDate.minusDays(1)
                }
                streak
            } else {
                0
            }

            Pair(currentStreak, bestStreak)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0, 0)
        }
    }
}

class HabitViewModelFactory(
    private val repository: HabitRepository,
    private val syncManager: SyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repository, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

