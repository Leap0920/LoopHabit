package com.example.loophabit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.*

// (P6) Lazy date strip - dates are computed on the fly from their index.
// No pre-generation of 366 items. The LazyRow starts at the last index (today)
// and only composes the ~8 visible items. Scrolling left reveals past dates
// one at a time; they're computed instantly from the index.
// This eliminates the initial load spike and the auto-scroll animation.

private data class DateItem(
    val dateStr: String,
    val monthLabel: String,
    val dayLabel: String,
    val dayOfWeekLabel: String,
    val isToday: Boolean
)

// Total range: 365 days back. But they're NEVER all composed at once -
// LazyRow only composes visible + recycled items.
private const val DATE_RANGE = 365

@Composable
fun DatePickerRow(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    completionDates: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthFmt = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val dowFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val todayStr = remember { dateFmt.format(Date()) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = DATE_RANGE)

    // (F4) theme tokens
    val selectedColor = MaterialTheme.colorScheme.primary
    val selectedContentColor = MaterialTheme.colorScheme.onPrimary
    val hasRecordColor = MaterialTheme.colorScheme.tertiary
    val defaultBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val onSurface = MaterialTheme.colorScheme.onSurface

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(DATE_RANGE + 1) { index ->
            // index 0 = 365 days ago, index DATE_RANGE = today
            val daysAgo = DATE_RANGE - index
            val item = remember(index, todayStr) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -daysAgo)
                }
                DateItem(
                    dateStr = dateFmt.format(cal.time),
                    monthLabel = monthFmt.format(cal.time).uppercase(),
                    dayLabel = dayFmt.format(cal.time),
                    dayOfWeekLabel = dowFmt.format(cal.time).uppercase(),
                    isToday = daysAgo == 0
                )
            }

            val isSelected = item.dateStr == selectedDate
            val hasRecord = item.dateStr in completionDates

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(selectedColor)
                        } else if (item.isToday) {
                            Modifier.border(1.5.dp, selectedColor, RoundedCornerShape(10.dp))
                        } else if (hasRecord) {
                            Modifier.background(hasRecordColor.copy(alpha = 0.2f))
                                .border(1.dp, hasRecordColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        } else {
                            Modifier.background(defaultBg)
                        }
                    )
                    .clickable { onDateSelected(item.dateStr) }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            ) {
                Text(
                    text = item.monthLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) selectedContentColor else if (hasRecord) hasRecordColor else onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = item.dayLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) selectedContentColor else if (hasRecord) hasRecordColor else onSurface
                )
                Text(
                    text = item.dayOfWeekLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) selectedContentColor else if (hasRecord) hasRecordColor else onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
