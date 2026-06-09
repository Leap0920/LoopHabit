package com.example.loophabit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.loophabit.ui.parseHexColorOrDefault
import com.example.loophabit.ui.theme.*

// Curated color palette
val ColorPaletteList = listOf(
    "#E57373", "#F06292", "#BA68C8", "#9575CD",
    "#7986CB", "#64B5F6", "#4FC3F7", "#4DD0E1",
    "#4DB6AC", "#81C784", "#AED581", "#DCE775",
    "#FFD54F", "#FFB74D", "#FF8A65", "#A1887F"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAddHabit: (title: String, colorHex: String, targetDaysPerWeek: Int, isNumerical: Boolean, numericalGoal: Double, numericalUnit: String, daysOfWeekPattern: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ColorPaletteList[0]) }
    var targetDaysPerWeek by remember { mutableStateOf("7") }
    var isNumerical by remember { mutableStateOf(false) }
    var numericalGoal by remember { mutableStateOf("") }
    var numericalUnit by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Habit",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Habit name
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Color selection
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(ColorPaletteList) { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColorOrDefault(color))
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = if (selectedColor == color)
                                        MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                // Target days per week
                OutlinedTextField(
                    value = targetDaysPerWeek,
                    onValueChange = { targetDaysPerWeek = it },
                    label = { Text("Target Days per Week") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Numerical habit toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Numerical Habit",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isNumerical,
                        onCheckedChange = { isNumerical = it }
                    )
                }

                // Numerical goal fields
                if (isNumerical) {
                    OutlinedTextField(
                        value = numericalGoal,
                        onValueChange = { numericalGoal = it },
                        label = { Text("Goal Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = numericalUnit,
                        onValueChange = { numericalUnit = it },
                        label = { Text("Unit (e.g., glasses, pages)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Day selection
                Text(
                    text = "Active Days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                        val dayNumber = index + 1
                        val isSelected = dayNumber in selectedDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    selectedDays - dayNumber
                                } else {
                                    selectedDays + dayNumber
                                }
                            },
                            label = { Text(day) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAddHabit(
                            title,
                            selectedColor,
                            targetDaysPerWeek.toIntOrNull() ?: 7,
                            isNumerical,
                            numericalGoal.toDoubleOrNull() ?: 0.0,
                            numericalUnit.ifBlank { "" },
                            selectedDays.sorted().joinToString(",")
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add Habit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
