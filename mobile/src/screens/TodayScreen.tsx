import React, { useMemo, useState } from 'react';
import {
  FlatList,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Plus, CheckCircle2 } from 'lucide-react-native';
import { SwipeableHabitCard } from '../components/SwipeableHabitCard';
import { useHabitStore } from '../store/useHabitStore';
import { useTheme } from '../theme/ThemeContext';
import { formatDisplayDate } from '../utils/date';
import { HABIT_PALETTE } from '../theme/colors';

export function TodayScreen() {
  const { colors } = useTheme();
  const selectedDate = useHabitStore((s) => s.selectedDate);
  const habits = useHabitStore((s) => s.habits);
  const incompleteHabits = useHabitStore((s) => s.incompleteHabits);
  const completedHabits = useHabitStore((s) => s.completedHabits);
  const currentHabit = useHabitStore((s) => s.currentHabit);
  const completeHabit = useHabitStore((s) => s.completeHabit);
  const skipCurrent = useHabitStore((s) => s.skipCurrent);
  const uncompleteHabit = useHabitStore((s) => s.uncompleteHabit);
  const addHabit = useHabitStore((s) => s.addHabit);

  const [showAdd, setShowAdd] = useState(false);
  const [title, setTitle] = useState('');
  const [color, setColor] = useState<string>(HABIT_PALETTE[0]);

  const incomplete = incompleteHabits();
  const completed = completedHabits();
  const current = currentHabit();

  const progress = useMemo(() => {
    if (!habits.length) return 0;
    return completed.length / habits.length;
  }, [habits.length, completed.length]);

  const submit = async () => {
    if (!title.trim()) return;
    await addHabit(title, color);
    setTitle('');
    setShowAdd(false);
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={styles.headerRow}>
        <View>
          <Text style={[styles.date, { color: colors.onSurfaceVariant }]}>
            {formatDisplayDate(selectedDate)}
          </Text>
          <Text style={[styles.heading, { color: colors.onBackground }]}>Today&apos;s Loop</Text>
        </View>
        <Pressable
          onPress={() => setShowAdd(true)}
          style={[styles.fab, { backgroundColor: colors.primary }]}
        >
          <Plus color={colors.onPrimary} size={22} />
        </Pressable>
      </View>

      <View style={[styles.progressTrack, { backgroundColor: colors.surfaceVariant }]}>
        <View
          style={[
            styles.progressFill,
            { width: `${Math.round(progress * 100)}%`, backgroundColor: colors.tertiary },
          ]}
        />
      </View>
      <Text style={[styles.progressLabel, { color: colors.onSurfaceVariant }]}>
        {completed.length} / {habits.length} complete
      </Text>

      {current ? (
        <SwipeableHabitCard
          key={current.id}
          habit={current}
          onComplete={() => void completeHabit(current.id)}
          onSkip={skipCurrent}
        />
      ) : (
        <View style={[styles.emptyCard, { borderColor: colors.border, backgroundColor: colors.card }]}>
          <CheckCircle2 color={colors.tertiary} size={40} />
          <Text style={[styles.emptyTitle, { color: colors.onSurface }]}>
            {habits.length ? 'All done for today!' : 'No habits yet'}
          </Text>
          <Text style={{ color: colors.onSurfaceVariant, textAlign: 'center' }}>
            {habits.length
              ? 'Great work. Come back tomorrow or add another habit.'
              : 'Tap + to create your first habit.'}
          </Text>
        </View>
      )}

      {completed.length > 0 && (
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: colors.onSurface }]}>Completed</Text>
          <FlatList
            data={completed}
            keyExtractor={(item) => String(item.id)}
            scrollEnabled={false}
            renderItem={({ item }) => (
              <Pressable
                onLongPress={() => void uncompleteHabit(item.id)}
                style={[styles.doneRow, { backgroundColor: colors.card, borderColor: colors.border }]}
              >
                <View style={[styles.dot, { backgroundColor: item.colorHex }]} />
                <Text style={[styles.doneTitle, { color: colors.onSurface }]}>{item.title}</Text>
                <CheckCircle2 color={colors.tertiary} size={18} />
              </Pressable>
            )}
          />
          <Text style={[styles.hint, { color: colors.onSurfaceVariant }]}>
            Long-press a completed habit to undo
          </Text>
        </View>
      )}

      {incomplete.length > 1 && (
        <Text style={[styles.hint, { color: colors.onSurfaceVariant }]}>
          {incomplete.length - 1} more in the loop
        </Text>
      )}

      <Modal visible={showAdd} transparent animationType="fade" onRequestClose={() => setShowAdd(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.modalTitle, { color: colors.onSurface }]}>New habit</Text>
            <TextInput
              value={title}
              onChangeText={setTitle}
              placeholder="e.g. Morning stretch"
              placeholderTextColor={colors.onSurfaceVariant}
              style={[
                styles.input,
                { color: colors.onSurface, borderColor: colors.border, backgroundColor: colors.card },
              ]}
              autoFocus
            />
            <View style={styles.palette}>
              {HABIT_PALETTE.map((c) => (
                <Pressable
                  key={c}
                  onPress={() => setColor(c)}
                  style={[
                    styles.swatch,
                    { backgroundColor: c, borderWidth: color === c ? 3 : 0, borderColor: colors.onSurface },
                  ]}
                />
              ))}
            </View>
            <View style={styles.modalActions}>
              <Pressable onPress={() => setShowAdd(false)} style={styles.modalBtn}>
                <Text style={{ color: colors.onSurfaceVariant, fontWeight: '600' }}>Cancel</Text>
              </Pressable>
              <Pressable
                onPress={() => void submit()}
                style={[styles.modalBtn, styles.modalPrimary, { backgroundColor: colors.primary }]}
              >
                <Text style={{ color: colors.onPrimary, fontWeight: '700' }}>Add</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: 20, paddingTop: 8 },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  date: { fontSize: 13, fontWeight: '600' },
  heading: { fontSize: 26, fontWeight: '800', marginTop: 2 },
  fab: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  progressTrack: { height: 8, borderRadius: 8, overflow: 'hidden' },
  progressFill: { height: '100%', borderRadius: 8 },
  progressLabel: { marginTop: 6, marginBottom: 16, fontSize: 13, fontWeight: '600' },
  emptyCard: {
    borderWidth: 1,
    borderRadius: 24,
    minHeight: 220,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 10,
  },
  emptyTitle: { fontSize: 20, fontWeight: '800' },
  section: { marginTop: 20 },
  sectionTitle: { fontSize: 16, fontWeight: '700', marginBottom: 8 },
  doneRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderWidth: 1,
    borderRadius: 14,
    padding: 12,
    marginBottom: 8,
  },
  dot: { width: 10, height: 10, borderRadius: 5 },
  doneTitle: { flex: 1, fontSize: 15, fontWeight: '600' },
  hint: { fontSize: 12, marginTop: 6 },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
    justifyContent: 'center',
    padding: 24,
  },
  modalCard: { borderRadius: 20, padding: 20, gap: 12 },
  modalTitle: { fontSize: 18, fontWeight: '800' },
  input: {
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
  },
  palette: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  swatch: { width: 28, height: 28, borderRadius: 14 },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 10, marginTop: 4 },
  modalBtn: { paddingHorizontal: 16, paddingVertical: 10, borderRadius: 12 },
  modalPrimary: {},
});
