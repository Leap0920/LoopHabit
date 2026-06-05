package com.example.loophabit.data.supabase.mappers

import com.example.loophabit.data.Habit
import com.example.loophabit.data.supabase.dto.HabitDto
import java.time.Instant
import java.util.UUID

/**
 * Mapper for converting between Habit (Room) and HabitDto (Supabase).
 */
object HabitMapper : EntityDtoMapper<Habit, HabitDto> {

    override fun toDto(entity: Habit): HabitDto {
        return HabitDto(
            id = UUID.fromString(entity.id.toString()), // Long -> UUID
            userId = UUID.fromString(entity.userId.toString()), // Long -> UUID
            title = entity.title,
            colorHex = entity.colorHex,
            createdAt = Instant.ofEpochMilli(entity.createdAt),
            targetDaysPerWeek = entity.targetDaysPerWeek,
            updatedAt = Instant.ofEpochMilli(entity.createdAt) // No updatedAt in entity
        )
    }

    override fun toEntity(dto: HabitDto): Habit {
        return Habit(
            id = dto.id.toString().hashCode().toLong(), // UUID -> Long (lossy, needs mapping)
            userId = dto.userId.toString().hashCode().toLong(), // UUID -> Long
            title = dto.title,
            colorHex = dto.colorHex,
            createdAt = dto.createdAt.toEpochMilli(),
            targetDaysPerWeek = dto.targetDaysPerWeek
        )
    }

    override fun toInsertDto(entity: Habit): HabitDto {
        return HabitDto.create(
            userId = UUID.fromString(entity.userId.toString()),
            title = entity.title,
            colorHex = entity.colorHex,
            targetDaysPerWeek = entity.targetDaysPerWeek
        )
    }
}