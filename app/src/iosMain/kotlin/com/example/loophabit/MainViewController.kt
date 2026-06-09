package com.example.loophabit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController

@Composable
fun AppContent() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "LoopHabit (iOS)",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

fun MainViewController() = ComposeUIViewController {
    AppContent()
}
