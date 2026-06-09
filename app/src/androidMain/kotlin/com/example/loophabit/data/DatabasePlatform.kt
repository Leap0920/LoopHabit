package com.example.loophabit.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            DELETE FROM habit_completions 
            WHERE rowid NOT IN (
                SELECT MIN(rowid) FROM habit_completions GROUP BY habitId, date
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_completions_habitId_date ON habit_completions (habitId, date)")
    }
}

fun createDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = context.getDatabasePath("LoopHabit")
    return Room.databaseBuilder<AppDatabase>(context, dbFile.absolutePath)
        .addMigrations(MIGRATION_4_5)
}
