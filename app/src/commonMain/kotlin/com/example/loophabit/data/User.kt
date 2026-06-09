package com.example.loophabit.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
@Serializable
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String,
    val password: String,
    val securityQuestion: String,
    val securityAnswer: String
)
