package com.example.loophabit.data.supabase.mappers

import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.supabase.dto.HabitCompletionDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Mapper for converting between HabitCompletion (Room) and HabitCompletionDto (Supabase).
 * DTOs use String types for IDs and timestamps for serialization safety.
 */
object HabitCompletionMapper : EntityDtoMapper<HabitCompletion, HabitCompletionDto> {

    override fun toDto(entity: HabitCompletion): HabitCompletionDto {
        return HabitCompletionDto(
            id = entity.id.toString(),
            habit_id = entity.habitId.toString(),
            date = entity.date,
            notes = entity.notes,
            created_at = Instant.now().toString(),
            value = entity.value
        )
    }

    override fun toEntity(dto: HabitCompletionDto): HabitCompletion {
        return HabitCompletion(
            id = dto.id.toLongOrNull() ?: dto.id.hashCode().toLong(),
            habitId = dto.habit_id.toLongOrNull() ?: dto.habit_id.hashCode().toLong(),
            date = dto.date,
            notes = dto.notes,
            value = dto.value
        )
    }

    override fun toInsertDto(entity: HabitCompletion): HabitCompletionDto {
        return HabitCompletionDto.create(
            habitId = UUID.fromString(entity.habitId.toString()),
            date = LocalDate.parse(entity.date),
            notes = entity.notes,
            value = entity.value
        )
    }
}
