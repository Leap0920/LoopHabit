import React from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { Moon, Trash2 } from 'lucide-react-native';
import { useHabitStore } from '../store/useHabitStore';
import { useTheme } from '../theme/ThemeContext';

export function SettingsScreen({ onClose }: { onClose: () => void }) {
  const { colors, darkMode, toggleDarkMode } = useTheme();
  const habits = useHabitStore((s) => s.habits);
  const deleteHabit = useHabitStore((s) => s.deleteHabit);

  const confirmDelete = (id: number, title: string) => {
    Alert.alert('Delete habit', `Remove “${title}”?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => void deleteHabit(id),
      },
    ]);
  };

  return (
    <View style={[styles.sheet, { backgroundColor: colors.surface }]}>
      <View style={styles.handleRow}>
        <Text style={[styles.title, { color: colors.onSurface }]}>Settings</Text>
        <Pressable onPress={onClose}>
          <Text style={{ color: colors.primary, fontWeight: '700' }}>Done</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
        <View style={[styles.row, { borderColor: colors.border }]}>
          <View style={styles.rowLeft}>
            <Moon color={colors.onSurface} size={20} />
            <Text style={[styles.rowLabel, { color: colors.onSurface }]}>Dark mode</Text>
          </View>
          <Switch
            value={darkMode}
            onValueChange={toggleDarkMode}
            trackColor={{ true: colors.primary, false: colors.surfaceVariant }}
          />
        </View>

        <Text style={[styles.section, { color: colors.onSurfaceVariant }]}>Manage habits</Text>
        {habits.length === 0 && (
          <Text style={{ color: colors.onSurfaceVariant, paddingHorizontal: 4 }}>
            No habits to manage yet.
          </Text>
        )}
        {habits.map((h) => (
          <View key={h.id} style={[styles.habitRow, { borderColor: colors.border }]}>
            <View style={[styles.dot, { backgroundColor: h.colorHex }]} />
            <Text style={[styles.habitTitle, { color: colors.onSurface }]} numberOfLines={1}>
              {h.title}
            </Text>
            <Pressable onPress={() => confirmDelete(h.id, h.title)} hitSlop={8}>
              <Trash2 color={colors.error} size={18} />
            </Pressable>
          </View>
        ))}

        <Text style={[styles.footer, { color: colors.onSurfaceVariant }]}>
          LoopHabit React Native · local-first SQLite{'\n'}
          Native Android widgets, Supabase sync, and background focus service
          can be ported in follow-up phases.
        </Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  sheet: {
    flex: 1,
    marginTop: 48,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 20,
  },
  handleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: { fontSize: 20, fontWeight: '800' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    paddingVertical: 14,
  },
  rowLeft: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  rowLabel: { fontSize: 16, fontWeight: '600' },
  section: {
    marginTop: 24,
    marginBottom: 10,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.6,
    textTransform: 'uppercase',
  },
  habitRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    borderBottomWidth: 1,
    paddingVertical: 12,
  },
  dot: { width: 10, height: 10, borderRadius: 5 },
  habitTitle: { flex: 1, fontSize: 15, fontWeight: '600' },
  footer: { marginTop: 28, fontSize: 12, lineHeight: 18 },
});
