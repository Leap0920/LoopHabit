package com.example.loophabit.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items WHERE userId = :userId ORDER BY isCompleted ASC, createdAt DESC")
    fun getTodosForUser(userId: Long): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem): Int

    @Delete
    suspend fun deleteTodo(todo: TodoItem): Int

    @Query("DELETE FROM todo_items WHERE userId = :userId")
    suspend fun clearTodosForUser(userId: Long): Int
}
