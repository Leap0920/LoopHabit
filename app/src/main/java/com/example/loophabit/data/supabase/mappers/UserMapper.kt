package com.example.loophabit.data.supabase.mappers

import com.example.loophabit.data.User
import com.example.loophabit.data.supabase.dto.UserDto
import java.time.Instant
import java.util.UUID

/**
 * Mapper for converting between User (Room) and UserDto (Supabase).
 * Note: Supabase Auth manages users separately with UUID IDs.
 * This mapper is for syncing additional user profile data to a custom 'profiles' table.
 */
object UserMapper : EntityDtoMapper<User, UserDto> {

    override fun toDto(entity: User): UserDto {
        return UserDto(
            id = UUID.fromString(entity.id.toString()), // Local Long ID -> UUID (will need mapping table)
            email = entity.email,
            username = entity.username,
            securityQuestion = entity.securityQuestion,
            securityAnswerHash = entity.password, // In production, this should be hashed
            createdAt = Instant.ofEpochMilli(entity.id), // Placeholder - add createdAt to User entity
            updatedAt = Instant.ofEpochMilli(entity.id)  // Placeholder - add updatedAt to User entity
        )
    }

    override fun toEntity(dto: UserDto): User {
        return User(
            id = dto.id.toString().hashCode().toLong(), // UUID -> Long (lossy, needs mapping table)
            username = dto.username,
            email = dto.email,
            password = dto.securityAnswerHash, // In production, don't store plaintext
            securityQuestion = dto.securityQuestion,
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