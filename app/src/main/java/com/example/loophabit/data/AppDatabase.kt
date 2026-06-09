package com.example.loophabit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, Habit::class, HabitCompletion::class, FocusSession::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration 4 → 5: Add unique index on habit_completions(habitId, date)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove duplicate completions before adding unique constraint
                db.execSQL("""
                    DELETE FROM habit_completions 
                    WHERE rowid NOT IN (
                        SELECT MIN(rowid) FROM habit_completions GROUP BY habitId, date
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_completions_habitId_date ON habit_completions (habitId, date)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "LoopHabit"
                )
                .addMigrations(MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
