package com.example.loophabit.data.supabase.dto

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Data Transfer Object representing a Habit Completion in Supabase.
 * Uses String types for serialization safety with both Gson and kotlinx.serialization.
 */
@Serializable
data class HabitCompletionDto(
    val id: String,
    val habit_id: String,
    val date: String,
    val notes: String? = null,
    val created_at: String,
    val value: Double = 0.0
) {
    companion object {
        fun create(
            habitId: UUID,
            date: LocalDate,
            notes: String? = null,
            value: Double = 0.0
        ): HabitCompletionDto {
            return HabitCompletionDto(
                id = UUID.randomUUID().toString(),
                habit_id = habitId.toString(),
                date = date.toString(),
                notes = notes,
                created_at = Instant.now().toString(),
                value = value
            )
        }
    }
}
