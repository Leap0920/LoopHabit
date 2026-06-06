package com.example.loophabit.data.supabase.dto

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.util.UUID

/**
 * Data Transfer Object representing a User profile in Supabase.
 * This is for the custom 'profiles' table (not auth.users which is managed by Supabase Auth).
 */
data class UserDto(
    @SerializedName("id") val id: UUID,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("security_question") val securityQuestion: String,
    @SerializedName("security_answer_hash") val securityAnswerHash: String,
    @SerializedName("created_at") val createdAt: Instant,
    @SerializedName("updated_at") val updatedAt: Instant
) {
    companion object {
        fun create(
            id: UUID,
            email: String,
            username: String,
            securityQuestion: String,
            securityAnswerHash: String
        ): UserDto {
            val now = Instant.now()
            return UserDto(
                id = id,
                email = email,
                username = username,
                securityQuestion = securityQuestion,
                securityAnswerHash = securityAnswerHash,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}