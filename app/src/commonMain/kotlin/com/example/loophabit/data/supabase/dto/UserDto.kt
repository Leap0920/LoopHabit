package com.example.loophabit.data.supabase.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            id: String,
            email: String,
            username: String,
            securityQuestion: String,
            securityAnswerHash: String
        ): UserDto {
            val now = currentTimestamp()
            return UserDto(
                id = id.ifEmpty { Uuid.random().toString() },
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
