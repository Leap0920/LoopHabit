package com.example.loophabit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.loophabit.ui.HabitViewModel
import com.example.loophabit.ui.HabitViewModelFactory
import com.example.loophabit.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(
            (application as LoopHabitApp).repository,
            (application as LoopHabitApp).syncManager,
            applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MainScreen(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.hasExtra("focus_habit_id")) {
            val habitId = intent.getLongExtra("focus_habit_id", -1L)
            if (habitId != -1L) {
                viewModel.setFocusHabitId(habitId)
            }
        }
    }
}