package com.example.loophabit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loophabit.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * iOS implementation of LoopHabitViewModel.
 * Uses commonMain data layer directly (no Android Context needed).
 */
class IOSHabitViewModel(
    private val habitDao: HabitDao,
    private val userDao: UserDao,
    private val loopPreferences: LoopPreferences
) : ViewModel(), LoopHabitViewModel {

    private val repository = HabitRepository(
        habitDao = habitDao,
        userDao = userDao,
        loopPreferences = loopPreferences,
        syncManager = null
    )

    // User state
    override val currentUserId: StateFlow<Long> = loopPreferences.currentUserIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    override val todayDate: String
        get() = todayLocalDate().toString()

    // Habits
    override val allHabits: StateFlow<List<Habit>> = currentUserId.flatMapLatest { userId ->
        if (userId > 0) habitDao.getAllHabits(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val incompleteHabits: StateFlow<List<Habit>> = combine(currentUserId, allHabits) { userId, habits ->
        if (userId > 0) {
            // Filter to only habits scheduled for today
            val todayDayOfWeek = getDayOfWeekNumber(todayLocalDate())
            habits.filter { habit ->
                val requiredDays = habit.daysOfWeekPattern.split(",").map { it.trim().toInt() }.toSet()
                todayDayOfWeek in requiredDays
            }
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val completedHabits: StateFlow<List<Habit>> = combine(currentUserId, allHabits) { userId, habits ->
        if (userId > 0) {
            val todayDayOfWeek = getDayOfWeekNumber(todayLocalDate())
            habits.filter { habit ->
                val requiredDays = habit.daysOfWeekPattern.split(",").map { it.trim().toInt() }.toSet()
                todayDayOfWeek !in requiredDays
            }
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val loopIndex: StateFlow<Int> = loopPreferences.loopIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    override val currentHabit: StateFlow<Habit?> = combine(allHabits, loopIndex) { habits, index ->
        if (habits.isNotEmpty()) {
            val safeIndex = index.coerceIn(0, habits.lastIndex)
            habits[safeIndex]
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Completions
    override val allCompletions: StateFlow<List<HabitCompletion>> = currentUserId.flatMapLatest { userId ->
        if (userId > 0) habitDao.getAllCompletionsForUser(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Focus sessions
    override val allFocusSessions: StateFlow<List<FocusSession>> = currentUserId.flatMapLatest { userId ->
        if (userId > 0) habitDao.getAllFocusSessions(userId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync state (no-op on iOS for now)
    override val syncState: StateFlow<SyncState> = MutableStateFlow(SyncState.Idle)

    // Focus state
    override val focusHabitId: StateFlow<Long?> = loopPreferences.focusState
        .map { it?.habitId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    override val focusState: StateFlow<LoopPreferences.FocusState> = loopPreferences.focusState
        .map { it ?: LoopPreferences.FocusState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoopPreferences.FocusState())

    // Backup settings
    override val autoBackupInterval: StateFlow<Int> = loopPreferences.autoBackupIntervalFlow
    override val autoBackupUri: StateFlow<String?> = loopPreferences.autoBackupUriFlow

    init {
        // Auto-create default local user if none exists
        viewModelScope.launch {
            currentUserId.collect { userId ->
                if (userId == 0L) {
                    val defaultUser = userDao.getUserByUsername("local_user")
                    if (defaultUser == null) {
                        val newUser = User(
                            username = "local_user",
                            email = "local@loophabit.com",
                            password = "local_password",
                            securityQuestion = "Local?",
                            securityAnswer = "Yes"
                        )
                        val insertedId = userDao.insertUser(newUser)
                        loopPreferences.setCurrentUserId(insertedId)
                    } else {
                        loopPreferences.setCurrentUserId(defaultUser.id)
                    }
                }
            }
        }
    }

    // Habit operations
    override fun addHabit(
        title: String,
        colorHex: String,
        targetDaysPerWeek: Int,
        isNumerical: Boolean,
        numericalGoal: Double,
        numericalUnit: String,
        daysOfWeekPattern: String
    ) {
        viewModelScope.launch {
            val habit = Habit(
                userId = currentUserId.value,
                title = title,
                colorHex = colorHex,
                targetDaysPerWeek = targetDaysPerWeek,
                isNumerical = isNumerical,
                numericalGoal = numericalGoal,
                numericalUnit = numericalUnit,
                daysOfWeekPattern = daysOfWeekPattern
            )
            habitDao.upsertHabit(habit)
        }
    }

    override fun deleteHabit(habit: Habit) {
        viewModelScope.launch { habitDao.deleteHabit(habit) }
    }

    override fun completeHabit(habitId: Long) {
        viewModelScope.launch {
            val completion = HabitCompletion(
                habitId = habitId,
                date = todayDate,
                notes = null,
                value = null
            )
            habitDao.upsertCompletion(completion)
        }
    }

    override fun uncompleteHabit(habitId: Long) {
        viewModelScope.launch { habitDao.deleteCompletion(habitId, todayDate) }
    }

    override fun nextHabit() {
        viewModelScope.launch { repository.cycleIndex(currentUserId.value, 1, todayDate) }
    }

    override fun prevHabit() {
        viewModelScope.launch { repository.cycleIndex(currentUserId.value, -1, todayDate) }
    }

    override fun setIndex(index: Int) {
        viewModelScope.launch { repository.setLoopIndex(index) }
    }

    // Focus
    override fun setFocusHabitId(id: Long?) {
        viewModelScope.launch {
            val current = loopPreferences.focusState.value
            loopPreferences.setFocusState(
                (current ?: LoopPreferences.FocusState()).copy(habitId = id)
            )
        }
    }

    override fun updateFocusState(state: LoopPreferences.FocusState) {
        viewModelScope.launch { loopPreferences.setFocusState(state) }
    }

    override fun logFocusSession(habitId: Long?, durationSeconds: Int, details: String?) {
        viewModelScope.launch {
            repository.logFocusSession(currentUserId.value, habitId, durationSeconds, details)
        }
    }

    // Completion operations
    override fun toggleHabitCompletionForDate(
        habitId: Long,
        dateStr: String,
        wasCompleted: Boolean,
        notes: String?,
        value: Double
    ) {
        viewModelScope.launch {
            if (wasCompleted) {
                habitDao.deleteCompletion(habitId, dateStr)
            } else {
                habitDao.upsertCompletion(
                    HabitCompletion(
                        habitId = habitId,
                        date = dateStr,
                        notes = notes,
                        value = value
                    )
                )
            }
        }
    }

    override fun completeHabitNumerical(habitId: Long, value: Double, notes: String?) {
        viewModelScope.launch {
            habitDao.upsertCompletion(
                HabitCompletion(
                    habitId = habitId,
                    date = todayDate,
                    notes = notes,
                    value = value
                )
            )
        }
    }

    override fun saveCompletionNote(habitId: Long, dateStr: String, notes: String?, value: Double) {
        viewModelScope.launch {
            val existing = habitDao.getCompletion(habitId, dateStr).first()
            if (existing != null) {
                habitDao.updateCompletion(existing.copy(notes = notes, value = value))
            }
        }
    }

    override fun getCompletionDates(habitId: Long): Flow<List<String>> {
        return habitDao.getCompletionDates(habitId)
    }

    override fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return habitDao.getCompletionsForHabit(habitId)
    }

    // Streak calculation
    override fun calculateStreaks(dates: List<String>, daysOfWeekPattern: String): Pair<Int, Int> {
        if (dates.isEmpty()) return 0 to 0

        val sortedDates = dates.sortedDescending()
        val today = todayLocalDate()
        val requiredDays = daysOfWeekPattern.split(",").map { it.trim().toInt() }.toSet()

        // Current streak
        var currentStreak = 0
        var checkDate = today
        while (true) {
            val dayOfWeek = getDayOfWeekNumber(checkDate)
            if (dayOfWeek !in requiredDays) {
                checkDate = checkDate.minus(kotlinx.datetime.DateTimeUnit.Day)
                continue
            }
            if (sortedDates.contains(checkDate.toString())) {
                currentStreak++
                checkDate = checkDate.minus(kotlinx.datetime.DateTimeUnit.Day)
            } else {
                break
            }
        }

        // Best streak
        var bestStreak = 0
        var tempStreak = 0
        val allDates = sortedDates.map { kotlinx.datetime.LocalDate.parse(it) }.sorted()

        for (date in allDates) {
            val dayOfWeek = getDayOfWeekNumber(date)
            if (dayOfWeek !in requiredDays) continue

            if (tempStreak == 0) {
                tempStreak = 1
            } else {
                val prevDate = date.minus(kotlinx.datetime.DateTimeUnit.Day)
                if (allDates.contains(prevDate) && getDayOfWeekNumber(prevDate) in requiredDays) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
            }
            if (tempStreak > bestStreak) bestStreak = tempStreak
        }

        return currentStreak to bestStreak
    }

    // Authentication
    override fun login(usernameOrEmail: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                var user = userDao.getUserByEmail(usernameOrEmail)
                if (user == null) {
                    user = userDao.getUserByUsername(usernameOrEmail)
                }
                if (user == null) {
                    onError("User not found")
                    return@launch
                }
                if (user.password != password) {
                    onError("Invalid password")
                    return@launch
                }
                loopPreferences.setCurrentUserId(user.id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Login failed")
            }
        }
    }

    override fun register(
        username: String,
        email: String,
        password: String,
        securityQuestion: String,
        securityAnswer: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existingUser = userDao.getUserByEmail(email)
                if (existingUser != null) {
                    onError("Email already registered")
                    return@launch
                }
                val user = User(
                    username = username,
                    email = email,
                    password = password,
                    securityQuestion = securityQuestion,
                    securityAnswer = securityAnswer
                )
                val userId = userDao.insertUser(user)
                loopPreferences.setCurrentUserId(userId)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Registration failed")
            }
        }
    }

    override fun getSecurityQuestion(email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = userDao.getUserByEmail(email)
            if (user != null) {
                onSuccess(user.securityQuestion)
            } else {
                onError("Email not found")
            }
        }
    }

    override fun resetPassword(email: String, answer: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = userDao.getUserByEmail(email)
                if (user == null) {
                    onError("User not found")
                    return@launch
                }
                if (user.securityAnswer != answer) {
                    onError("Incorrect security answer")
                    return@launch
                }
                userDao.updateUser(user.copy(password = newPassword))
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Password reset failed")
            }
        }
    }

    override fun logout() {
        viewModelScope.launch { loopPreferences.setCurrentUserId(0L) }
    }

    // Backup
    override fun setAutoBackupInterval(intervalHours: Int) {
        viewModelScope.launch { loopPreferences.setAutoBackupInterval(intervalHours) }
    }

    override fun setAutoBackupUri(uriString: String?) {
        viewModelScope.launch { loopPreferences.setAutoBackupUri(uriString) }
    }

    override fun exportData() {
        // iOS export — would use share sheet in production
    }

    override fun importData(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val root = Json.parseToJsonElement(jsonString) as JsonObject

                val habitsStr = root["habits"]?.toString() ?: "[]"
                val habits = Json.decodeFromString<List<Habit>>(habitsStr)
                habits.forEach { habitDao.upsertHabit(it) }

                val completionsStr = root["completions"]?.toString() ?: "[]"
                val completions = Json.decodeFromString<List<HabitCompletion>>(completionsStr)
                completions.forEach { habitDao.upsertCompletion(it) }

                val focusStr = root["focusSessions"]?.toString() ?: "[]"
                val focusSessions = Json.decodeFromString<List<FocusSession>>(focusStr)
                focusSessions.forEach { habitDao.insertFocusSession(it) }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Import failed")
            }
        }
    }

    override fun resetAllData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val userId = currentUserId.value
            if (userId > 0) {
                habitDao.clearHabitsForUser(userId)
                habitDao.clearCompletionsForUser(userId)
                habitDao.clearFocusSessionsForUser(userId)
            }
            onSuccess()
        }
    }
}
