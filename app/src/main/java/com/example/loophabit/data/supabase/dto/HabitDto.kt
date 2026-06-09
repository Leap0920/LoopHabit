package com.example.loophabit.data.supabase.dto

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object representing a Habit in Supabase.
 * Uses String types for serialization safety with both Gson and kotlinx.serialization.
 */
@Serializable
data class HabitDto(
    val id: String,
    val user_id: String,
    val title: String,
    val color_hex: String,
    val created_at: String,
    val target_days_per_week: Int,
    val updated_at: String,
    val is_numerical: Boolean = false,
    val numerical_goal: Double = 0.0,
    val numerical_unit: String = "",
    val days_of_week_pattern: String = "1111111"
) {
    companion object {
        fun create(
            userId: UUID,
            title: String,
            colorHex: String,
            targetDaysPerWeek: Int = 7,
            isNumerical: Boolean = false,
            numericalGoal: Double = 0.0,
            numericalUnit: String = "",
            daysOfWeekPattern: String = "1111111"
        ): HabitDto {
            val now = Instant.now().toString()
            return HabitDto(
                id = UUID.randomUUID().toString(),
                user_id = userId.toString(),
                title = title,
                color_hex = colorHex,
                created_at = now,
                target_days_per_week = targetDaysPerWeek,
                updated_at = now,
                is_numerical = isNumerical,
                numerical_goal = numericalGoal,
                numerical_unit = numericalUnit,
                days_of_week_pattern = daysOfWeekPattern
            )
        }
    }
}
