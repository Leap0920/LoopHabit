export type User = {
  id: number;
  username: string;
  email: string;
  password: string;
  securityQuestion: string;
  securityAnswer: string;
};

export type Habit = {
  id: number;
  userId: number;
  title: string;
  colorHex: string;
  createdAt: number;
  targetDaysPerWeek: number;
  isNumerical: boolean;
  numericalGoal: number;
  numericalUnit: string;
  daysOfWeekPattern: string;
};

export type HabitCompletion = {
  id: number;
  habitId: number;
  date: string; // YYYY-MM-DD
  notes: string | null;
  value: number;
};

export type FocusSession = {
  id: number;
  userId: number;
  habitId: number | null;
  durationSeconds: number;
  details: string | null;
  timestamp: number;
};

export type TodoItem = {
  id: number;
  userId: number;
  title: string;
  notes: string | null;
  isCompleted: boolean;
  createdAt: number;
  completedAt: number | null;
  sortOrder: number;
};

export type FocusMode = 'TIMER' | 'STOPWATCH';

export type FocusUiState = {
  mode: FocusMode;
  habitId: number | null;
  initialDurationMinutes: number;
  isRunning: boolean;
  isPaused: boolean;
  secondsLeft: number;
  secondsElapsed: number;
};
