import React, { useState } from 'react';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Check, Plus, Trash2 } from 'lucide-react-native';
import { useHabitStore } from '../store/useHabitStore';
import { useTheme } from '../theme/ThemeContext';

export function TodoScreen() {
  const { colors } = useTheme();
  const todos = useHabitStore((s) => s.todos);
  const addTodo = useHabitStore((s) => s.addTodo);
  const toggleTodo = useHabitStore((s) => s.toggleTodo);
  const deleteTodo = useHabitStore((s) => s.deleteTodo);
  const [draft, setDraft] = useState('');

  const submit = async () => {
    if (!draft.trim()) return;
    await addTodo(draft);
    setDraft('');
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.heading, { color: colors.onBackground }]}>Todos</Text>
      <Text style={[styles.sub, { color: colors.onSurfaceVariant }]}>
        Quick tasks alongside your habits
      </Text>

      <View style={styles.inputRow}>
        <TextInput
          value={draft}
          onChangeText={setDraft}
          placeholder="Add a todo..."
          placeholderTextColor={colors.onSurfaceVariant}
          onSubmitEditing={() => void submit()}
          style={[
            styles.input,
            { color: colors.onSurface, borderColor: colors.border, backgroundColor: colors.card },
          ]}
        />
        <Pressable
          onPress={() => void submit()}
          style={[styles.addBtn, { backgroundColor: colors.primary }]}
        >
          <Plus color={colors.onPrimary} size={20} />
        </Pressable>
      </View>

      <FlatList
        data={todos}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ paddingBottom: 40, gap: 8 }}
        ListEmptyComponent={
          <Text style={{ color: colors.onSurfaceVariant, marginTop: 24, textAlign: 'center' }}>
            No todos yet. Capture something small.
          </Text>
        }
        renderItem={({ item }) => (
          <View
            style={[
              styles.row,
              {
                backgroundColor: colors.card,
                borderColor: colors.border,
                opacity: item.isCompleted ? 0.65 : 1,
              },
            ]}
          >
            <Pressable
              onPress={() => void toggleTodo(item.id, !item.isCompleted)}
              style={[
                styles.check,
                {
                  borderColor: item.isCompleted ? colors.tertiary : colors.outline,
                  backgroundColor: item.isCompleted ? colors.tertiary : 'transparent',
                },
              ]}
            >
              {item.isCompleted && <Check color="#fff" size={14} />}
            </Pressable>
            <Text
              style={[
                styles.title,
                {
                  color: colors.onSurface,
                  textDecorationLine: item.isCompleted ? 'line-through' : 'none',
                },
              ]}
            >
              {item.title}
            </Text>
            <Pressable onPress={() => void deleteTodo(item.id)} hitSlop={8}>
              <Trash2 color={colors.onSurfaceVariant} size={18} />
            </Pressable>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: 20, paddingTop: 8 },
  heading: { fontSize: 26, fontWeight: '800' },
  sub: { marginTop: 4, marginBottom: 16, fontSize: 14 },
  inputRow: { flexDirection: 'row', gap: 10, marginBottom: 16 },
  input: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
  },
  addBtn: {
    width: 48,
    height: 48,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    borderWidth: 1,
    borderRadius: 14,
    padding: 14,
  },
  check: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: { flex: 1, fontSize: 15, fontWeight: '600' },
});
