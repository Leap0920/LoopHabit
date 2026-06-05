package com.example.loophabit.data.supabase.mappers

import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.supabase.dto.HabitCompletionDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Mapper for converting between HabitCompletion (Room) and HabitCompletionDto (Supabase).
 */
object HabitCompletionMapper : EntityDtoMapper<HabitCompletion, HabitCompletionDto> {

    override fun toDto(entity: HabitCompletion): HabitCompletionDto {
        return HabitCompletionDto(
            id = UUID.fromString(entity.id.toString()), // Long -> UUID
            habitId = UUID.fromString(entity.habitId.toString()), // Long -> UUID
            date = LocalDate.parse(entity.date), // YYYY-MM-DD
            notes = entity.notes,
            createdAt = Instant.now() // No createdAt in entity
        )
    }

    override fun toEntity(dto: HabitCompletionDto): HabitCompletion {
        return HabitCompletion(
            id = dto.id.toString().hashCode().toLong(), // UUID -> Long
            habitId = dto.habitId.toString().hashCode().toLong(), // UUID -> Long
            date = dto.date.toString(), // YYYY-MM-DD
            notes = dto.notes
        )
    }

    override fun toInsertDto(entity: HabitCompletion): HabitCompletionDto {
        return HabitCompletionDto.create(
            habitId = UUID.fromString(entity.habitId.toString()),
            date = LocalDate.parse(entity.date),
            notes = entity.notes
        )
    }
}