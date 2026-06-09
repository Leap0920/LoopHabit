package com.example.loophabit.ui

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * iOS actual: Parse hex color string to ARGB Long.
 * Handles #RGB, #RRGGBB, #AARRGGBB formats.
 */
actual fun parseHexColorArgb(hexString: String): Long {
    val hex = hexString.trimStart('#')
    return when (hex.length) {
        3 -> {
            // #RGB → #RRGGBB
            val r = hex[0].toString().repeat(2)
            val g = hex[1].toString().repeat(2)
            val b = hex[2].toString().repeat(2)
            (0xFF shl 24) or (r.toLong(16) shl 16) or (g.toLong(16) shl 8) or b.toLong(16)
        }
        6 -> {
            // #RRGGBB → #FFRRGGBB
            (0xFF shl 24) or (hex.substring(0, 2).toLong(16) shl 16) or
                    (hex.substring(2, 4).toLong(16) shl 8) or hex.substring(4, 6).toLong(16)
        }
        8 -> {
            // #AARRGGBB
            hex.toLong(16)
        }
        else -> throw IllegalArgumentException("Invalid hex color: $hexString")
    }
}

/**
 * iOS actual: Get today's date using kotlinx-datetime.
 */
actual fun todayLocalDate(): LocalDate {
    return Clock.System.todayIn(TimeZone.currentSystemDefault())
}

/**
 * iOS actual: Get month display name.
 * Uses a simple lookup since NSDateFormatter requires platform interop.
 */
actual fun getMonthDisplayName(monthNumber: Int): String {
    return when (monthNumber) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> throw IllegalArgumentException("Invalid month: $monthNumber")
    }
}
