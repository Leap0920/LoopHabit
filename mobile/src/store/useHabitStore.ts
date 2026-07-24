import { create } from 'zustand';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as repo from '../db/repository';
import type { FocusSession, FocusUiState, Habit, HabitCompletion, TodoItem } from '../types/models';
import { todayYmd } from '../utils/date';
import { HABIT_PALETTE } from '../theme/colors';

const USER_KEY = 'loophabit.currentUserId';

type HabitStore = {
  ready: boolean;
  userId: number;
  selectedDate: string;
  habits: Habit[];
  completionsToday: HabitCompletion[];
  allCompletionDates: string[];
  allCompletions: HabitCompletion[];
  todos: TodoItem[];
  focusSessions: FocusSession[];
  loopIndex: number;
  focus: FocusUiState;
  init: () => Promise<void>;
  refresh: () => Promise<void>;
  setSelectedDate: (date: string) => Promise<void>;
  addHabit: (title: string, colorHex?: string) => Promise<void>;
  deleteHabit: (id: number) => Promise<void>;
  completeHabit: (habitId: number, value?: number) => Promise<void>;
  uncompleteHabit: (habitId: number) => Promise<void>;
  skipCurrent: () => void;
  setLoopIndex: (i: number) => void;
  addTodo: (title: string) => Promise<void>;
  toggleTodo: (id: number, completed: boolean) => Promise<void>;
  deleteTodo: (id: number) => Promise<void>;
  setFocusMode: (mode: FocusUiState['mode']) => void;
  setFocusHabitId: (habitId: number | null) => void;
  setFocusDuration: (minutes: number) => void;
  startFocus: () => void;
  pauseFocus: () => void;
  resumeFocus: () => void;
  tickFocus: () => void;
  finishFocus: (details?: string) => Promise<void>;
  resetFocus: () => void;
  incompleteHabits: () => Habit[];
  completedHabits: () => Habit[];
  currentHabit: () => Habit | null;
};

const defaultFocus = (): FocusUiState => ({
  mode: 'TIMER',
  habitId: null,
  initialDurationMinutes: 25,
  isRunning: false,
  isPaused: false,
  secondsLeft: 25 * 60,
  secondsElapsed: 0,
});

