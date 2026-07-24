import { getDb } from './database';
import type { FocusSession, Habit, HabitCompletion, TodoItem, User } from '../types/models';

function rowToHabit(r: any): Habit {
  return {
    id: r.id,
    userId: r.userId,
    title: r.title,
    colorHex: r.colorHex,
    createdAt: r.createdAt,
    targetDaysPerWeek: r.targetDaysPerWeek,
    isNumerical: !!r.isNumerical,
    numericalGoal: r.numericalGoal,
    numericalUnit: r.numericalUnit ?? '',
    daysOfWeekPattern: r.daysOfWeekPattern ?? '1111111',
  };
}

function rowToTodo(r: any): TodoItem {
  return {
    id: r.id,
    userId: r.userId,
    title: r.title,
    notes: r.notes ?? null,
    isCompleted: !!r.isCompleted,
    createdAt: r.createdAt,
    completedAt: r.completedAt ?? null,
    sortOrder: r.sortOrder ?? 0,
  };
}

export async function getUserByUsername(username: string): Promise<User | null> {
  const db = await getDb();
  return (await db.getFirstAsync<User>('SELECT * FROM users WHERE username = ?', username)) ?? null;
}

export async function getUserById(id: number): Promise<User | null> {
  const db = await getDb();
  return (await db.getFirstAsync<User>('SELECT * FROM users WHERE id = ?', id)) ?? null;
}

export async function registerUser(user: Omit<User, 'id'>): Promise<number> {
  const db = await getDb();
  const result = await db.runAsync(
    `INSERT INTO users (username, email, password, securityQuestion, securityAnswer)
     VALUES (?, ?, ?, ?, ?)`,
    user.username,
    user.email,
    user.password,
    user.securityQuestion,
    user.securityAnswer
  );
  return result.lastInsertRowId;
}

export async function ensureLocalUser(): Promise<number> {
  const existing = await getUserByUsername('local_user');
  if (existing) return existing.id;
  return registerUser({
    username: 'local_user',
    email: 'local@loophabit.com',
    password: 'local_password',
    securityQuestion: 'Local?',
    securityAnswer: 'Yes',
  });
}

export async function getHabits(userId: number): Promise<Habit[]> {
  const db = await getDb();
  const rows = await db.getAllAsync('SELECT * FROM habits WHERE userId = ? ORDER BY createdAt ASC', userId);
  return rows.map(rowToHabit);
}

