package com.example.loophabit.data.supabase.mappers

import com.example.loophabit.data.User
import com.example.loophabit.data.supabase.dto.UserDto

object UserMapper : EntityDtoMapper<User, UserDto> {

    override fun toDto(entity: User): UserDto {
        return UserDto(
            id = entity.id.toString(),
            email = entity.email,
            username = entity.username,
            security_question = entity.securityQuestion,
            security_answer_hash = entity.password,
            created_at = epochToIso(entity.id),
            updated_at = epochToIso(entity.id)
        )
    }

    override fun toEntity(dto: UserDto): User {
        return User(
            id = dto.id.toLongOrNull() ?: dto.id.hashCode().toLong(),
            username = dto.username,
            email = dto.email,
            password = dto.security_answer_hash,
            securityQuestion = dto.security_question,
            securityAnswer = ""
        )
    }

    override fun toInsertDto(entity: User): UserDto {
        return UserDto.create(
            id = entity.id.toString(),
            email = entity.email,
            username = entity.username,
            securityQuestion = entity.securityQuestion,
            securityAnswerHash = entity.password
        )
    }
}
