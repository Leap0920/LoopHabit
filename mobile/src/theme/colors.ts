// Ported from Android Compose theme (Color.kt)
export const Indigo = {
  50: '#EDE9FE',
  100: '#DDD6FE',
  200: '#C3B0FD',
  400: '#8B5CF6',
  500: '#7C3AED',
  600: '#6D28D9',
  700: '#5B21B6',
} as const;

export const lightColors = {
  primary: Indigo[600],
  onPrimary: '#FFFFFF',
  primaryContainer: Indigo[50],
  onPrimaryContainer: Indigo[700],
  secondary: '#625B71',
  onSecondary: '#FFFFFF',
  tertiary: '#0D9488',
  onTertiary: '#FFFFFF',
  tertiaryContainer: '#CCFBF1',
  background: '#FAFAF9',
  onBackground: '#1C1B1F',
  surface: '#FCFBFF',
  onSurface: '#1C1B1F',
  surfaceVariant: '#E7E0E8',
  onSurfaceVariant: '#49454E',
  outline: '#7A757F',
  error: '#B3261E',
  card: '#FFFFFF',
  border: '#E7E0E8',
  swipeComplete: '#0D9488',
  swipeSkip: Indigo[600],
  habitFallback: Indigo[500],
} as const;

export const darkColors = {
  primary: Indigo[400],
  onPrimary: '#FFFFFF',
  primaryContainer: Indigo[700],
  onPrimaryContainer: Indigo[100],
  secondary: '#CBC4D3',
  onSecondary: '#332D3D',
  tertiary: '#5EEAD4',
  onTertiary: '#003731',
  tertiaryContainer: '#00504A',
  background: '#131218',
  onBackground: '#E6E1E8',
  surface: '#1A1A20',
  onSurface: '#E6E1E8',
  surfaceVariant: '#2A2730',
  onSurfaceVariant: '#CAC4CF',
  outline: '#948F99',
  error: '#F2B8B5',
  card: '#1A1A20',
  border: '#2A2730',
  swipeComplete: '#5EEAD4',
  swipeSkip: Indigo[400],
  habitFallback: Indigo[400],
} as const;

export type AppColors = {
  [K in keyof typeof lightColors]: string;
};

export const HABIT_PALETTE = [
  '#7C3AED',
  '#0D9488',
  '#2563EB',
  '#DB2777',
  '#EA580C',
  '#CA8A04',
  '#4F46E5',
  '#059669',
] as const;
