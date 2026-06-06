package com.example.loophabit.data.supabase

import android.content.Context
import io.github.jan.supabase.SupabaseClient as OfficialSupabaseClient

/**
 * Singleton wrapper for Supabase client initialization and access.
 * Uses the unified SupabaseClient from supabase-kt library.
 */
object SupabaseClient {

    private var client: OfficialSupabaseClient? = null
    private var isInitialized = false

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

        client = OfficialSupabaseClient(supabaseUrl, supabaseAnonKey)
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
            @Suppress("UNCHECKED_CAST")
            return client as OfficialSupabaseClient
        }

    /** Checks if the client has been initialized. */
    fun isInitialized(): Boolean = isInitialized

    /** Resets the client (primarily for testing). Not recommended for production use. */
    internal fun reset() {
        client = null
        isInitialized = false
    }
}