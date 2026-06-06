package com.example.loophabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import com.example.loophabit.data.Habit
import com.example.loophabit.data.HabitCompletion
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightsDashboard(
    viewModel: HabitViewModel,
    onSelectHabit: (Habit) -> Unit
) {
    val allHabits by viewModel.allHabits.collectAsState()
    val allCompletions by viewModel.allCompletions.collectAsState()

    var animateCharts by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        animateCharts = true
    }
    val chartProgress by animateFloatAsState(
        targetValue = if (animateCharts) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "chartProgress"
    )

    // 1. Calculations:
    val today = java.time.LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong()) // Monday
    val currentWeekDates = (0..6).map { startOfWeek.plusDays(it.toLong()).toString() }

    val totalCompletions = allCompletions.size

    // Last 30 days overall consistency metric
    val last30Days = (0 until 30).map { today.minusDays(it.toLong()).toString() }
    val completionsInLast30 = allCompletions.count { last30Days.contains(it.date) }
    val maxPossibleCompletions = allHabits.size * 30
    val overallConsistency = if (maxPossibleCompletions > 0) {
        (completionsInLast30.toFloat() / maxPossibleCompletions.toFloat() * 100).roundToInt()
    } else {
        0
    }

    // Streaks Leaders mapping
    val habitStreaks = remember(allHabits, allCompletions) {
        allHabits.map { habit ->
            val dates = allCompletions.filter { it.habitId == habit.id }.map { it.date }
            val (current, best) = viewModel.calculateStreaks(dates)
            Triple(habit, current, best)
        }.sortedByDescending { it.second }
    }

    // 14-day history activity list
    val dateList = remember {
        (0 until 14).map { today.minusDays(it.toLong()) }.reversed()
    }
    val completionsPerDay = remember(allCompletions, dateList) {
        dateList.map { date ->
            val dateStr = date.toString()
            val count = allCompletions.count { it.date == dateStr }
            Pair(date, count)
        }
    }

    // Day of Week Frequency Heatmap
    val weekdayCompletions = remember(allCompletions) {
        val counts = IntArray(7) // Mon=0 .. Sun=6
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        for (completion in allCompletions) {
            try {
                val ld = java.time.LocalDate.parse(completion.date, formatter)
                counts[ld.dayOfWeek.value - 1]++
            } catch (e: Exception) {
                // Ignore parsing exceptions
            }
        }
        counts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Performance Dashboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track your overall performance & statistics",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Consistency & Total Completion Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Consistency Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Consistency (30d)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$overallConsistency%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Total completions card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Completed",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$totalCompletions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 14-day history bar chart
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Activity (Last 14 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                val maxVal = (completionsPerDay.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    completionsPerDay.forEach { (date, count) ->
                        val ratio = (count.toFloat() / maxVal.toFloat()) * chartProgress
                        val barHeight = (ratio * 70).coerceAtLeast(4f).dp
                        val isCurrentDay = date == today

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isCurrentDay) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Weekday Productivity Distribution
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Consistency by Day of Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                val daysOfWeekNames = listOf("M", "T", "W", "T", "F", "S", "S")
                val maxCount = weekdayCompletions.maxOrNull()?.coerceAtLeast(1) ?: 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekdayCompletions.forEachIndexed { idx, count ->
                        val ratio = (count.toFloat() / maxCount.toFloat()) * chartProgress
                        val barHeight = (ratio * 80).coerceAtLeast(4f).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = daysOfWeekNames[idx],
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Habit performance leaderboard
        Text(
            text = "Habit Insights List",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (habitStreaks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No habits to show.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    habitStreaks.forEach { (habit, current, best) ->
                        val parsedColor = remember(habit.colorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(habit.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF8338EC)
                            }
                        }

                        // Completions this week
                        val dates = allCompletions.filter { it.habitId == habit.id }.map { it.date }
                        val completionsThisWeek = dates.count { currentWeekDates.contains(it) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHabit(habit) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = habit.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Goal: $completionsThisWeek/${habit.targetDaysPerWeek} this week",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (current > 0) {
                                    Text(
                                        text = "🔥 $current d",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = parsedColor
                                    )
                                }
                                Text(
                                    text = "🏆 $best d",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MonthlyCalendarTab(
    completions: List<HabitCompletion>,
    parsedColor: Color,
    onDayClick: (String) -> Unit
) {
    var currentMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    val completionDates = remember(completions) { completions.map { it.date } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Calendar Controller Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous Month")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekday Headers
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("M", "T", "W", "T", "F", "S", "S")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid Days
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val daysInMonth = currentMonth.lengthOfMonth()
        val totalCells = ((daysInMonth + (firstDayOfWeek - 1) + 6) / 7) * 7

        var cellCount = 0
        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until totalCells / 7) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayNum = cellCount - (firstDayOfWeek - 2)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val dateStr = String.format(Locale.US, "%04d-%02d-%02d", currentMonth.year, currentMonth.monthValue, dayNum)
                                val completion = completions.find { it.date == dateStr }
                                val isCompleted = completion != null
                                val hasNotes = !completion?.notes.isNullOrBlank()

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) parsedColor.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isCompleted) parsedColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                        .clickable { onDayClick(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 13.sp,
                                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCompleted) parsedColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hasNotes) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 3.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(parsedColor)
                                        )
                                    }
                                }
                            }
                        }
                        cellCount++
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Month Journal Notes list
        val monthCompletionsWithNotes = remember(completions, currentMonth) {
            completions.filter {
                it.date.startsWith(String.format(Locale.US, "%04d-%02d", currentMonth.year, currentMonth.monthValue)) &&
                        !it.notes.isNullOrBlank()
            }.sortedByDescending { it.date }
        }

        Text(
            text = "Month Diary Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (monthCompletionsWithNotes.isEmpty()) {
            Text(
                text = "No journal notes logged in this month yet. Tap any day on the calendar to add notes!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthCompletionsWithNotes.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.date,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = parsedColor
                                )
                                Text(
                                    text = "✓ Completed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06D6A0)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.notes ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearAtAGlanceGrid(
    completions: List<HabitCompletion>,
    parsedColor: Color,
    onDayClick: (String) -> Unit
) {
    var selectedYear by remember { mutableStateOf(java.time.LocalDate.now().year) }
    val completionDates = remember(completions) { completions.map { it.date } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Year controller header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selectedYear-- }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Prev Year")
            }
            Text(
                text = "Year $selectedYear",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { selectedYear++ }) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next Year")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Label details help
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(parsedColor))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Done", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.size(8.dp).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid Container with horizontal scrolling in case of small devices
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(12.dp)
            ) {
                // Header Month Abbreviations: Day, J, F, M, A, M, J, J, A, S, O, N, D
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left corner offset
                    Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                        Text("", fontSize = 10.sp)
                    }

                    val monthsInitials = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                    monthsInitials.forEach { mInitial ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(21.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mInitial,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Rows for day numbers: 1 to 31
                for (dayNum in 1..31) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left index number
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(21.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "$dayNum",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        // 12 months for this day
                        for (monthNum in 1..12) {
                            val isValidDay = try {
                                java.time.YearMonth.of(selectedYear, monthNum).isValidDay(dayNum)
                            } catch (e: Exception) {
                                false
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                                    .size(21.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isValidDay) {
                                    val dateStr = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, monthNum, dayNum)
                                    val isCompleted = completionDates.contains(dateStr)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted) parsedColor
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            )
                                            .clickable { onDayClick(dateStr) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun InsightsStatsTab(
    habit: Habit,
    completions: List<HabitCompletion>,
    currentStreak: Int,
    bestStreak: Int,
    parsedColor: Color
) {
    val totalCompletions = completions.size

    // Weekly Goal Counter calculation
    val today = java.time.LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong()) // Monday
    val currentWeekDates = (0..6).map { startOfWeek.plusDays(it.toLong()).toString() }
    val completionsThisWeek = completions.count { currentWeekDates.contains(it.date) }

    val weeklyGoalProgress = if (habit.targetDaysPerWeek > 0) {
        completionsThisWeek.toFloat() / habit.targetDaysPerWeek.toFloat()
    } else {
        0f
    }

    var animateProgress by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        animateProgress = true
    }
    val animatedProgressVal by animateFloatAsState(
        targetValue = if (animateProgress) weeklyGoalProgress else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "progressAnim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stats Cards Dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current Streak Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔥 Streak", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$currentStreak d", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = parsedColor)
                }
            }

            // Best Streak Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆 Best", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$bestStreak d", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Total completions card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✓ Total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalCompletions", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Target Goal Progress Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = parsedColor.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Target Progress",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Goal is ${habit.targetDaysPerWeek} days a week. You completed $completionsThisWeek days so far.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgressVal.coerceIn(0f, 1f) },
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 5.dp,
                        color = parsedColor,
                        trackColor = parsedColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "$completionsThisWeek/${habit.targetDaysPerWeek}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History logs list
        Text(
            text = "Chronological History Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.height(10.dp))

        val notesLogs = completions.filter { !it.notes.isNullOrBlank() }.sortedByDescending { it.date }
        if (notesLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notes saved for this habit yet. Write some diaries on the Calendar tab!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notesLogs.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.date,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = parsedColor
                                )
                                Text(
                                    text = "Completed",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.notes ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
