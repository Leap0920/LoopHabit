package com.example.loophabit.data

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings

actual fun createPlatformSettings(): Settings {
    return NSUserDefaultsSettings(suiteName = "loop_preferences")
}
