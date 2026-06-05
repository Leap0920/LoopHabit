package com.example.loophabit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.loophabit.ui.HabitViewModel
import com.example.loophabit.ui.HabitViewModelFactory
import com.example.loophabit.ui.MainScreen
import com.example.loophabit.ui.theme.LoopHabitTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels {
        HabitViewModelFactory((application as LoopHabitApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoopHabitTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}