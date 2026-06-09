package com.example.loophabit.data

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

fun createDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = NSHomeDirectory() + "/LoopHabit"
    return Room.databaseBuilder<AppDatabase>(name = dbFilePath)
}
