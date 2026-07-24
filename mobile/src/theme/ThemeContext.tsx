import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { darkColors, lightColors, type AppColors } from './colors';

type ThemeContextValue = {
  darkMode: boolean;
  colors: AppColors;
  setDarkMode: (value: boolean) => void;
  toggleDarkMode: () => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);
const DARK_KEY = 'loophabit.darkMode';

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [darkMode, setDarkModeState] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(DARK_KEY).then((v) => {
      setDarkModeState(v === '1');
      setReady(true);
    });
  }, []);

  const setDarkMode = (value: boolean) => {
    setDarkModeState(value);
    void AsyncStorage.setItem(DARK_KEY, value ? '1' : '0');
  };

  const value = useMemo(
    () => ({
      darkMode,
      colors: darkMode ? darkColors : lightColors,
      setDarkMode,
      toggleDarkMode: () => setDarkMode(!darkMode),
    }),
    [darkMode]
  );

  if (!ready) return null;

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider');
  return ctx;
}
