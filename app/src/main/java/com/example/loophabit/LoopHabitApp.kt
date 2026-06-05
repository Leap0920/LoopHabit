package com.example.loophabit

import android.app.Application
import com.example.loophabit.data.AppDatabase
import com.example.loophabit.data.HabitRepository
import com.example.loophabit.data.LoopPreferences

class LoopHabitApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val preferences by lazy { LoopPreferences(this) }
    val repository by lazy { HabitRepository(database.habitDao(), preferences) }
}
