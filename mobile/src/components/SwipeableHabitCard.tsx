import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Animated, {
  runOnJS,
  useAnimatedStyle,
  useSharedValue,
  withSpring,
  withTiming,
} from 'react-native-reanimated';
import * as Haptics from 'expo-haptics';
import { Check, X } from 'lucide-react-native';
import type { Habit } from '../types/models';
import { useTheme } from '../theme/ThemeContext';

type Props = {
  habit: Habit;
  onComplete: () => void;
  onSkip: () => void;
};

const THRESHOLD = 120;

export function SwipeableHabitCard({ habit, onComplete, onSkip }: Props) {
  const { colors } = useTheme();
  const translateX = useSharedValue(0);

  const complete = () => {
    void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    onComplete();
  };
  const skip = () => {
    void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    onSkip();
  };

  const pan = Gesture.Pan()
    .onUpdate((e) => {
      translateX.value = e.translationX;
    })
    .onEnd(() => {
      if (translateX.value > THRESHOLD) {
        translateX.value = withTiming(500, { duration: 180 }, () => {
          runOnJS(complete)();
          translateX.value = 0;
        });
      } else if (translateX.value < -THRESHOLD) {
        translateX.value = withTiming(-500, { duration: 180 }, () => {
          runOnJS(skip)();
          translateX.value = 0;
        });
      } else {
        translateX.value = withSpring(0, { damping: 16 });
      }
    });

  const cardStyle = useAnimatedStyle(() => ({
    transform: [
      { translateX: translateX.value },
      { rotate: `${translateX.value / 40}deg` },
    ],
    opacity: 1 - Math.min(Math.abs(translateX.value) / 1200, 0.8),
  }));

  const completeHint = useAnimatedStyle(() => ({
    opacity: Math.min(Math.max(translateX.value / THRESHOLD, 0), 1),
  }));
  const skipHint = useAnimatedStyle(() => ({
    opacity: Math.min(Math.max(-translateX.value / THRESHOLD, 0), 1),
  }));

  return (
    <View style={styles.wrap}>
      <Animated.View style={[styles.hint, styles.hintRight, completeHint]}>
        <Check color={colors.swipeComplete} size={28} />
        <Text style={{ color: colors.swipeComplete, fontWeight: '700' }}>Done</Text>
      </Animated.View>
      <Animated.View style={[styles.hint, styles.hintLeft, skipHint]}>
        <Text style={{ color: colors.swipeSkip, fontWeight: '700' }}>Skip</Text>
        <X color={colors.swipeSkip} size={28} />
      </Animated.View>

      <GestureDetector gesture={pan}>
        <Animated.View
          style={[
            styles.card,
            cardStyle,
            {
              backgroundColor: colors.card,
              borderColor: colors.border,
              shadowColor: '#000',
            },
          ]}
        >
          <View style={[styles.accent, { backgroundColor: habit.colorHex || colors.habitFallback }]} />
          <View style={styles.body}>
            <Text style={[styles.kicker, { color: colors.onSurfaceVariant }]}>Today&apos;s Loop</Text>
            <Text style={[styles.title, { color: colors.onSurface }]} numberOfLines={3}>
              {habit.title}
            </Text>
            <Text style={[styles.hintText, { color: colors.onSurfaceVariant }]}>
              Swipe right to complete · left to skip
            </Text>
          </View>
        </Animated.View>
      </GestureDetector>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    height: 280,
    justifyContent: 'center',
  },
  card: {
    borderRadius: 24,
    borderWidth: 1,
    minHeight: 240,
    overflow: 'hidden',
    flexDirection: 'row',
    elevation: 4,
    shadowOpacity: 0.12,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
  },
  accent: {
    width: 10,
  },
  body: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
    gap: 10,
  },
  kicker: {
    fontSize: 13,
    fontWeight: '600',
    letterSpacing: 0.4,
    textTransform: 'uppercase',
  },
  title: {
    fontSize: 28,
    fontWeight: '800',
    lineHeight: 34,
  },
  hintText: {
    marginTop: 8,
    fontSize: 13,
  },
  hint: {
    position: 'absolute',
    top: '45%',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    zIndex: 0,
  },
  hintRight: { left: 16 },
  hintLeft: { right: 16 },
});
