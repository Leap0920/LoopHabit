package com.example.loophabit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)
