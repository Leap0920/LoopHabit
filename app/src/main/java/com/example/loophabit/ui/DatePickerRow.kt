package com.example.loophabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DatePickerRow(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    completionDates: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val today = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val dates = remember {
        val calendar = Calendar.getInstance()
        calendar.time = today.time
        val result = mutableListOf<Pair<String, Long>>()

        // Go back 365 days
        for (i in 0..365) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val displayInfo = Triple(
                SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time).uppercase(),
                calendar.get(Calendar.DAY_OF_MONTH).toString(),
                SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time).uppercase()
            )
            result.add(Pair(dateStr, calendar.timeInMillis))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        result.reversed()
    }

    val listState = rememberLazyListState()

    // Auto-scroll to today
    LaunchedEffect(Unit) {
        val todayIndex = dates.indexOfFirst { it.first == selectedDate }
        if (todayIndex >= 0) {
            listState.animateScrollToItem(todayIndex, scrollOffset = -200)
        }
    }

    // Pink/salmon accent for selected date (matching reference UI)
    val selectedPink = Color(0xFFF5B7B1)
    // Green accent for dates with completion records
    val hasRecordColor = Color(0xFF81C784)

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(dates, key = { it.first }) { (dateStr, _) ->
            val isSelected = dateStr == selectedDate
            val isToday = dateStr == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)
            val hasRecord = dateStr in completionDates
            val date = remember(dateStr) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            }
            val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date!!).uppercase()
            val day = SimpleDateFormat("d", Locale.getDefault()).format(date)
            val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase()

            val onSurface = MaterialTheme.colorScheme.onSurface

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(selectedPink)
                        } else if (isToday) {
                            Modifier.border(1.5.dp, selectedPink, RoundedCornerShape(10.dp))
                        } else if (hasRecord) {
                            Modifier.background(hasRecordColor.copy(alpha = 0.25f))
                                .border(1.dp, hasRecordColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        } else {
                            Modifier.background(Color(0xFF2A2A2A))
                        }
                    )
                    .clickable { onDateSelected(dateStr) }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            ) {
                Text(
                    text = month,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFF333333) else if (hasRecord) hasRecordColor else onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = day,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF333333) else if (hasRecord) hasRecordColor else onSurface
                )
                Text(
                    text = dayOfWeek,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color(0xFF333333) else if (hasRecord) hasRecordColor else onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
