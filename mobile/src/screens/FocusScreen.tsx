import React, { useEffect } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Pause, Play, RotateCcw } from 'lucide-react-native';
import { useHabitStore } from '../store/useHabitStore';
import { useTheme } from '../theme/ThemeContext';
import { formatTimer } from '../utils/date';

const PRESETS = [1, 15, 25, 45, 60];

export function FocusScreen() {
  const { colors } = useTheme();
  const focus = useHabitStore((s) => s.focus);
  const habits = useHabitStore((s) => s.habits);
  const incompleteHabits = useHabitStore((s) => s.incompleteHabits);
  const setFocusMode = useHabitStore((s) => s.setFocusMode);
  const setFocusHabitId = useHabitStore((s) => s.setFocusHabitId);
  const setFocusDuration = useHabitStore((s) => s.setFocusDuration);
  const startFocus = useHabitStore((s) => s.startFocus);
  const pauseFocus = useHabitStore((s) => s.pauseFocus);
  const resumeFocus = useHabitStore((s) => s.resumeFocus);
  const tickFocus = useHabitStore((s) => s.tickFocus);
  const finishFocus = useHabitStore((s) => s.finishFocus);
  const resetFocus = useHabitStore((s) => s.resetFocus);

  const incomplete = incompleteHabits();
  const selected =
    habits.find((h) => h.id === focus.habitId) ??
    incomplete[0] ??
    habits[0] ??
    null;

  useEffect(() => {
    if (selected && focus.habitId !== selected.id) {
      setFocusHabitId(selected.id);
    }
  }, [selected?.id]);

  useEffect(() => {
    if (!focus.isRunning) return;
    const id = setInterval(() => tickFocus(), 1000);
    return () => clearInterval(id);
  }, [focus.isRunning, tickFocus]);

  const displaySeconds =
    focus.mode === 'TIMER' ? focus.secondsLeft : focus.secondsElapsed;
  const total = focus.initialDurationMinutes * 60;
  const progress =
    focus.mode === 'TIMER'
      ? 1 - focus.secondsLeft / Math.max(total, 1)
      : Math.min(focus.secondsElapsed / Math.max(total, 1), 1);

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.heading, { color: colors.onBackground }]}>Focus</Text>
      <Text style={[styles.sub, { color: colors.onSurfaceVariant }]}>
        {selected ? selected.title : 'No habit selected'}
      </Text>

      <View style={styles.modeRow}>
        {(['TIMER', 'STOPWATCH'] as const).map((mode) => {
          const active = focus.mode === mode;
          return (
            <Pressable
              key={mode}
              disabled={focus.isRunning}
              onPress={() => setFocusMode(mode)}
              style={[
                styles.modePill,
                {
                  backgroundColor: active ? colors.primary : colors.surfaceVariant,
                },
              ]}
            >
              <Text
                style={{
                  color: active ? colors.onPrimary : colors.onSurfaceVariant,
                  fontWeight: '700',
                  fontSize: 13,
                }}
              >
                {mode === 'TIMER' ? 'Countdown' : 'Stopwatch'}
              </Text>
            </Pressable>
          );
        })}
      </View>

      <View style={[styles.ring, { borderColor: colors.surfaceVariant }]}>
        <View
          style={[
            styles.ringProgress,
            {
              borderColor: colors.primary,
              opacity: 0.15 + progress * 0.85,
              transform: [{ scale: 0.92 + progress * 0.08 }],
            },
          ]}
        />
        <Text style={[styles.timer, { color: colors.onBackground }]}>
          {formatTimer(displaySeconds)}
        </Text>
        <Text style={{ color: colors.onSurfaceVariant, fontWeight: '600' }}>
          {focus.mode === 'TIMER' ? 'remaining' : 'elapsed'}
        </Text>
      </View>

      {focus.mode === 'TIMER' && !focus.isRunning && (
        <View style={styles.presets}>
          {PRESETS.map((m) => {
            const active = focus.initialDurationMinutes === m;
            return (
              <Pressable
                key={m}
                onPress={() => setFocusDuration(m)}
                style={[
                  styles.preset,
                  {
                    backgroundColor: active ? colors.primaryContainer : colors.card,
                    borderColor: active ? colors.primary : colors.border,
                  },
                ]}
              >
                <Text
                  style={{
                    color: active ? colors.onPrimaryContainer : colors.onSurface,
                    fontWeight: '700',
                  }}
                >
                  {m}m
                </Text>
              </Pressable>
            );
          })}
        </View>
      )}

      {!!habits.length && (
        <View style={styles.habitPick}>
          {habits.slice(0, 6).map((h) => {
            const active = selected?.id === h.id;
            return (
              <Pressable
                key={h.id}
                disabled={focus.isRunning}
                onPress={() => setFocusHabitId(h.id)}
                style={[
                  styles.habitChip,
                  {
                    backgroundColor: active ? h.colorHex : colors.card,
                    borderColor: h.colorHex,
                  },
                ]}
              >
                <Text
                  style={{
                    color: active ? '#fff' : colors.onSurface,
                    fontWeight: '600',
                    fontSize: 12,
                  }}
                  numberOfLines={1}
                >
                  {h.title}
                </Text>
              </Pressable>
            );
          })}
        </View>
      )}

      <View style={styles.controls}>
        {!focus.isRunning && !focus.isPaused && (
          <Pressable
            onPress={startFocus}
            style={[styles.mainBtn, { backgroundColor: colors.primary }]}
          >
            <Play color={colors.onPrimary} size={22} fill={colors.onPrimary} />
            <Text style={[styles.mainBtnText, { color: colors.onPrimary }]}>Start</Text>
          </Pressable>
        )}
        {focus.isRunning && (
          <Pressable
            onPress={pauseFocus}
            style={[styles.mainBtn, { backgroundColor: colors.secondary }]}
          >
            <Pause color={colors.onSecondary} size={22} />
            <Text style={[styles.mainBtnText, { color: colors.onSecondary }]}>Pause</Text>
          </Pressable>
        )}
        {focus.isPaused && (
          <>
            <Pressable
              onPress={resumeFocus}
              style={[styles.mainBtn, { backgroundColor: colors.primary }]}
            >
              <Play color={colors.onPrimary} size={22} fill={colors.onPrimary} />
              <Text style={[styles.mainBtnText, { color: colors.onPrimary }]}>Resume</Text>
            </Pressable>
            <Pressable
              onPress={() => void finishFocus('Manual stop')}
              style={[styles.secondaryBtn, { borderColor: colors.border }]}
            >
              <Text style={{ color: colors.onSurface, fontWeight: '700' }}>Finish</Text>
            </Pressable>
          </>
        )}
        {(focus.isRunning || focus.isPaused) && (
          <Pressable onPress={resetFocus} style={styles.iconBtn}>
            <RotateCcw color={colors.onSurfaceVariant} size={20} />
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: 20, paddingTop: 8 },
  heading: { fontSize: 26, fontWeight: '800' },
  sub: { marginTop: 4, marginBottom: 16, fontSize: 14, fontWeight: '600' },
  modeRow: { flexDirection: 'row', gap: 10, marginBottom: 24 },
  modePill: { paddingHorizontal: 16, paddingVertical: 10, borderRadius: 999 },
  ring: {
    alignSelf: 'center',
    width: 240,
    height: 240,
    borderRadius: 120,
    borderWidth: 10,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
  },
  ringProgress: {
    ...StyleSheet.absoluteFill,
    borderRadius: 120,
    borderWidth: 10,
  },
  timer: { fontSize: 44, fontWeight: '800', fontVariant: ['tabular-nums'] },
  presets: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, justifyContent: 'center' },
  preset: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 999,
    borderWidth: 1,
  },
  habitPick: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 20,
  },
  habitChip: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
    maxWidth: '48%',
  },
  controls: {
    marginTop: 28,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  mainBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 28,
    paddingVertical: 14,
    borderRadius: 999,
  },
  mainBtnText: { fontSize: 16, fontWeight: '800' },
  secondaryBtn: {
    borderWidth: 1,
    paddingHorizontal: 18,
    paddingVertical: 14,
    borderRadius: 999,
  },
  iconBtn: { padding: 12 },
});
