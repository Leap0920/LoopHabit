package com.example.loophabit.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HabitRepository(
    private val habitDao: HabitDao,
    private val loopPreferences: LoopPreferences
) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val loopIndexFlow: Flow<Int> = loopPreferences.loopIndexFlow

    fun getIncompleteHabitsOfToday(date: String): Flow<List<Habit>> =
        habitDao.getIncompleteHabits(date)

    fun getCompletedHabitsOfToday(date: String): Flow<List<Habit>> =
        habitDao.getCompletedHabits(date)

    suspend fun addHabit(title: String, colorHex: String, date: String) {
        val habit = Habit(title = title, colorHex = colorHex)
        habitDao.insertHabit(habit)
        validateIndex(date)
    }

    suspend fun deleteHabit(habit: Habit, date: String) {
        habitDao.deleteHabit(habit)
        validateIndex(date)
    }

    suspend fun completeHabit(habitId: Long, date: String) {
        habitDao.insertCompletion(HabitCompletion(habitId = habitId, date = date))
        validateIndex(date)
    }

    suspend fun uncompleteHabit(habitId: Long, date: String) {
        habitDao.deleteCompletion(habitId, date)
        validateIndex(date)
    }

    suspend fun cycleIndex(direction: Int, date: String) {
        val incomplete = habitDao.getIncompleteHabits(date).first()
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

    suspend fun validateIndex(date: String) {
        val incomplete = habitDao.getIncompleteHabits(date).first()
        val size = incomplete.size
        val currentIndex = loopPreferences.loopIndexFlow.first()
        if (size == 0) {
            loopPreferences.setLoopIndex(0)
        } else if (currentIndex < 0 || currentIndex >= size) {
            loopPreferences.setLoopIndex(0)
        }
    }
}
