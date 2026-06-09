package com.example.loophabit.data.supabase.dto

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object representing a User profile in Supabase.
 * Uses String types for serialization safety with both Gson and kotlinx.serialization.
 * This is for the custom 'profiles' table (not auth.users which is managed by Supabase Auth).
 */
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val security_question: String,
    val security_answer_hash: String,
    val created_at: String,
    val updated_at: String
) {
    companion object {
        fun create(
            id: UUID,
            email: String,
            username: String,
            securityQuestion: String,
            securityAnswerHash: String
        ): UserDto {
            val now = Instant.now().toString()
            return UserDto(
                id = id.toString(),
                email = email,
                username = username,
                security_question = securityQuestion,
                security_answer_hash = securityAnswerHash,
                created_at = now,
                updated_at = now
            )
        }
    }
}
