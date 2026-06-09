package com.example.loophabit.data

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun createPlatformSettings(): Settings {
    // This will be initialized by LoopHabitApp with the application context
    return LoopPreferencesHolder.settings
}

object LoopPreferencesHolder {
    lateinit var settings: Settings
    fun init(context: Context) {
        if (!::settings.isInitialized) {
            settings = SharedPreferencesSettings(
                context.getSharedPreferences("loop_preferences", Context.MODE_PRIVATE)
            )
        }
    }
}
