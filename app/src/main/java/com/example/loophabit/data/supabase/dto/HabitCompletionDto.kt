package com.example.loophabit.data.supabase.dto

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Data Transfer Object representing a Habit Completion in Supabase.
 * Matches the 'habit_completions' table structure.
 */
data class HabitCompletionDto(
    @SerializedName("id") val id: UUID,
    @SerializedName("habit_id") val habitId: UUID,
    @SerializedName("date") val date: LocalDate,
    @SerializedName("notes") val notes: String?,
    @SerializedName("created_at") val createdAt: Instant
) {
    companion object {
        fun create(
            habitId: UUID,
            date: LocalDate,
            notes: String? = null
        ): HabitCompletionDto {
            return HabitCompletionDto(
                id = UUID.randomUUID(),
                habitId = habitId,
                date = date,
                notes = notes,
                createdAt = Instant.now()
            )
        }
    }
}