package com.example.loophabit.data.supabase.mappers

/**
 * Base interface for mapping between local entities and Supabase DTOs.
 */
interface EntityDtoMapper<E, D> {
    fun toDto(entity: E): D
    fun toEntity(dto: D): E
    fun toInsertDto(entity: E): D
}
