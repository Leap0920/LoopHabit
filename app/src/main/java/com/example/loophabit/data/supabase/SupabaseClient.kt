package com.example.loophabit.data.supabase

import android.content.Context
import io.github.jan.supabase.SupabaseClient as OfficialSupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Singleton wrapper for Supabase client initialization and access.
 * Uses the unified SupabaseClient from supabase-kt library.
 */
object SupabaseClient {

    private var client: OfficialSupabaseClient? = null
    private var isInitialized = false

    /** Auth plugin for authentication operations */
    val auth: Auth
        get() {
            if (!isInitialized || client == null) {
                throw IllegalStateException(
                    "SupabaseClient not initialized. " +
                    "Call SupabaseClient.initialize(context) in your Application class first."
                )
            }
            return client!!.auth
        }

    /** Postgrest plugin for database operations */
    val postgrest: Postgrest
        get() {
            if (!isInitialized || client == null) {
                throw IllegalStateException(
                    "SupabaseClient not initialized. " +
                    "Call SupabaseClient.initialize(context) in your Application class first."
                )
            }
            return client!!.postgrest
        }

    /** Realtime plugin for realtime subscriptions */
    val realtime: Realtime
        get() {
            if (!isInitialized || client == null) {
                throw IllegalStateException(
                    "SupabaseClient not initialized. " +
                    "Call SupabaseClient.initialize(context) in your Application class first."
                )
            }
            return client!!.realtime
        }

    /** Storage plugin for file storage operations */
    val storage: Storage
        get() {
            if (!isInitialized || client == null) {
                throw IllegalStateException(
                    "SupabaseClient not initialized. " +
                    "Call SupabaseClient.initialize(context) in your Application class first."
                )
            }
            return client!!.storage
        }

    /**
     * Initializes the Supabase client with configuration from BuildConfig.
     * Must be called once during application startup (e.g., in Application.onCreate()).
     *
     * @param context Android context to access BuildConfig
     * @throws IllegalStateException if already initialized
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            throw IllegalStateException("SupabaseClient already initialized. Call initialize() only once.")
        }

        val supabaseUrl = com.example.loophabit.BuildConfig.SUPABASE_URL
        val supabaseAnonKey = com.example.loophabit.BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            throw IllegalArgumentException(
                "Supabase URL or Anon Key is missing. " +
                "Ensure SUPABASE_URL and SUPABASE_ANON_KEY are defined in BuildConfig"
            )
        }

        client = createSupabaseClient(supabaseUrl, supabaseAnonKey) {
            httpEngine = io.ktor.client.engine.okhttp.OkHttp.create()
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
        isInitialized = true
    }

    /**
     * Returns the initialized Supabase client instance.
     * @throws IllegalStateException if initialize() hasn't been called yet
     */
    val instance: OfficialSupabaseClient
        get() {
            if (!isInitialized || client == null) {
                throw IllegalStateException(
                    "SupabaseClient not initialized. " +
                    "Call SupabaseClient.initialize(context) in your Application class first."
                )
            }
            return client!!
        }

    /** Checks if the client has been initialized. */
    fun isInitialized(): Boolean = isInitialized

    /** Resets the client (primarily for testing). Not recommended for production use. */
    internal fun reset() {
        client = null
        isInitialized = false
    }
}
