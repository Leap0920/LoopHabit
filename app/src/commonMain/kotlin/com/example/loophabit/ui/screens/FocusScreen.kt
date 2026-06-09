@file:OptIn(kotlin.time.ExperimentalTime::class, ExperimentalMaterial3Api::class)

package com.example.loophabit.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loophabit.data.Habit
import com.example.loophabit.data.LoopPreferences
import com.example.loophabit.ui.LoopHabitViewModel
import com.example.loophabit.ui.parseHexColorOrDefault
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Clock

enum class FocusMode {
    TIMER, STOPWATCH
}

@Composable
fun FocusScreen(
    viewModel: LoopHabitViewModel,
    modifier: Modifier = Modifier
) {
    val focusState by viewModel.focusState.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()

    var isRunning by remember { mutableStateOf(focusState.isRunning) }
    var mode by remember { mutableStateOf(focusState.mode.ifBlank { FocusMode.TIMER.name }) }
    var secondsLeft by remember {
        mutableStateOf(
            if (focusState.mode == FocusMode.TIMER.name) {
                focusState.pausedSeconds ?: (focusState.initialDurationMinutes * 60)
            } else 0
        )
    }
    var secondsElapsed by remember {
        mutableStateOf(
            if (focusState.mode == FocusMode.STOPWATCH.name) focusState.pausedSeconds else 0
        )
    }
    var selectedHabitId by remember { mutableStateOf(focusState.habitId) }
    var taskDetails by remember { mutableStateOf(focusState.taskDetails ?: "") }

    // Timer coroutine
    LaunchedEffect(isRunning, mode) {
        if (isRunning) {
            while (isActive) {
                delay(1000)
                if (mode == FocusMode.TIMER.name) {
                    if (secondsLeft > 0) {
                        secondsLeft--
                        secondsElapsed++
                        viewModel.updateFocusState(
                            LoopPreferences.FocusState(
                                isRunning = true,
                                mode = mode,
                                habitId = selectedHabitId ?: 0L,
                                taskDetails = taskDetails.ifBlank { "" },
                                initialDurationMinutes = (secondsLeft + secondsElapsed) / 60,
                                pausedSeconds = secondsLeft,
                                baseTimestamp = Clock.System.now().toEpochMilliseconds()
                            )
                        )
                    } else {
                        isRunning = false
                        viewModel.logFocusSession(selectedHabitId, secondsElapsed, taskDetails.ifBlank { null })
                    }
                } else {
                    secondsElapsed++
                    viewModel.updateFocusState(
                        LoopPreferences.FocusState(
                            isRunning = true,
                            mode = mode,
                            habitId = selectedHabitId ?: 0L,
                            taskDetails = taskDetails.ifBlank { "" },
                            pausedSeconds = secondsElapsed,
                            baseTimestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FocusMode.values().forEach { focusMode ->
                FilterChip(
                    selected = mode == focusMode.name,
                    onClick = {
                        if (!isRunning) {
                            mode = focusMode.name
                            if (focusMode == FocusMode.TIMER) {
                                secondsLeft = 25 * 60
                                secondsElapsed = 0
                            } else {
                                secondsLeft = 0
                                secondsElapsed = 0
                            }
                        }
                    },
                    label = { Text(focusMode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    enabled = !isRunning
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Timer display
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(if (mode == FocusMode.TIMER.name) secondsLeft else secondsElapsed),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (mode == FocusMode.TIMER.name && secondsElapsed > 0) {
                    Text(
                        text = "Elapsed: ${formatTime(secondsElapsed)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Habit selector
        var expanded by remember { mutableStateOf(false) }
        val selectedHabit = allHabits.find { it.id == selectedHabitId }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedHabit?.title ?: "No habit selected",
                onValueChange = {},
                readOnly = true,
                label = { Text("Associated Habit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        selectedHabitId = 0L
                        expanded = false
                    }
                )
                allHabits.forEach { habit ->
                    DropdownMenuItem(
                        text = { Text(habit.title) },
                        onClick = {
                            selectedHabitId = habit.id
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task details
        OutlinedTextField(
            value = taskDetails,
            onValueChange = { taskDetails = it },
            label = { Text("Task Details (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Reset button
            FilledTonalButton(
                onClick = {
                    isRunning = false
                    secondsLeft = 25 * 60
                    secondsElapsed = 0
                    viewModel.updateFocusState(LoopPreferences.FocusState())
                },
                enabled = isRunning || secondsElapsed > 0
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset")
            }

            // Start/Pause button
            Button(
                onClick = {
                    isRunning = !isRunning
                    viewModel.updateFocusState(
                        LoopPreferences.FocusState(
                            isRunning = isRunning,
                            mode = mode,
                            habitId = selectedHabitId ?: 0L,
                            taskDetails = taskDetails.ifBlank { "" },
                            initialDurationMinutes = 25,
                            pausedSeconds = if (mode == FocusMode.TIMER.name) secondsLeft else secondsElapsed,
                            baseTimestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Log session button
            FilledTonalButton(
                onClick = {
                    if (secondsElapsed > 0) {
                        viewModel.logFocusSession(selectedHabitId, secondsElapsed, taskDetails.ifBlank { null })
                        isRunning = false
                        secondsElapsed = 0
                        secondsLeft = 25 * 60
                        viewModel.updateFocusState(LoopPreferences.FocusState())
                    }
                },
                enabled = secondsElapsed > 0 && !isRunning
            ) {
                Icon(Icons.Default.Save, contentDescription = "Log")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log")
            }
        }

        // Duration presets for timer
        if (mode == FocusMode.TIMER.name && !isRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(15, 25, 30, 45, 60).forEach { minutes ->
                    FilterChip(
                        selected = secondsLeft == minutes * 60,
                        onClick = { secondsLeft = minutes * 60 },
                        label = { Text("${minutes}m") }
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
