package com.example.loophabit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun systemDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
actual fun isDynamicColorAvailable(): Boolean = false // Material You not available on iOS

@Composable
actual fun getPlatformColorScheme(darkTheme: Boolean): ColorScheme {
    // Fallback to static color scheme on iOS
    return if (darkTheme) darkColorScheme() else lightColorScheme()
}
