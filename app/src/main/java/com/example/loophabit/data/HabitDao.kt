package com.example.loophabit.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY createdAt ASC")
    fun getAllHabits(userId: Long): Flow<List<Habit>>

    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND id NOT IN (SELECT habitId FROM habit_completions WHERE date = :date) ORDER BY createdAt ASC")
    fun getIncompleteHabits(userId: Long, date: String): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND id IN (SELECT habitId FROM habit_completions WHERE date = :date) ORDER BY createdAt ASC")
    fun getCompletedHabits(userId: Long, date: String): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: Habit): Long

    @Delete
    suspend fun deleteHabit(habit: Habit): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: HabitCompletion): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND date = :date)")
    suspend fun isCompletedToday(habitId: Long, date: String): Boolean

    @Query("SELECT date FROM habit_completions WHERE habitId = :habitId ORDER BY date ASC")
    fun getCompletionDates(habitId: Long): Flow<List<String>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCompletion(habitId: Long, date: String): HabitCompletion?

    @Update
    suspend fun updateCompletion(completion: HabitCompletion): Int

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date ASC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions")
    fun getAllCompletions(): Flow<List<HabitCompletion>>

    @Query("SELECT hc.* FROM habit_completions hc INNER JOIN habits h ON hc.habitId = h.id WHERE h.userId = :userId")
    fun getAllCompletionsForUser(userId: Long): Flow<List<HabitCompletion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSession): Long

    @Update
    suspend fun updateFocusSession(session: FocusSession): Int

    @Query("DELETE FROM focus_sessions WHERE id = :sessionId")
    suspend fun deleteFocusSessionById(sessionId: Long): Int

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId AND habitId = :habitId AND details = :details LIMIT 1")
    suspend fun getFocusSessionByDetails(userId: Long, habitId: Long, details: String): FocusSession?

    @Query("SELECT * FROM focus_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllFocusSessions(userId: Long): Flow<List<FocusSession>>

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun clearHabitsForUser(userId: Long): Int

    @Query("DELETE FROM focus_sessions WHERE userId = :userId")
    suspend fun clearFocusSessionsForUser(userId: Long): Int

    @Query("DELETE FROM habit_completions WHERE habitId IN (SELECT id FROM habits WHERE userId = :userId)")
    suspend fun clearCompletionsForUser(userId: Long): Int
}
