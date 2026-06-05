package com.example.loophabit.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id NOT IN (SELECT habitId FROM habit_completions WHERE date = :date) ORDER BY createdAt ASC")
    fun getIncompleteHabits(date: String): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id IN (SELECT habitId FROM habit_completions WHERE date = :date) ORDER BY createdAt ASC")
    fun getCompletedHabits(date: String): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Delete
    suspend fun deleteHabit(habit: Habit): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND date = :date)")
    suspend fun isCompletedToday(habitId: Long, date: String): Boolean
}
