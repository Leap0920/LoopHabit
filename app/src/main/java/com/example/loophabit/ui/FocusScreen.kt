package com.example.loophabit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loophabit.data.Habit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: HabitViewModel) {
    val habits by viewModel.allHabits.collectAsState()

    var selectedHabit by remember { mutableStateOf<Habit?>(null) }
    var taskDetails by remember { mutableStateOf("") }
    
    // Switcher mode state: TIMER, STOPWATCH
    var focusMode by remember { mutableStateOf("TIMER") }

    // Timer state
    var initialDurationMinutes by remember { mutableStateOf(25) }
    var secondsLeft by remember { mutableStateOf(25 * 60) }
    
    // Stopwatch state
    var secondsElapsed by remember { mutableStateOf(0) }

    var isRunning by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Sync selected habit when habits change or initially
    LaunchedEffect(habits) {
        if (selectedHabit == null && habits.isNotEmpty()) {
            selectedHabit = habits.firstOrNull()
        }
    }

    // Reset secondsLeft when initialDurationMinutes changes (if not running)
    LaunchedEffect(initialDurationMinutes) {
        if (!isRunning && focusMode == "TIMER") {
            secondsLeft = initialDurationMinutes * 60
        }
    }

    // Reset stopwatch/timer when switching modes
    LaunchedEffect(focusMode) {
        isRunning = false
        if (focusMode == "TIMER") {
            secondsLeft = initialDurationMinutes * 60
        } else {
            secondsElapsed = 0
        }
    }

    // Ticking loop
    LaunchedEffect(isRunning, secondsLeft, secondsElapsed, focusMode) {
        if (isRunning) {
            delay(1000L)
            if (focusMode == "TIMER") {
                if (secondsLeft > 0) {
                    secondsLeft -= 1
                    if (secondsLeft == 0) {
                        isRunning = false
                        showSuccessDialog = true
                    }
                }
            } else {
                secondsElapsed += 1
            }
        }
    }

    val parsedColor = remember(selectedHabit?.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedHabit?.colorHex ?: "#8338EC"))
        } catch (e: Exception) {
            Color(0xFF8338EC)
        }
    }

    // Progress percentage
    val progress = if (focusMode == "TIMER") {
        if (initialDurationMinutes > 0) {
            secondsLeft.toFloat() / (initialDurationMinutes * 60).toFloat()
        } else {
            0f
        }
    } else {
        // For stopwatch, show progress within the current minute
        (secondsElapsed % 60) / 60f
    }

    // Display formatted time
    val displaySeconds = if (focusMode == "TIMER") secondsLeft else secondsElapsed
    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    var showHabitsDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Focus Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                text = "Dedicate uninterrupted time to achieve your goals",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Timer / Stopwatch Pill Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("TIMER", "STOPWATCH").forEach { mode ->
                    val isSelected = focusMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) parsedColor else Color.Transparent)
                            .clickable(enabled = !isRunning) {
                                focusMode = mode
                            }
                    ) {
                        Text(
                            text = if (mode == "TIMER") "Countdown Timer" else "Stopwatch Mode",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Large Timer Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
            ) {
                // Progress indicator wrapping the circle
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(232.dp),
                    strokeWidth = 10.dp,
                    color = parsedColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeString,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRunning) "STAY FOCUSED" else "PAUSED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isRunning) parsedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls Buttons: Reset, Play/Pause, Finish
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        isRunning = false
                        if (focusMode == "TIMER") {
                            secondsLeft = initialDurationMinutes * 60
                        } else {
                            secondsElapsed = 0
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset Timer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Play / Pause Button
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(containerColor = parsedColor),
                    shape = CircleShape,
                    modifier = Modifier
                        .height(56.dp)
                        .width(160.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Pause" else "Start",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                // Finish / Complete Button (Visible in Stopwatch mode or early stop for timer)
                val isTimerEarly = focusMode == "TIMER" && secondsLeft < (initialDurationMinutes * 60)
                val isStopwatchLogged = focusMode == "STOPWATCH" && secondsElapsed > 0
                if (isTimerEarly || isStopwatchLogged) {
                    IconButton(
                        onClick = {
                            isRunning = false
                            showSuccessDialog = true
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(parsedColor.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Finish Session",
                            tint = parsedColor
                        )
                    }
                } else {
                    // Spacer to keep layout balanced
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Duration selector pills (Only show when in Countdown Timer Mode)
            if (focusMode == "TIMER") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Session Duration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1, 15, 25, 45, 60).forEach { mins ->
                                val isSelected = initialDurationMinutes == mins
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) parsedColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                        )
                                        .clickable(enabled = !isRunning) {
                                            initialDurationMinutes = mins
                                        }
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Habit and details settings
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Associate with Habit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Habit Dropdown trigger with matched border/sizing UI
                    var parentWidth by remember { mutableStateOf(0) }
                    val density = LocalDensity.current

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                parentWidth = coordinates.size.width
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (showHabitsDropdown) parsedColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = if (showHabitsDropdown) 0.2f else 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isRunning) { showHabitsDropdown = true }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                )
                                Text(
                                    text = selectedHabit?.title ?: "Select Habit",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Open selector",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showHabitsDropdown,
                            onDismissRequest = { showHabitsDropdown = false },
                            modifier = Modifier
                                .width(with(density) { parentWidth.toDp() })
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (habits.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No habits available", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = { showHabitsDropdown = false }
                                )
                            } else {
                                habits.forEach { habit ->
                                    val color = remember(habit.colorHex) {
                                        try {
                                            Color(android.graphics.Color.parseColor(habit.colorHex))
                                        } catch (e: Exception) {
                                            Color(0xFF8338EC)
                                        }
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Text(
                                                    text = habit.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedHabit = habit
                                            showHabitsDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Task Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Transparent Glassmorphic Input Details
                    OutlinedTextField(
                        value = taskDetails,
                        onValueChange = { taskDetails = it },
                        placeholder = { Text("What are you working on? (Optional)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = parsedColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Bottom spacer for navigation padding
        }
    }

    // Success completed dialogue
    if (showSuccessDialog) {
        val minutesCompleted = if (focusMode == "TIMER") {
            (initialDurationMinutes * 60 - secondsLeft) / 60
        } else {
            secondsElapsed / 60
        }
        val secondsCompleted = if (focusMode == "TIMER") {
            (initialDurationMinutes * 60 - secondsLeft) % 60
        } else {
            secondsElapsed % 60
        }
        
        val timeDisplay = if (minutesCompleted > 0) {
            "${minutesCompleted}m ${secondsCompleted}s"
        } else {
            "${secondsCompleted}s"
        }

        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Success",
                    tint = parsedColor,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Focus Complete!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (selectedHabit != null) {
                        "Congratulations! You completed $timeDisplay of focus for '${selectedHabit?.title}'. Would you like to log this habit completion?"
                    } else {
                        "Congratulations! You completed a $timeDisplay focus session!"
                    },
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                if (selectedHabit != null) {
                    Button(
                        onClick = {
                            viewModel.completeHabit(selectedHabit!!.id)
                            showSuccessDialog = false
                            if (focusMode == "TIMER") {
                                secondsLeft = initialDurationMinutes * 60
                            } else {
                                secondsElapsed = 0
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parsedColor)
                    ) {
                        Text("Log Completion", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            if (focusMode == "TIMER") {
                                secondsLeft = initialDurationMinutes * 60
                            } else {
                                secondsElapsed = 0
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parsedColor)
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        if (focusMode == "TIMER") {
                            secondsLeft = initialDurationMinutes * 60
                        } else {
                            secondsElapsed = 0
                        }
                    }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
