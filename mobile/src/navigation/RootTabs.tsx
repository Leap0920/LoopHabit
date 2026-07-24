import React, { useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { CalendarDays, CheckCircle2, Home, Settings, Timer } from 'lucide-react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { TodayScreen } from '../screens/TodayScreen';
import { FocusScreen } from '../screens/FocusScreen';
import { TodoScreen } from '../screens/TodoScreen';
import { InsightsScreen } from '../screens/InsightsScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { useTheme } from '../theme/ThemeContext';

const Tab = createBottomTabNavigator();

export function RootTabs() {
  const { colors, darkMode } = useTheme();
  const insets = useSafeAreaInsets();
  const [settingsOpen, setSettingsOpen] = useState(false);

  return (
    <View style={{ flex: 1, backgroundColor: colors.background }}>
      <View
        style={[
          styles.topBar,
          {
            paddingTop: insets.top + 8,
            backgroundColor: colors.background,
            borderBottomColor: colors.border,
          },
        ]}
      >
        <Text style={[styles.brand, { color: colors.onBackground }]}>LoopHabit</Text>
        <Pressable onPress={() => setSettingsOpen(true)} hitSlop={10}>
          <Settings color={colors.onSurface} size={22} />
        </Pressable>
      </View>

      <Tab.Navigator
        screenOptions={({ route }) => ({
          headerShown: false,
          tabBarActiveTintColor: colors.primary,
          tabBarInactiveTintColor: colors.onSurfaceVariant,
          tabBarStyle: {
            backgroundColor: colors.surface,
            borderTopColor: colors.border,
            height: 60 + insets.bottom,
            paddingBottom: Math.max(insets.bottom, 8),
            paddingTop: 8,
          },
          tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
          tabBarIcon: ({ color, size }) => {
            if (route.name === 'Today') return <Home color={color} size={size} />;
            if (route.name === 'Focus') return <Timer color={color} size={size} />;
            if (route.name === 'Todo') return <CheckCircle2 color={color} size={size} />;
            return <CalendarDays color={color} size={size} />;
          },
        })}
      >
        <Tab.Screen name="Today" component={TodayScreen} />
        <Tab.Screen name="Focus" component={FocusScreen} />
        <Tab.Screen name="Todo" component={TodoScreen} />
        <Tab.Screen name="Insights" component={InsightsScreen} />
      </Tab.Navigator>

      <Modal
        visible={settingsOpen}
        animationType="slide"
        transparent
        onRequestClose={() => setSettingsOpen(false)}
      >
        <View style={{ flex: 1, backgroundColor: darkMode ? 'rgba(0,0,0,0.6)' : 'rgba(0,0,0,0.35)' }}>
          <SettingsScreen onClose={() => setSettingsOpen(false)} />
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  topBar: {
    paddingHorizontal: 20,
    paddingBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  brand: {
    fontSize: 20,
    fontWeight: '900',
    letterSpacing: 0.2,
  },
});
