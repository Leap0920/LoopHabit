package com.example.loophabit.data.supabase

import android.content.Context
import com.example.loophabit.data.User
import com.supabase.gotrue.User as SupabaseUser
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
) {

    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState = _authState.asStateFlow()

    init {
        // Initialize Supabase client if not already done
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
        data class Authenticated(val user: SupabaseUser) : AuthState
        data class Unauthenticated(val message: String?) : AuthState
        object Unknown : AuthState
        data class Error(val message: String) : AuthState
    }

    /** Listen to auth state changes and update flows. */
    private fun listenToAuthChanges() {
        CoroutineScope(Dispatchers.IO).launch {
            SupabaseClient.auth.onAuthStateChange.listen { event ->
                when (event.event) {
                    "SIGNED_IN" -> {
                        _currentUser.value = event.session?.user
                        _authState.value = AuthState.Authenticated(event.session!!.user)
                    }
                    "SIGNED_OUT" -> {
                        _currentUser.value = null
                        _authState.value = AuthState.Unauthenticated(null)
                    }
                    "USER_UPDATED" -> {
                        _currentUser.value = event.session?.user
                        if (event.session?.user != null) {
                            _authState.value = AuthState.Authenticated(event.session!!.user)
                        }
                    }
                    "TOKEN_REFRESHED" -> {
                        // Token refreshed silently
                    }
                    else -> {
                        // Other events: PASSWORD_RECOVERY, etc.
                    }
                }
            }
        }
    }

    /** Sign up with email, password, and metadata (username, security Q&A). */
    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        securityQuestion: String,
        securityAnswer: String
    ): Result<SupabaseUser> {
        return try {
            val response = SupabaseClient.auth.signUp(
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
    suspend fun signIn(email: String, password: String): Result<SupabaseUser> {
        return try {
            val response = SupabaseClient.auth.signInWith(
                provider = com.supabase.gotrue.EmailPassword.Provider.EmailPassword(email, password)
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
            SupabaseClient.auth.updateUser(
                attributes = com.supabase.gotrue.UpdateUserAttributes(password = newPassword)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get current session (if any). */
    suspend fun getCurrentSession(): Result<com.supabase.gotrue.Session?> {
        return try {
            val session = SupabaseClient.auth.currentSession
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Get current user (from memory or session). */
    suspend fun getCurrentUser(): Result<SupabaseUser?> {
        return try {
            val session = SupabaseClient.auth.currentSession
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
        get() = _currentUser.value?.id?.toString()
}