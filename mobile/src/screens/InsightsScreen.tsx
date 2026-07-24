import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ChevronLeft, ChevronRight, Flame, Timer } from 'lucide-react-native';
import { addMonths, format, subMonths } from 'date-fns';
import { useHabitStore } from '../store/useHabitStore';
import { useTheme } from '../theme/ThemeContext';
import { daysInMonthGrid, formatMonthYear, todayYmd } from '../utils/date';

function computeStreak(dates: string[]): number {
  if (!dates.length) return 0;
  const set = new Set(dates);
  let cursor = todayYmd();
  // If today has no completions, start from yesterday
  if (!set.has(cursor)) {
    const d = new Date();
    d.setDate(d.getDate() - 1);
    cursor = format(d, 'yyyy-MM-dd');
  }
  let streak = 0;
  while (set.has(cursor)) {
    streak += 1;
    const d = new Date(cursor + 'T12:00:00');
    d.setDate(d.getDate() - 1);
    cursor = format(d, 'yyyy-MM-dd');
  }
  return streak;
}

export function InsightsScreen() {
  const { colors } = useTheme();
  const allCompletionDates = useHabitStore((s) => s.allCompletionDates);
  const focusSessions = useHabitStore((s) => s.focusSessions);
  const habits = useHabitStore((s) => s.habits);
  const [month, setMonth] = useState(new Date());

  const dateSet = useMemo(() => new Set(allCompletionDates), [allCompletionDates]);
  const streak = useMemo(() => computeStreak(allCompletionDates), [allCompletionDates]);
  const totalFocusMin = useMemo(
    () => Math.round(focusSessions.reduce((sum, s) => sum + s.durationSeconds, 0) / 60),
    [focusSessions]
  );
  const grid = useMemo(() => daysInMonthGrid(month), [month]);
  const today = todayYmd();

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: colors.background }}
      contentContainerStyle={styles.container}
    >
      <Text style={[styles.heading, { color: colors.onBackground }]}>Insights</Text>

      <View style={styles.statsRow}>
        <View style={[styles.statCard, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <Flame color={colors.primary} size={22} />
          <Text style={[styles.statValue, { color: colors.onSurface }]}>{streak}</Text>
          <Text style={{ color: colors.onSurfaceVariant, fontWeight: '600' }}>Day streak</Text>
        </View>
        <View style={[styles.statCard, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <Timer color={colors.tertiary} size={22} />
          <Text style={[styles.statValue, { color: colors.onSurface }]}>{totalFocusMin}m</Text>
          <Text style={{ color: colors.onSurfaceVariant, fontWeight: '600' }}>Focus total</Text>
        </View>
      </View>

      <View style={[styles.calCard, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <View style={styles.calHeader}>
          <Pressable onPress={() => setMonth((m) => subMonths(m, 1))} hitSlop={8}>
            <ChevronLeft color={colors.onSurface} size={22} />
          </Pressable>
          <Text style={[styles.monthTitle, { color: colors.onSurface }]}>
            {formatMonthYear(month)}
          </Text>
          <Pressable onPress={() => setMonth((m) => addMonths(m, 1))} hitSlop={8}>
            <ChevronRight color={colors.onSurface} size={22} />
          </Pressable>
        </View>

        <View style={styles.weekRow}>
          {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((d, i) => (
            <Text key={`${d}-${i}`} style={[styles.weekLabel, { color: colors.onSurfaceVariant }]}>
              {d}
            </Text>
          ))}
        </View>

        <View style={styles.grid}>
          {grid.map((cell) => {
            const active = dateSet.has(cell.ymd);
            const isToday = cell.ymd === today;
            return (
              <View
                key={cell.ymd + String(cell.inMonth)}
                style={[
                  styles.day,
                  {
                    backgroundColor: active ? colors.primary : 'transparent',
                    opacity: cell.inMonth ? 1 : 0.25,
                    borderWidth: isToday ? 2 : 0,
                    borderColor: colors.tertiary,
                  },
                ]}
              >
                <Text
                  style={{
                    color: active ? colors.onPrimary : colors.onSurface,
                    fontSize: 12,
                    fontWeight: isToday ? '800' : '600',
                  }}
                >
                  {cell.date.getDate()}
                </Text>
              </View>
            );
          })}
        </View>
      </View>

      <View style={[styles.summary, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <Text style={[styles.summaryTitle, { color: colors.onSurface }]}>Summary</Text>
        <Text style={{ color: colors.onSurfaceVariant, lineHeight: 22 }}>
          {habits.length} habits · {allCompletionDates.length} active days ·{' '}
          {focusSessions.length} focus sessions logged
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 40 },
  heading: { fontSize: 26, fontWeight: '800', marginBottom: 16 },
  statsRow: { flexDirection: 'row', gap: 12, marginBottom: 16 },
  statCard: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    gap: 6,
  },
  statValue: { fontSize: 28, fontWeight: '800', marginTop: 4 },
  calCard: { borderWidth: 1, borderRadius: 18, padding: 16 },
  calHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  monthTitle: { fontSize: 16, fontWeight: '800' },
  weekRow: { flexDirection: 'row', marginBottom: 6 },
  weekLabel: { width: `${100 / 7}%`, textAlign: 'center', fontSize: 12, fontWeight: '700' },
  grid: { flexDirection: 'row', flexWrap: 'wrap' },
  day: {
    width: `${100 / 7}%`,
    aspectRatio: 1,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 999,
    marginVertical: 2,
  },
  summary: {
    marginTop: 16,
    borderWidth: 1,
    borderRadius: 18,
    padding: 16,
    gap: 6,
  },
  summaryTitle: { fontSize: 16, fontWeight: '800' },
});
