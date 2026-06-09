package com.example.loophabit.ui

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Parse a hex color string (#RRGGBB or #AARRGGBB) to a Compose Color.
 * Falls back to purple if parsing fails.
 */
fun parseHexColorOrDefault(hexString: String, default: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF8338EC)): androidx.compose.ui.graphics.Color {
    return try {
        val argb = parseHexColorArgb(hexString)
        androidx.compose.ui.graphics.Color(argb)
    } catch (_: Exception) {
        default
    }
}

/**
 * Platform-specific: Parse hex string to ARGB Long.
 */
expect fun parseHexColorArgb(hexString: String): Long

/**
 * Platform-specific: Get today's date as LocalDate.
 */
expect fun todayLocalDate(): LocalDate

/**
 * Get the day-of-week number (1=Monday, 7=Sunday).
 */
fun getDayOfWeekNumber(date: LocalDate): Int = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
    DayOfWeek.SUNDAY -> 7
}

/**
 * Platform-specific: Get display name for a month number (1-12).
 */
expect fun getMonthDisplayName(monthNumber: Int): String

/**
 * Format a date string "yyyy-MM-dd".
 */
fun formatDateString(year: Int, month: Int, day: Int): String {
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

fun formatYearMonthString(year: Int, month: Int): String {
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}"
}

/**
 * Simple YearMonth helper for commonMain (replaces java.time.YearMonth).
 */
data class SimpleYearMonth(val year: Int, val monthNumber: Int) {
    fun atDay(day: Int): LocalDate = LocalDate(year, monthNumber, day)

    fun lengthOfMonth(): Int {
        return when (monthNumber) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> throw IllegalArgumentException("Invalid month: $monthNumber")
        }
    }

    fun isValidDay(day: Int): Boolean = day in 1..lengthOfMonth()

    fun minusMonths(months: Int): SimpleYearMonth {
        var totalMonths = (year * 12 + (monthNumber - 1)) - months
        val newYear = totalMonths / 12
        val newMonth = (totalMonths % 12) + 1
        return SimpleYearMonth(newYear, if (newMonth <= 0) newMonth + 12 else newMonth)
    }

    fun plusMonths(months: Int): SimpleYearMonth {
        var totalMonths = (year * 12 + (monthNumber - 1)) + months
        val newYear = totalMonths / 12
        val newMonth = (totalMonths % 12) + 1
        return SimpleYearMonth(newYear, if (newMonth <= 0) newMonth + 12 else newMonth)
    }

    companion object {
        fun now(): SimpleYearMonth {
            val today = todayLocalDate()
            return SimpleYearMonth(today.year, today.monthNumber)
        }

        fun of(year: Int, month: Int): SimpleYearMonth = SimpleYearMonth(year, month)
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
