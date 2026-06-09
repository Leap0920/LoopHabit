package com.example.loophabit.data.supabase

import android.content.Context
import com.example.loophabit.data.sync.AuthStateProvider
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

    private var authJob: kotlinx.coroutines.Job? = null

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

    /** Listen to auth state changes via sessionStatus StateFlow and update flows. */
    private fun listenToAuthChanges() {
        authJob?.cancel()
        authJob = CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user: UserInfo? = status.session.user
                        if (user != null) {
                            _currentUser.value = user
                            _authState.value = AuthState.Authenticated(user)
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _currentUser.value = null
                        _authState.value = AuthState.Unauthenticated(null)
                    }
                    is SessionStatus.Initializing -> {
                        // Still loading; keep current state
                    }
                    is SessionStatus.RefreshFailure -> {
                        // Refresh failed; keep current state
                    }
                }
            }
        }
    }

    /** Cancel background auth listener to prevent coroutine leak. */
    fun destroy() {
        authJob?.cancel()
        authJob = null
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
            val response: UserInfo? = SupabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = buildJsonObject {
                    put("username", username)
                    put("security_question", securityQuestion)
                    put("security_answer", securityAnswer)
                }
            }
            if (response != null) {
                _currentUser.value = response
                _authState.value = AuthState.Authenticated(response)
                Result.success(response)
            } else {
                Result.failure(Exception("Sign up succeeded but no user returned"))
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            Result.failure(e)
        }
    }

    /** Sign in with email and password. */
    suspend fun signIn(email: String, password: String): Result<Any> {
        return try {
            SupabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            // signInWith completes the sign-in; the session is published via sessionStatus.
            // Read currentSessionOrNull to surface the user object immediately.
            val user = SupabaseClient.auth.currentSessionOrNull()?.user
            if (user != null) {
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                // Fallback: wait briefly for sessionStatus to update
                kotlinx.coroutines.delay(500)
                val delayedUser = SupabaseClient.auth.currentSessionOrNull()?.user
                if (delayedUser != null) {
                    _currentUser.value = delayedUser
                    _authState.value = AuthState.Authenticated(delayedUser)
                    Result.success(delayedUser)
                } else {
                    Result.failure(Exception("Sign in succeeded but no user returned"))
                }
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            Result.failure(e)
        }
    }

    /** Sign out the current user. */
    suspend fun signOut(): Result<Unit> {
        return try {
            SupabaseClient.auth.signOut()
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
            SupabaseClient.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Update user password (requires active session). */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            SupabaseClient.auth.updateUser {
                this.password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get current session (if any). */
    suspend fun getCurrentSession(): Result<Any?> {
        return try {
            val session = SupabaseClient.auth.currentSessionOrNull()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get current user (from memory or session). */
    suspend fun getCurrentUser(): Result<Any?> {
        return try {
            val session = SupabaseClient.auth.currentSessionOrNull()
            val user = session?.user ?: _currentUser.value
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Check if user is currently signed in. */
    override val isSignedIn: Boolean
        get() = _currentUser.value != null

    /** Get current user ID as UUID string (for use with profiles table). */
    val currentUserId: String?
        get() {
            val user = _currentUser.value as? UserInfo
            return user?.id
        }
}
