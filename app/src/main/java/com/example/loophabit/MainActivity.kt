package com.example.loophabit

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
            (application as LoopHabitApp).syncManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen(viewModel = viewModel)
        }
    }
}