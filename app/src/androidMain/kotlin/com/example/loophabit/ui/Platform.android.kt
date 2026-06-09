package com.example.loophabit.ui

import android.graphics.Color as AndroidColor
import kotlinx.datetime.LocalDate
import java.time.Month

/**
 * Android actual: Parse hex color string to ARGB Long using android.graphics.Color.parseColor.
 */
actual fun parseHexColorArgb(hexString: String): Long {
    return AndroidColor.parseColor(hexString).toLong()
}

/**
 * Android actual: Get today's date using java.time.
 */
actual fun todayLocalDate(): LocalDate {
    val today = java.time.LocalDate.now()
    return LocalDate(today.year, today.monthValue, today.dayOfMonth)
}

/**
 * Android actual: Get month display name using java.time.Month.
 */
actual fun getMonthDisplayName(monthNumber: Int): String {
    return Month.of(monthNumber).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())
}
