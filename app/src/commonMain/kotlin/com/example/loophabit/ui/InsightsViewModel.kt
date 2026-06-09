package com.example.loophabit.ui

import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for the data that InsightsComponents needs from the ViewModel.
 * Allows InsightsComponents to live in commonMain while HabitViewModel stays in androidMain.
 */
interface InsightsViewModel {
    val allHabits: StateFlow<List<Habit>>
    val allCompletions: StateFlow<List<HabitCompletion>>
    val allFocusSessions: StateFlow<List<FocusSession>>
    fun calculateStreaks(dates: List<String>, daysOfWeekPattern: String): Pair<Int, Int>
}
