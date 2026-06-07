package com.example.loophabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onAdd: (String, String, Int, Boolean, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ColorPaletteList[0]) }
    var targetDaysPerWeek by remember { mutableStateOf(7) }

    var isNumerical by remember { mutableStateOf(false) }
    var numericalGoal by remember { mutableStateOf("") }
    var numericalUnit by remember { mutableStateOf("") }

    var repeatType by remember { mutableStateOf("EVERYDAY") } // EVERYDAY, WEEKDAYS, CUSTOM
    var customDays by remember { mutableStateOf(listOf(true, true, true, true, true, true, true)) } // M, T, W, T, F, S, S

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

                // Repeat Schedule Options
                Text(
                    text = "Repeat Schedule",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EVERYDAY" to "Everyday", "WEEKDAYS" to "Weekdays", "CUSTOM" to "Custom").forEach { (type, label) ->
                        val isSelected = repeatType == type
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(android.graphics.Color.parseColor(selectedColor))
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { repeatType = type }
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (repeatType == "CUSTOM") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val weekdayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                        customDays.forEachIndexed { index, isSelected ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(android.graphics.Color.parseColor(selectedColor)).copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(android.graphics.Color.parseColor(selectedColor))
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        customDays = customDays.toMutableList().apply { this[index] = !isSelected }
                                    }
                            ) {
                                Text(
                                    text = weekdayNames[index],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(android.graphics.Color.parseColor(selectedColor))
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numerical Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set Numerical Goal",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isNumerical,
                        onCheckedChange = { isNumerical = it }
                    )
                }

                if (isNumerical) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = numericalGoal,
                            onValueChange = { numericalGoal = it },
                            label = { Text("Target Goal") },
                            placeholder = { Text("e.g. 3000") },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = numericalUnit,
                            onValueChange = { numericalUnit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("e.g. ml") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

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
                        onClick = {
                            if (title.isNotBlank()) {
                                val goal = numericalGoal.toDoubleOrNull() ?: 0.0
                                val pattern = when (repeatType) {
                                    "EVERYDAY" -> "1111111"
                                    "WEEKDAYS" -> "1111100"
                                    else -> customDays.map { if (it) '1' else '0' }.joinToString("")
                                }
                                onAdd(title, selectedColor, targetDaysPerWeek, isNumerical, goal, numericalUnit, pattern)
                            }
                        },
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
fun SettingsDialog(
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onDelete: (Habit) -> Unit,
    onSelectHabit: (Habit) -> Unit,
    viewModel: HabitViewModel,
    app: LoopHabitApp,
    onImportClick: () -> Unit
) {
    val darkModeEnabled by app.preferences.darkModeEnabledFlow.collectAsState(initial = false)
    val autoBackupHours by viewModel.autoBackupInterval.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showResetConfirm by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val backupOptions = listOf(
        0 to "Disabled",
        6 to "6 Hours",
        12 to "12 Hours",
        24 to "24 Hours (Daily)",
        168 to "7 Days (Weekly)"
    )
    val currentBackupLabel = backupOptions.find { it.first == autoBackupHours }?.second ?: "Disabled"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adjust background theme style",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = darkModeEnabled,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch { app.preferences.setDarkModeEnabled(enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    // Auto-Backup Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Backup frequency",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Schedule background backup logs",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                                        .clickable { dropdownExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = currentBackupLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    backupOptions.forEach { (hours, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                viewModel.setAutoBackupInterval(hours)
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Backup & Restore Actions
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Local Backup & Restore",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.exportData(context) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Export Data", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onImportClick,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Import Data", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Reset Data Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Reset App / Logout",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Permanently wipe all habits & history",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                            TextButton(
                                onClick = { showResetConfirm = true }
                            ) {
                                Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Habits List section
                    item {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Manage Habits (${habits.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (habits.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No habits to manage.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
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
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
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

    if (showResetConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all your habits, completed checks, and focus session records. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        viewModel.resetAllData {
                            Toast.makeText(context, "All app data reset successfully.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
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
        viewModel.calculateStreaks(completionDates, habit.daysOfWeekPattern)
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
            isNumerical = habit.isNumerical,
            numericalGoal = habit.numericalGoal,
            numericalUnit = habit.numericalUnit,
            existingValue = existingCompletion?.value ?: 0.0,
            onDismiss = { showNoteEditDialogForDate = null },
            onSave = { isCompleted, notesText, value ->
                viewModel.toggleHabitCompletionForDate(habit.id, dateStr, wasCompleted, if (isCompleted) notesText else null, value)
                // If it was already completed and remains completed, but they edited notes:
                if (wasCompleted && isCompleted) {
                    viewModel.saveCompletionNote(habit.id, dateStr, notesText.ifBlank { null }, value)
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
    isNumerical: Boolean = false,
    numericalGoal: Double = 0.0,
    numericalUnit: String = "",
    existingValue: Double = 0.0,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, Double) -> Unit
) {
    var isCompleted by remember { mutableStateOf(wasCompleted) }
    var notesText by remember { mutableStateOf(existingNotes) }
    var valueText by remember { mutableStateOf(if (existingValue > 0.0) existingValue.toString() else "") }

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

                if (isNumerical) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = valueText,
                        onValueChange = {
                            valueText = it
                            if (it.toDoubleOrNull() != null) {
                                isCompleted = true
                            }
                        },
                        label = { Text("Progress Value ($numericalUnit)") },
                        placeholder = { Text("Goal: $numericalGoal $numericalUnit") },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
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
                        onClick = {
                            val v = valueText.toDoubleOrNull() ?: 0.0
                            onSave(isCompleted, notesText, v)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun NumericalLogDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onLog: (Double) -> Unit
) {
    var valueStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Log ${habit.title}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Daily Goal: ${habit.numericalGoal} ${habit.numericalUnit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Logged value (${habit.numericalUnit})") },
                    placeholder = { Text("e.g. 2500") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                        onClick = {
                            val v = valueStr.toDoubleOrNull() ?: 0.0
                            onLog(v)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Log Value")
                    }
                }
            }
        }
    }
}

