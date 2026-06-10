package com.example.loophabit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loophabit.data.Habit
import android.media.RingtoneManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.VibratorManager
import android.os.Build
import android.content.Context

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: HabitViewModel) {
    val context = LocalContext.current
    val allHabits by viewModel.allHabits.collectAsState()
    val incompleteHabits by viewModel.incompleteHabits.collectAsState()

    val persistedState by viewModel.focusState.collectAsState()

    val isServiceRunning by FocusService.isServiceRunning.collectAsState()
    val serviceIsPaused by FocusService.isPaused.collectAsState()
    val serviceMode by FocusService.mode.collectAsState()
    val serviceSecondsLeft by FocusService.secondsLeft.collectAsState()
    val serviceSecondsElapsed by FocusService.secondsElapsed.collectAsState()
    val serviceHabitTitle by FocusService.habitTitle.collectAsState()

    val isRunning = isServiceRunning && !serviceIsPaused
    val focusMode = if (isServiceRunning) serviceMode else persistedState.mode

    val selectedHabit = remember(isServiceRunning, serviceHabitTitle, persistedState.habitId, allHabits, incompleteHabits) {
        if (isServiceRunning) {
            allHabits.find { it.title == serviceHabitTitle }
        } else {
            val savedHabit = allHabits.find { it.id == persistedState.habitId }
            if (savedHabit != null && incompleteHabits.any { it.id == savedHabit.id }) {
                savedHabit
            } else {
                incompleteHabits.firstOrNull() ?: allHabits.firstOrNull()
            }
        }
    }

    val initialDurationMinutes = persistedState.initialDurationMinutes

    val secondsLeft = if (isServiceRunning && serviceMode == "TIMER") {
        serviceSecondsLeft
    } else {
        persistedState.pausedSeconds
    }

    val secondsElapsed = if (isServiceRunning && serviceMode == "STOPWATCH") {
        serviceSecondsElapsed
    } else {
        persistedState.pausedSeconds
    }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }

    // Recover running state if app was killed/restarted but preferences say it's running
    LaunchedEffect(persistedState) {
        if (persistedState.isRunning && !FocusService.isServiceRunning.value) {
            val now = System.currentTimeMillis()
            val diff = ((now - persistedState.baseTimestamp) / 1000).toInt()

            if (persistedState.mode == "TIMER") {
                val remaining = persistedState.pausedSeconds - diff
                if (remaining > 0) {
                    FocusService.startService(
                        context = context,
                        mode = "TIMER",
                        habitTitle = persistedState.habitTitle,
                        durationSeconds = persistedState.initialDurationMinutes * 60,
                        secondsLeft = remaining,
                        secondsElapsed = 0
                    )
                } else {
                    // Timer completed in background while app was dead
                    val duration = persistedState.initialDurationMinutes * 60
                    viewModel.logFocusSession(
                        habitId = persistedState.habitId.takeIf { it > 0 },
                        durationSeconds = duration,
                        details = null
                    )
                    showSuccessDialog = true
                    viewModel.updateFocusState(
                        persistedState.copy(
                            isRunning = false,
                            pausedSeconds = persistedState.initialDurationMinutes * 60
                        )
                    )
                }
            } else {
                // STOPWATCH
                val elapsed = persistedState.pausedSeconds + diff
                FocusService.startService(
                    context = context,
                    mode = "STOPWATCH",
                    habitTitle = persistedState.habitTitle,
                    durationSeconds = 0,
                    secondsLeft = 0,
                    secondsElapsed = elapsed
                )
            }
        }
    }

    // Handle timer completion dialog when service ticks to 0
    LaunchedEffect(isServiceRunning, serviceSecondsLeft) {
        if (isServiceRunning && serviceMode == "TIMER" && serviceSecondsLeft == 0) {
            showSuccessDialog = true
            val defaultSecs = initialDurationMinutes * 60
            viewModel.updateFocusState(
                persistedState.copy(
                    isRunning = false,
                    pausedSeconds = defaultSecs
                )
            )
            FocusService.stopService(context)
            
            // Play Ringtone default chime
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Trigger soft dual-pulse vibration pattern
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.let {
                    if (it.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val timings = longArrayOf(0, 150, 100, 150)
                            val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                            it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(longArrayOf(0, 150, 100, 150), -1)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    val glowTransition = rememberInfiniteTransition(label = "focusGlow")
    val glowScale by glowTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRunning) 1.18f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focusGlowScale"
    )
    val glowAlpha by glowTransition.animateFloat(
        initialValue = if (isRunning) 0.22f else 0.12f,
        targetValue = if (isRunning) 0.45f else 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focusGlowAlpha"
    )

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
                                val defaultSecs = if (mode == "TIMER") persistedState.initialDurationMinutes * 60 else 0
                                viewModel.updateFocusState(
                                    persistedState.copy(
                                        mode = mode,
                                        pausedSeconds = defaultSecs
                                    )
                                )
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
                    .size(272.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(248.dp)
                        .graphicsLayer {
                            scaleX = glowScale
                            scaleY = glowScale
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    parsedColor.copy(alpha = glowAlpha),
                                    parsedColor.copy(alpha = glowAlpha * 0.35f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !isServiceRunning && focusMode == "TIMER") {
                            showCustomDurationDialog = true
                        }
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
                        if (isServiceRunning) {
                            val intent = android.content.Intent(context, FocusService::class.java).apply {
                                action = FocusService.ACTION_RESET
                            }
                            context.startService(intent)
                        } else {
                            val defaultSecs = if (focusMode == "TIMER") initialDurationMinutes * 60 else 0
                            viewModel.updateFocusState(
                                persistedState.copy(
                                    isRunning = false,
                                    pausedSeconds = defaultSecs
                                )
                            )
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
                val isTicking = isServiceRunning && !serviceIsPaused
                Button(
                    onClick = {
                        if (isServiceRunning) {
                            if (serviceIsPaused) {
                                val intent = android.content.Intent(context, FocusService::class.java).apply {
                                    action = FocusService.ACTION_RESUME
                                }
                                context.startService(intent)
                            } else {
                                val intent = android.content.Intent(context, FocusService::class.java).apply {
                                    action = FocusService.ACTION_PAUSE
                                }
                                context.startService(intent)
                            }
                        } else {
                            val currentSecs = if (focusMode == "TIMER") secondsLeft else secondsElapsed
                            val now = System.currentTimeMillis()
                            viewModel.updateFocusState(
                                persistedState.copy(
                                    isRunning = true,
                                    pausedSeconds = currentSecs,
                                    baseTimestamp = now,
                                    habitTitle = selectedHabit?.title ?: ""
                                )
                            )
                            FocusService.startService(
                                context = context,
                                mode = focusMode,
                                habitTitle = selectedHabit?.title ?: "",
                                durationSeconds = initialDurationMinutes * 60,
                                secondsLeft = if (focusMode == "TIMER") currentSecs else 0,
                                secondsElapsed = if (focusMode == "STOPWATCH") currentSecs else 0
                            )
                        }
                    },
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
                            imageVector = if (isTicking) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (isTicking) "Pause" else "Start",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTicking) "Pause" else "Start",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                // Finish / Complete Button (Visible in Stopwatch mode or early stop for timer)
                val isTimerEarly = isServiceRunning && focusMode == "TIMER" && secondsLeft < (initialDurationMinutes * 60)
                val isStopwatchLogged = focusMode == "STOPWATCH" && secondsElapsed > 0
                if (isTimerEarly || isStopwatchLogged) {
                    IconButton(
                        onClick = {
                            FocusService.stopService(context)
                            val duration = if (focusMode == "TIMER") {
                                initialDurationMinutes * 60 - secondsLeft
                            } else {
                                secondsElapsed
                            }
                            viewModel.logFocusSession(
                                habitId = selectedHabit?.id,
                                durationSeconds = duration,
                                details = null
                            )
                            val defaultSecs = if (focusMode == "TIMER") initialDurationMinutes * 60 else 0
                            viewModel.updateFocusState(
                                persistedState.copy(
                                    isRunning = false,
                                    pausedSeconds = defaultSecs
                                )
                            )
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
                            val presets = listOf(1, 15, 25, 45, 60)
                            val isCustomSelected = initialDurationMinutes !in presets

                            presets.forEach { mins ->
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
                                        .clickable(enabled = !isServiceRunning) {
                                            viewModel.updateFocusState(
                                                persistedState.copy(
                                                    initialDurationMinutes = mins,
                                                    pausedSeconds = mins * 60
                                                )
                                            )
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

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCustomSelected) parsedColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    )
                                    .clickable(enabled = !isServiceRunning) {
                                        showCustomDurationDialog = true
                                    }
                            ) {
                                Text(
                                    text = if (isCustomSelected) "${initialDurationMinutes}m" else "Custom",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isCustomSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                .clickable(enabled = !isServiceRunning) { showHabitsDropdown = true }
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
                            if (incompleteHabits.isEmpty()) {
                                val msg = if (allHabits.isEmpty()) "No habits available" else "All habits completed for today!"
                                DropdownMenuItem(
                                    text = { Text(msg, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = { showHabitsDropdown = false }
                                )
                            } else {
                                incompleteHabits.forEach { habit ->
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
                                            viewModel.updateFocusState(
                                                persistedState.copy(
                                                    habitId = habit.id,
                                                    habitTitle = habit.title
                                                )
                                            )
                                            showHabitsDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = parsedColor)
                    ) {
                        Text("Log Completion", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            showSuccessDialog = false
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
                    }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showCustomDurationDialog) {
        var customMinutesText by remember { mutableStateOf(initialDurationMinutes.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            title = {
                Text(
                    text = "Custom Duration",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter timer duration in minutes (1 - 999):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                customMinutesText = input
                                isError = input.toIntOrNull()?.let { it < 1 } ?: true
                            }
                        },
                        isError = isError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        placeholder = { Text("e.g. 30") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            text = "Please enter a valid number between 1 and 999",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customMinutesText.toIntOrNull()
                        if (mins != null && mins in 1..999) {
                            viewModel.updateFocusState(
                                persistedState.copy(
                                    initialDurationMinutes = mins,
                                    pausedSeconds = mins * 60
                                )
                            )
                            showCustomDurationDialog = false
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = parsedColor)
                ) {
                    Text("Set", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCustomDurationDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