export const useHabitStore = create<HabitStore>((set, get) => ({
  ready: false,
  userId: 0,
  selectedDate: todayYmd(),
  habits: [],
  completionsToday: [],
  allCompletionDates: [],
  allCompletions: [],
  todos: [],
  focusSessions: [],
  loopIndex: 0,
  focus: defaultFocus(),

  init: async () => {
    let userId = Number((await AsyncStorage.getItem(USER_KEY)) || '0');
    if (!userId) {
      userId = await repo.ensureLocalUser();
      await AsyncStorage.setItem(USER_KEY, String(userId));
    } else {
      const user = await repo.getUserById(userId);
      if (!user) {
        userId = await repo.ensureLocalUser();
        await AsyncStorage.setItem(USER_KEY, String(userId));
      }
    }
    set({ userId });
    await get().refresh();
    set({ ready: true });
  },

  refresh: async () => {
    const { userId, selectedDate } = get();
    if (!userId) return;
    const [habits, completionsToday, allCompletionDates, allCompletions, todos, focusSessions] =
      await Promise.all([
        repo.getHabits(userId),
        repo.getCompletionsForDate(userId, selectedDate),
        repo.getAllCompletionDates(userId),
        repo.getAllCompletions(userId),
        repo.getTodos(userId),
        repo.getFocusSessions(userId),
      ]);
    set({ habits, completionsToday, allCompletionDates, allCompletions, todos, focusSessions });
  },

  setSelectedDate: async (date) => {
    set({ selectedDate: date, loopIndex: 0 });
    await get().refresh();
  },

  addHabit: async (title, colorHex) => {
    const { userId, habits } = get();
    const color = colorHex ?? HABIT_PALETTE[habits.length % HABIT_PALETTE.length];
    await repo.insertHabit({
      userId,
      title: title.trim(),
      colorHex: color,
      targetDaysPerWeek: 7,
      isNumerical: false,
      numericalGoal: 0,
      numericalUnit: '',
      daysOfWeekPattern: '1111111',
    });
    await get().refresh();
  },

  deleteHabit: async (id) => {
    await repo.deleteHabit(id);
    await get().refresh();
  },

  completeHabit: async (habitId, value = 0) => {
    const { selectedDate } = get();
    await repo.markHabitComplete(habitId, selectedDate, value);
    set((s) => ({ loopIndex: Math.min(s.loopIndex, Math.max(0, s.habits.length - 2)) }));
    await get().refresh();
  },

  uncompleteHabit: async (habitId) => {
    const { selectedDate } = get();
    await repo.unmarkHabitComplete(habitId, selectedDate);
    await get().refresh();
  },

  skipCurrent: () => {
    const incomplete = get().incompleteHabits();
    if (incomplete.length <= 1) return;
    set((s) => ({ loopIndex: (s.loopIndex + 1) % incomplete.length }));
  },

  setLoopIndex: (i) => set({ loopIndex: i }),

  addTodo: async (title) => {
    const { userId } = get();
    if (!title.trim()) return;
    await repo.insertTodo({ userId, title: title.trim(), notes: null });
    await get().refresh();
  },

  toggleTodo: async (id, completed) => {
    await repo.setTodoCompleted(id, completed);
    await get().refresh();
  },

  deleteTodo: async (id) => {
    await repo.deleteTodo(id);
    await get().refresh();
  },

  setFocusMode: (mode) =>
    set((s) => ({
      focus: {
        ...s.focus,
        mode,
        isRunning: false,
        isPaused: false,
        secondsLeft: s.focus.initialDurationMinutes * 60,
        secondsElapsed: 0,
      },
    })),

  setFocusHabitId: (habitId) => set((s) => ({ focus: { ...s.focus, habitId } })),

  setFocusDuration: (minutes) =>
    set((s) => ({
      focus: {
        ...s.focus,
        initialDurationMinutes: minutes,
        secondsLeft: minutes * 60,
        isRunning: false,
        isPaused: false,
        secondsElapsed: 0,
      },
    })),

  startFocus: () =>
    set((s) => ({
      focus: {
        ...s.focus,
        isRunning: true,
        isPaused: false,
        secondsLeft: s.focus.initialDurationMinutes * 60,
        secondsElapsed: 0,
      },
    })),

  pauseFocus: () => set((s) => ({ focus: { ...s.focus, isRunning: false, isPaused: true } })),

  resumeFocus: () => set((s) => ({ focus: { ...s.focus, isRunning: true, isPaused: false } })),

  tickFocus: () => {
    const { focus } = get();
    if (!focus.isRunning) return;
    if (focus.mode === 'TIMER') {
      const next = focus.secondsLeft - 1;
      if (next <= 0) {
        set({ focus: { ...focus, secondsLeft: 0, isRunning: false, isPaused: false } });
        void get().finishFocus('Timer completed');
      } else {
        set({ focus: { ...focus, secondsLeft: next } });
      }
    } else {
      set({ focus: { ...focus, secondsElapsed: focus.secondsElapsed + 1 } });
    }
  },

  finishFocus: async (details) => {
    const { userId, focus } = get();
    const duration =
      focus.mode === 'TIMER'
        ? focus.initialDurationMinutes * 60 - focus.secondsLeft
        : focus.secondsElapsed;
    if (duration > 0) {
      await repo.insertFocusSession({
        userId,
        habitId: focus.habitId,
        durationSeconds: duration,
        details: details ?? null,
      });
    }
    set({ focus: defaultFocus() });
    await get().refresh();
  },

  resetFocus: () => set({ focus: defaultFocus() }),

  incompleteHabits: () => {
    const { habits, completionsToday } = get();
    const done = new Set(completionsToday.map((c) => c.habitId));
    return habits.filter((h) => !done.has(h.id));
  },

  completedHabits: () => {
    const { habits, completionsToday } = get();
    const done = new Set(completionsToday.map((c) => c.habitId));
    return habits.filter((h) => done.has(h.id));
  },

  currentHabit: () => {
    const incomplete = get().incompleteHabits();
    if (!incomplete.length) return null;
    const idx = get().loopIndex % incomplete.length;
    return incomplete[idx] ?? incomplete[0];
  },
}));
