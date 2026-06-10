package com.example.loophabit

import com.example.loophabit.data.HabitCompletion
import com.example.loophabit.data.supabase.mappers.HabitCompletionMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitCompletionMapperTest {
    @Test
    fun toInsertDto_keepsNumericLocalIdsWithoutUuidParsing() {
        val completion = HabitCompletion(
            id = 12L,
            habitId = 34L,
            date = "2026-06-10",
            notes = "manual note",
            value = 2.5
        )

        val dto = HabitCompletionMapper.toInsertDto(completion)

        assertEquals("12", dto.id)
        assertEquals("34", dto.habit_id)
        assertEquals("2026-06-10", dto.date)
        assertEquals("manual note", dto.notes)
        assertEquals(2.5, dto.value, 0.0)
    }
}
