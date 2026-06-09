package com.example.loophabit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
expect fun systemDarkTheme(): Boolean

@Composable
expect fun isDynamicColorAvailable(): Boolean

@Composable
expect fun getPlatformColorScheme(darkTheme: Boolean): androidx.compose.material3.ColorScheme

@Composable
fun LoopHabitTheme(
    darkTheme: Boolean = systemDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && isDynamicColorAvailable() -> getPlatformColorScheme(darkTheme)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
