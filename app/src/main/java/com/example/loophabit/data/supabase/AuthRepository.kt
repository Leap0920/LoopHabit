package com.example.loophabit.data.supabase

import android.content.Context
import com.example.loophabit.data.sync.AuthStateProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository wrapper for Supabase Authentication operations.
 * Provides sign up, sign in, sign out, password reset, and auth state observation.
 */
class AuthRepository private constructor(
    private val context: Context
) : AuthStateProvider {

    private val _currentUser = MutableStateFlow<Any?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState = _authState.asStateFlow()

    init {
        if (!SupabaseClient.isInitialized()) {
            SupabaseClient.initialize(context)
        }
        listenToAuthChanges()
    }

    companion object {
        @Volatile private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /** Sealed class representing authentication state. */
    sealed interface AuthState {
        data class Authenticated(val user: Any) : AuthState
        data class Unauthenticated(val message: String?) : AuthState
        object Unknown : AuthState
        data class Error(val message: String) : AuthState
    }

    /** Listen to auth state changes and update flows. */
    private fun listenToAuthChanges() {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.instance.auth.onAuthStateChange.listen { event ->
                when (event.event) {
                    "SIGNED_IN" -> {
                        val user = event.session?.user
                        _currentUser.value = user
                        user?.let { _authState.value = AuthState.Authenticated(it) }
                    }
                    "SIGNED_OUT" -> {
                        _currentUser.value = null
                        _authState.value = AuthState.Unauthenticated(null)
                    }
                    "USER_UPDATED" -> {
                        val user = event.session?.user
                        _currentUser.value = user
                        user?.let { _authState.value = AuthState.Authenticated(it) }
                    }
                    "TOKEN_REFRESHED" -> { }
                    else -> { }
                }
            }
        }
    }

    /** Sign up with email, password, and metadata. */
    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        securityQuestion: String,
        securityAnswer: String
    ): Result<Any> {
        return try {
            val response = SupabaseClient.instance.auth.signUp(
                email = email,
                password = password,
                data = mapOf(
                    "username" to username,
                    "security_question" to securityQuestion,
                    "security_answer" to securityAnswer
                )
            )
            response.user?.let { user ->
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } ?: Result.failure(Exception("Sign up succeeded but no user returned"))
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            Result.failure(e)
        }
    }

    /** Sign in with email and password. */
    suspend fun signIn(email: String, password: String): Result<Any> {
        return try {
            val response = SupabaseClient.instance.auth.signIn(
                email = email,
                password = password
            )
            response.user?.let { user ->
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } ?: Result.failure(Exception("Sign in succeeded but no user returned"))
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }

    /** Sign out the current user. */
    suspend fun signOut(): Result<Unit> {
        return try {
            SupabaseClient.instance.auth.signOut()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated("Signed out")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Send password reset email. */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            SupabaseClient.instance.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Update user password (requires active session). */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            SupabaseClient.instance.auth.updateUser(
                attributes = mapOf("password" to newPassword)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get current session (if any). */
    suspend fun getCurrentSession(): Result<Any?> {
        return try {
            val session = SupabaseClient.instance.auth.currentSession
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get current user (from memory or session). */
    suspend fun getCurrentUser(): Result<Any?> {
        return try {
            val session = SupabaseClient.instance.auth.currentSession
            val user = session?.user ?: _currentUser.value
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Check if user is currently signed in. */
    val isSignedIn: Boolean
        get() = _currentUser.value != null

    /** Get current user ID as UUID string (for use with profiles table). */
    val currentUserId: String?
        get() {
            val user = _currentUser.value
            return if (user != null) {
                try {
                    (user.javaClass.getDeclaredField("id").apply { isAccessible = true }.get(user)).toString()
                } catch (e: Exception) {
                    null
                }
            } else null
        }
}