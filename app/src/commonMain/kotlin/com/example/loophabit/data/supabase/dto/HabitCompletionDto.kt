package com.example.loophabit.data.supabase.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            habitId: String,
            date: String,
            notes: String? = null,
            value: Double = 0.0
        ): HabitCompletionDto {
            return HabitCompletionDto(
                id = Uuid.random().toString(),
                habit_id = habitId,
                date = date,
                notes = notes,
                created_at = currentTimestamp(),
                value = value
            )
        }
    }
}
