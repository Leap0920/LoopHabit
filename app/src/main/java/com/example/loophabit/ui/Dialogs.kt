package com.example.loophabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.loophabit.LoopHabitApp
import com.example.loophabit.data.Habit
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Curated pastel/vibrant HSL colors for premium aesthetic
val ColorPaletteList = listOf(
    "#EF476F", // Soft Red/Rose
    "#FFD166", // Mustard/Amber
    "#06D6A0", // Mint/Emerald
    "#118AB2", // Ocean Blue
    "#8338EC", // Purple
    "#FF9F1C"  // Vivid Orange
)

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ColorPaletteList[0]) }
    var targetDaysPerWeek by remember { mutableStateOf(7) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "New Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is your habit?") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Weekly Goal frequency: $targetDaysPerWeek days / week",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = targetDaysPerWeek.toFloat(),
                    onValueChange = { targetDaysPerWeek = it.roundToInt() },
                    valueRange = 1f..7f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pick Color",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ColorPaletteList.forEach { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .padding(4.dp)
                        ) {
                            if (selectedColor == colorHex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onAdd(title, selectedColor, targetDaysPerWeek) },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun ManageHabitsDialog(
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onDelete: (Habit) -> Unit,
    onSelectHabit: (Habit) -> Unit,
    app: LoopHabitApp
) {
    val darkModeEnabled by app.preferences.darkModeEnabledFlow.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxHeight(0.6f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Manage Habits",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Dark Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dark Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = darkModeEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { app.preferences.setDarkModeEnabled(enabled) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                if (habits.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No habits to manage.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(habits) { habit ->
                            val parsedColor = remember(habit.colorHex) {
                                try {
                                    Color(android.graphics.Color.parseColor(habit.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFF8338EC)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onDismiss()
                                            onSelectHabit(habit)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(parsedColor)
                                    )
                                    Text(
                                        text = habit.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(habit) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Habit",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss, shape = RoundedCornerShape(16.dp)) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailsDialog(
    habit: Habit,
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val completions by viewModel.getCompletionsForHabit(habit.id).collectAsState(initial = emptyList())
    val completionDates = remember(completions) { completions.map { it.date } }
    val (currentStreak, bestStreak) = remember(completionDates) {
        viewModel.calculateStreaks(completionDates)
    }

    val parsedColor = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF8338EC)
        }
    }

    var selectedDetailTab by remember { mutableStateOf("MONTH") } // MONTH, YEAR, INSIGHTS
    var showNoteEditDialogForDate by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = habit.title,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Habit Insights & Log History",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Secondary Tab Row
                TabRow(
                    selectedTabIndex = when (selectedDetailTab) {
                        "MONTH" -> 0
                        "YEAR" -> 1
                        else -> 2
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedDetailTab == "MONTH",
                        onClick = { selectedDetailTab = "MONTH" },
                        text = { Text("Month", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedDetailTab == "YEAR",
                        onClick = { selectedDetailTab = "YEAR" },
                        text = { Text("Year Grid", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedDetailTab == "INSIGHTS",
                        onClick = { selectedDetailTab = "INSIGHTS" },
                        text = { Text("Logs & Stats", fontWeight = FontWeight.Bold) }
                    )
                }

                when (selectedDetailTab) {
                    "MONTH" -> {
                        MonthlyCalendarTab(
                            completions = completions,
                            parsedColor = parsedColor,
                            onDayClick = { dateStr -> showNoteEditDialogForDate = dateStr }
                        )
                    }
                    "YEAR" -> {
                        YearAtAGlanceGrid(
                            completions = completions,
                            parsedColor = parsedColor,
                            onDayClick = { dateStr -> showNoteEditDialogForDate = dateStr }
                        )
                    }
                    else -> {
                        InsightsStatsTab(
                            habit = habit,
                            completions = completions,
                            currentStreak = currentStreak,
                            bestStreak = bestStreak,
                            parsedColor = parsedColor
                        )
                    }
                }
            }
        }
    }

    if (showNoteEditDialogForDate != null) {
        val dateStr = showNoteEditDialogForDate!!
        val existingCompletion = completions.find { it.date == dateStr }
        val wasCompleted = existingCompletion != null
        val existingNotes = existingCompletion?.notes ?: ""

        NoteEditDialog(
            dateStr = dateStr,
            wasCompleted = wasCompleted,
            existingNotes = existingNotes,
            onDismiss = { showNoteEditDialogForDate = null },
            onSave = { isCompleted, notesText ->
                viewModel.toggleHabitCompletionForDate(habit.id, dateStr, wasCompleted, if (isCompleted) notesText else null)
                // If it was already completed and remains completed, but they edited notes:
                if (wasCompleted && isCompleted) {
                    viewModel.saveCompletionNote(habit.id, dateStr, notesText.ifBlank { null })
                }
                showNoteEditDialogForDate = null
            }
        )
    }
}

@Composable
fun NoteEditDialog(
    dateStr: String,
    wasCompleted: Boolean,
    existingNotes: String,
    onDismiss: () -> Unit,
    onSave: (Boolean, String) -> Unit
) {
    var isCompleted by remember { mutableStateOf(wasCompleted) }
    var notesText by remember { mutableStateOf(existingNotes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Log for $dateStr",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = { isCompleted = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Marked as Completed",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Journal Notes / Progress Memo") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(isCompleted, notesText) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
