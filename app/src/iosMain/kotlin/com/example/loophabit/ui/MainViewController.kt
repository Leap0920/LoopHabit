package com.example.loophabit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.loophabit.data.*
import com.example.loophabit.ui.screens.*
import com.example.loophabit.ui.theme.LoopHabitTheme
import platform.UIKit.UIViewController

fun MainViewController(
    viewModel: LoopHabitViewModel
): UIViewController {
    return ComposeUIViewController {
        LoopHabitTheme {
            AppContent(viewModel)
        }
    }
}

@Composable
private fun AppContent(viewModel: LoopHabitViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    var showAddHabit by remember { mutableStateOf(false) }
    var selectedHabitForDetails by remember { mutableStateOf<Habit?>(null) }
    var isAuthenticated by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = { isAuthenticated = true }
        )
    } else {
        MainScreen(
            viewModel = viewModel,
            onNavigateToFocus = {},
            onNavigateToInsights = {},
            onOpenSettings = { showSettings = true },
            onOpenAddHabit = { showAddHabit = true }
        )
    }

    // Settings dialog
    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false },
            onExportData = { viewModel.exportData() },
            onImportData = { /* TODO: iOS import */ },
            onResetData = {
                viewModel.resetAllData { showSettings = false }
            },
            onLogout = {
                viewModel.logout()
                isAuthenticated = false
                showSettings = false
            }
        )
    }

    // Add habit dialog
    if (showAddHabit) {
        AddHabitDialog(
            onDismiss = { showAddHabit = false },
            onAddHabit = { title, colorHex, targetDaysPerWeek, isNumerical, numericalGoal, numericalUnit, daysOfWeekPattern ->
                viewModel.addHabit(
                    title = title,
                    colorHex = colorHex,
                    targetDaysPerWeek = targetDaysPerWeek,
                    isNumerical = isNumerical,
                    numericalGoal = numericalGoal,
                    numericalUnit = numericalUnit,
                    daysOfWeekPattern = daysOfWeekPattern
                )
                showAddHabit = false
            }
        )
    }

    // Habit details dialog
    selectedHabitForDetails?.let { habit ->
        HabitDetailsDialog(
            habit = habit,
            viewModel = viewModel,
            onDismiss = { selectedHabitForDetails = null },
            onDeleteHabit = { h ->
                viewModel.deleteHabit(h)
                selectedHabitForDetails = null
            },
            onCompleteHabit = { h ->
                viewModel.completeHabit(h.id)
                selectedHabitForDetails = null
            },
            onAddNote = { habitId, date, notes ->
                viewModel.saveCompletionNote(habitId, date, notes, 0.0)
            }
        )
    }
}
