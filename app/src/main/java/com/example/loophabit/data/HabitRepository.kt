package com.example.loophabit.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HabitRepository(
    private val habitDao: HabitDao,
    private val userDao: UserDao,
    private val loopPreferences: LoopPreferences
) {
    val loopIndexFlow: Flow<Int> = loopPreferences.loopIndexFlow
    val currentUserIdFlow: Flow<Long> = loopPreferences.currentUserIdFlow

    fun getAllHabits(userId: Long): Flow<List<Habit>> = habitDao.getAllHabits(userId)

    fun getIncompleteHabitsOfToday(userId: Long, date: String): Flow<List<Habit>> =
        habitDao.getIncompleteHabits(userId, date)

    fun getCompletedHabitsOfToday(userId: Long, date: String): Flow<List<Habit>> =
        habitDao.getCompletedHabits(userId, date)

    suspend fun addHabit(userId: Long, title: String, colorHex: String, targetDaysPerWeek: Int, date: String) {
        val habit = Habit(userId = userId, title = title, colorHex = colorHex, targetDaysPerWeek = targetDaysPerWeek)
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

    suspend fun completeHabitWithNote(userId: Long, habitId: Long, date: String, notes: String?) {
        val existing = habitDao.getCompletion(habitId, date)
        if (existing != null) {
            habitDao.updateCompletion(existing.copy(notes = notes))
        } else {
            habitDao.insertCompletion(HabitCompletion(habitId = habitId, date = date, notes = notes))
        }
        validateIndex(userId, date)
    }

    suspend fun updateCompletionNotes(habitId: Long, date: String, notes: String?) {
        val completion = habitDao.getCompletion(habitId, date)
        if (completion != null) {
            habitDao.updateCompletion(completion.copy(notes = notes))
        }
    }
}

