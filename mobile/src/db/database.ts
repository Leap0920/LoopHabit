import * as SQLite from 'expo-sqlite';

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

export function getDb(): Promise<SQLite.SQLiteDatabase> {
  if (!dbPromise) {
    dbPromise = openAndMigrate();
  }
  return dbPromise;
}

async function openAndMigrate(): Promise<SQLite.SQLiteDatabase> {
  const db = await SQLite.openDatabaseAsync('LoopHabit.db');

  await db.execAsync(`
    PRAGMA journal_mode = WAL;
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      username TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE,
      password TEXT NOT NULL,
      securityQuestion TEXT NOT NULL,
      securityAnswer TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS habits (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      userId INTEGER NOT NULL,
      title TEXT NOT NULL,
      colorHex TEXT NOT NULL,
      createdAt INTEGER NOT NULL,
      targetDaysPerWeek INTEGER NOT NULL DEFAULT 7,
      isNumerical INTEGER NOT NULL DEFAULT 0,
      numericalGoal REAL NOT NULL DEFAULT 0,
      numericalUnit TEXT NOT NULL DEFAULT '',
      daysOfWeekPattern TEXT NOT NULL DEFAULT '1111111',
      FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS habit_completions (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      habitId INTEGER NOT NULL,
      date TEXT NOT NULL,
      notes TEXT,
      value REAL NOT NULL DEFAULT 0,
      FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE
    );

    CREATE UNIQUE INDEX IF NOT EXISTS index_habit_completions_habitId_date
      ON habit_completions (habitId, date);

    CREATE TABLE IF NOT EXISTS focus_sessions (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      userId INTEGER NOT NULL,
      habitId INTEGER,
      durationSeconds INTEGER NOT NULL,
      details TEXT,
      timestamp INTEGER NOT NULL,
      FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
      FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE SET NULL
    );

    CREATE TABLE IF NOT EXISTS todo_items (
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      userId INTEGER NOT NULL,
      title TEXT NOT NULL,
      notes TEXT,
      isCompleted INTEGER NOT NULL DEFAULT 0,
      createdAt INTEGER NOT NULL,
      completedAt INTEGER,
      sortOrder INTEGER NOT NULL DEFAULT 0,
      FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
    );

    CREATE INDEX IF NOT EXISTS index_habits_userId ON habits (userId);
    CREATE INDEX IF NOT EXISTS index_todo_items_userId ON todo_items (userId);
  `);

  return db;
}
