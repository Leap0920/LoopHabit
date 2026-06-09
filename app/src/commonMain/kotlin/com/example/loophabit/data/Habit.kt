package com.example.loophabit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
@Serializable
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val colorHex: String,
    val createdAt: Long = 0,
    val targetDaysPerWeek: Int = 7,
    val isNumerical: Boolean = false,
    val numericalGoal: Double = 0.0,
    val numericalUnit: String = "",
    val daysOfWeekPattern: String = "1111111"
)
