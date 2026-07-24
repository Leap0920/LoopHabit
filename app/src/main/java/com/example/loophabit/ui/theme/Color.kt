package com.example.loophabit.ui.theme

import androidx.compose.ui.graphics.Color

// (F1) Brand palette - desaturated indigo/violet primary on neutral zinc bases.
// LILA RULE override: the brand is explicitly purple (used in habit cards), so we
// embrace it, but with a single restrained accent, not neon AI-purple gradients.

// Primary - a calm, slightly desaturated indigo (not neon violet)
val Indigo50 = Color(0xFFEDE9FE)
val Indigo100 = Color(0xFFDDD6FE)
val Indigo200 = Color(0xFFC3B0FD)
val Indigo400 = Color(0xFF8B5CF6)
val Indigo500 = Color(0xFF7C3AED)
val Indigo600 = Color(0xFF6D28D9)
val Indigo700 = Color(0xFF5B21B6)

// Light scheme primary/secondary/tertiary
val LightPrimary = Indigo600
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Indigo50
val LightOnPrimaryContainer = Indigo700

val LightSecondary = Color(0xFF625B71)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE8E0EB)
val LightOnSecondaryContainer = Color(0xFF1E1B22)

val LightTertiary = Color(0xFF0D9488) // teal accent for "completed/success" states
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFCCFBF1)
val LightOnTertiaryContainer = Color(0xFF134E4A)

// Neutral surfaces - off-white/cream-tinted, not pure white
val LightBackground = Color(0xFFFAFAF9)
val LightOnBackground = Color(0xFF1C1B1F)
val LightSurface = Color(0xFFFCFBFF)
val LightOnSurface = Color(0xFF1C1B1F)
val LightSurfaceVariant = Color(0xFFE7E0E8)
val LightOnSurfaceVariant = Color(0xFF49454E)
val LightOutline = Color(0xFF7A757F)
val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

// Dark scheme
val DarkPrimary = Indigo400
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Indigo700
val DarkOnPrimaryContainer = Indigo100

val DarkSecondary = Color(0xFFCBC4D3)
val DarkOnSecondary = Color(0xFF332D3D)
val DarkSecondaryContainer = Color(0xFF49444F)
val DarkOnSecondaryContainer = Color(0xFFE8E0EB)

val DarkTertiary = Color(0xFF5EEAD4) // bright teal for dark mode
val DarkOnTertiary = Color(0xFF003731)
val DarkTertiaryContainer = Color(0xFF00504A)
val DarkOnTertiaryContainer = Color(0xFFCCFBF1)

val DarkBackground = Color(0xFF131218) // off-black, not pure black
val DarkOnBackground = Color(0xFFE6E1E8)
val DarkSurface = Color(0xFF1A1A20)
val DarkOnSurface = Color(0xFFE6E1E8)
val DarkSurfaceVariant = Color(0xFF2A2730)
val DarkOnSurfaceVariant = Color(0xFFCAC4CF)
val DarkOutline = Color(0xFF948F99)
val DarkError = Color(0xFFF2B8B5)
val DarkOnError = Color(0xFF601410)
val DarkErrorContainer = Color(0xFF8C1D18)
val DarkOnErrorContainer = Color(0xFFF9DEDC)

// Habit card accent colors (used by habit.colorHex from DB, but providing
// a curated fallback set that avoids neon)
val HabitFallbackColor = Indigo500
val SwipeCompleteColor = Color(0xFF0D9488) // teal, not neon green
val SwipeSkipColor = Indigo600
