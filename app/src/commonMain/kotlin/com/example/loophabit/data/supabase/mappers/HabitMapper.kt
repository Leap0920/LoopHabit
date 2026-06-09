package com.example.loophabit.data.supabase.mappers

import com.example.loophabit.data.Habit
import com.example.loophabit.data.supabase.dto.HabitDto

object HabitMapper : EntityDtoMapper<Habit, HabitDto> {

    override fun toDto(entity: Habit): HabitDto {
        return HabitDto(
            id = entity.id.toString(),
            user_id = entity.userId.toString(),
            title = entity.title,
            color_hex = entity.colorHex,
            created_at = epochToIso(entity.createdAt),
            target_days_per_week = entity.targetDaysPerWeek,
            updated_at = epochToIso(entity.createdAt),
            is_numerical = entity.isNumerical,
            numerical_goal = entity.numericalGoal,
            numerical_unit = entity.numericalUnit,
            days_of_week_pattern = entity.daysOfWeekPattern
        )
    }

    override fun toEntity(dto: HabitDto): Habit {
        return Habit(
            id = dto.id.toLongOrNull() ?: dto.id.hashCode().toLong(),
            userId = dto.user_id.toLongOrNull() ?: dto.user_id.hashCode().toLong(),
            title = dto.title,
            colorHex = dto.color_hex,
            createdAt = try { isoToEpoch(dto.created_at) } catch (e: Exception) { 0L },
            targetDaysPerWeek = dto.target_days_per_week,
            isNumerical = dto.is_numerical,
            numericalGoal = dto.numerical_goal,
            numericalUnit = dto.numerical_unit,
            daysOfWeekPattern = dto.days_of_week_pattern
        )
    }

    override fun toInsertDto(entity: Habit): HabitDto {
        return HabitDto.create(
            userId = entity.userId.toString(),
            title = entity.title,
            colorHex = entity.colorHex,
            targetDaysPerWeek = entity.targetDaysPerWeek,
            isNumerical = entity.isNumerical,
            numericalGoal = entity.numericalGoal,
            numericalUnit = entity.numericalUnit,
            daysOfWeekPattern = entity.daysOfWeekPattern
        )
    }
}