export async function insertHabit(
  habit: Omit<Habit, 'id' | 'createdAt'> & { createdAt?: number }
): Promise<number> {
  const db = await getDb();
  const createdAt = habit.createdAt ?? Date.now();
  const result = await db.runAsync(
    `INSERT INTO habits
      (userId, title, colorHex, createdAt, targetDaysPerWeek, isNumerical, numericalGoal, numericalUnit, daysOfWeekPattern)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    habit.userId,
    habit.title,
    habit.colorHex,
    createdAt,
    habit.targetDaysPerWeek,
    habit.isNumerical ? 1 : 0,
    habit.numericalGoal,
    habit.numericalUnit,
    habit.daysOfWeekPattern
  );
  return result.lastInsertRowId;
}

export async function updateHabit(habit: Habit): Promise<void> {
  const db = await getDb();
  await db.runAsync(
    `UPDATE habits SET title = ?, colorHex = ?, targetDaysPerWeek = ?, isNumerical = ?,
      numericalGoal = ?, numericalUnit = ?, daysOfWeekPattern = ? WHERE id = ?`,
    habit.title,
    habit.colorHex,
    habit.targetDaysPerWeek,
    habit.isNumerical ? 1 : 0,
    habit.numericalGoal,
    habit.numericalUnit,
    habit.daysOfWeekPattern,
    habit.id
  );
}

export async function deleteHabit(id: number): Promise<void> {
  const db = await getDb();
  await db.runAsync('DELETE FROM habits WHERE id = ?', id);
}

export async function getCompletionsForDate(userId: number, date: string): Promise<HabitCompletion[]> {
  const db = await getDb();
  const rows = await db.getAllAsync<HabitCompletion>(
    `SELECT hc.* FROM habit_completions hc
     INNER JOIN habits h ON h.id = hc.habitId
     WHERE h.userId = ? AND hc.date = ?`,
    userId,
    date
  );
  return rows;
}

export async function getAllCompletionDates(userId: number): Promise<string[]> {
  const db = await getDb();
  const rows = await db.getAllAsync<{ date: string }>(
    `SELECT DISTINCT hc.date AS date FROM habit_completions hc
     INNER JOIN habits h ON h.id = hc.habitId
     WHERE h.userId = ?
     ORDER BY hc.date DESC`,
    userId
  );
  return rows.map((r) => r.date);
}

export async function getAllCompletions(userId: number): Promise<HabitCompletion[]> {
  const db = await getDb();
  return db.getAllAsync<HabitCompletion>(
    `SELECT hc.* FROM habit_completions hc
     INNER JOIN habits h ON h.id = hc.habitId
     WHERE h.userId = ?`,
    userId
  );
}

export async function markHabitComplete(
  habitId: number,
  date: string,
  value = 0,
  notes: string | null = null
): Promise<void> {
  const db = await getDb();
  await db.runAsync(
    `INSERT INTO habit_completions (habitId, date, notes, value)
     VALUES (?, ?, ?, ?)
     ON CONFLICT(habitId, date) DO UPDATE SET value = excluded.value, notes = excluded.notes`,
    habitId,
    date,
    notes,
    value
  );
}

export async function unmarkHabitComplete(habitId: number, date: string): Promise<void> {
  const db = await getDb();
  await db.runAsync('DELETE FROM habit_completions WHERE habitId = ? AND date = ?', habitId, date);
}

export async function getFocusSessions(userId: number): Promise<FocusSession[]> {
  const db = await getDb();
  return db.getAllAsync<FocusSession>(
    'SELECT * FROM focus_sessions WHERE userId = ? ORDER BY timestamp DESC',
    userId
  );
}

export async function insertFocusSession(
  session: Omit<FocusSession, 'id' | 'timestamp'> & { timestamp?: number }
): Promise<number> {
  const db = await getDb();
  const result = await db.runAsync(
    `INSERT INTO focus_sessions (userId, habitId, durationSeconds, details, timestamp)
     VALUES (?, ?, ?, ?, ?)`,
    session.userId,
    session.habitId,
    session.durationSeconds,
    session.details,
    session.timestamp ?? Date.now()
  );
  return result.lastInsertRowId;
}

export async function getTodos(userId: number): Promise<TodoItem[]> {
  const db = await getDb();
  const rows = await db.getAllAsync(
    'SELECT * FROM todo_items WHERE userId = ? ORDER BY isCompleted ASC, sortOrder ASC, createdAt DESC',
    userId
  );
  return rows.map(rowToTodo);
}

export async function insertTodo(
  todo: Omit<TodoItem, 'id' | 'createdAt' | 'isCompleted' | 'completedAt' | 'sortOrder'> & {
    sortOrder?: number;
  }
): Promise<number> {
  const db = await getDb();
  const result = await db.runAsync(
    `INSERT INTO todo_items (userId, title, notes, isCompleted, createdAt, completedAt, sortOrder)
     VALUES (?, ?, ?, 0, ?, NULL, ?)`,
    todo.userId,
    todo.title,
    todo.notes,
    Date.now(),
    todo.sortOrder ?? 0
  );
  return result.lastInsertRowId;
}

export async function setTodoCompleted(id: number, completed: boolean): Promise<void> {
  const db = await getDb();
  await db.runAsync(
    'UPDATE todo_items SET isCompleted = ?, completedAt = ? WHERE id = ?',
    completed ? 1 : 0,
    completed ? Date.now() : null,
    id
  );
}

export async function deleteTodo(id: number): Promise<void> {
  const db = await getDb();
  await db.runAsync('DELETE FROM todo_items WHERE id = ?', id);
}

export async function updateTodoTitle(id: number, title: string, notes: string | null): Promise<void> {
  const db = await getDb();
  await db.runAsync('UPDATE todo_items SET title = ?, notes = ? WHERE id = ?', title, notes, id);
}
