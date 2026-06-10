package com.example.loophabit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, Habit::class, HabitCompletion::class, FocusSession::class, TodoItem::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun todoDao(): TodoDao
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todo_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        notes TEXT,
                        isCompleted INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_userId ON todo_items (userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todo_items_isCompleted ON todo_items (isCompleted)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "LoopHabit"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
