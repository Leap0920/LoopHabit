package com.example.loophabit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["habitId"])]
)
@Serializable
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val habitId: Long?,
    val durationSeconds: Int,
    val details: String?,
    val timestamp: Long = 0
)
