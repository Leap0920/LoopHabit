package com.example.loophabit.data.sync

import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.FocusSession

data class BackupData(
    val habits: List<Habit>?,
    val completions: List<HabitCompletion>?,
    val focusSessions: List<FocusSession>?
)
