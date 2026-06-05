package com.example.loophabit.data.supabase.mappers

/**
 * Base interface for mapping between local Room entities and Supabase DTOs.
 * @param <E> The local Room entity type
 * @param <D> The Supabase DTO type
 */
interface EntityDtoMapper<E, D> {
    /** Converts a Room entity to a Supabase DTO for upload. */
    fun toDto(entity: E): D

    /** Converts a Supabase DTO to a Room entity for local storage. */
    fun toEntity(dto: D): E

    /** Creates a new DTO for insertion (without server-generated fields like ID and timestamps). */
    fun toInsertDto(entity: E): D
}