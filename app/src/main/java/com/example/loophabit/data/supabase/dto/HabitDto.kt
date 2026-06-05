package com.example.loophabit.data.supabase.dto

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object representing a Habit in Supabase.
 * Matches the 'habits' table structure.
 */
data class HabitDto(
    @SerializedName("id") val id: UUID,
    @SerializedName("user_id") val userId: UUID,
    @SerializedName("title") val title: String,
    @SerializedName("color_hex") val colorHex: String,
    @SerializedName("created_at") val createdAt: Instant,
    @SerializedName("target_days_per_week") val targetDaysPerWeek: Int,
    @SerializedName("updated_at") val updatedAt: Instant
) {
    companion object {
        fun create(
            userId: UUID,
            title: String,
            colorHex: String,
            targetDaysPerWeek: Int = 7
        ): HabitDto {
            val now = Instant.now()
            return HabitDto(
                id = UUID.randomUUID(),
                userId = userId,
                title = title,
                colorHex = colorHex,
                createdAt = now,
                targetDaysPerWeek = targetDaysPerWeek,
                updatedAt = now
            )
        }
    }
}