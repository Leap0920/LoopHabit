package com.example.loophabit.data.supabase.mappers

import com.example.loophabit.data.User
import com.example.loophabit.data.supabase.dto.UserDto
import java.time.Instant
import java.util.UUID

/**
 * Mapper for converting between User (Room) and UserDto (Supabase).
 * DTOs use String types for IDs and timestamps for serialization safety.
 * Note: Supabase Auth manages users separately with UUID IDs.
 * This mapper is for syncing additional user profile data to a custom 'profiles' table.
 */
object UserMapper : EntityDtoMapper<User, UserDto> {

    override fun toDto(entity: User): UserDto {
        return UserDto(
            id = entity.id.toString(),
            email = entity.email,
            username = entity.username,
            security_question = entity.securityQuestion,
            security_answer_hash = entity.password, // In production, this should be hashed
            created_at = Instant.ofEpochMilli(entity.id).toString(),
            updated_at = Instant.ofEpochMilli(entity.id).toString()
        )
    }

    override fun toEntity(dto: UserDto): User {
        return User(
            id = dto.id.toLongOrNull() ?: dto.id.hashCode().toLong(),
            username = dto.username,
            email = dto.email,
            password = dto.security_answer_hash, // In production, don't store plaintext
            securityQuestion = dto.security_question,
            securityAnswer = "" // Not stored in DTO
        )
    }

    override fun toInsertDto(entity: User): UserDto {
        val uuid = UUID.randomUUID() // New profiles get new UUID
        return UserDto.create(
            id = uuid,
            email = entity.email,
            username = entity.username,
            securityQuestion = entity.securityQuestion,
            securityAnswerHash = entity.password
        )
    }
}
