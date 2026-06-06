package com.example.loophabit

import android.app.Application
import com.example.loophabit.data.AppDatabase
import com.example.loophabit.data.HabitRepository
import com.example.loophabit.data.LoopPreferences
import com.example.loophabit.data.supabase.AuthRepository
import com.example.loophabit.data.supabase.SupabaseClient
import com.example.loophabit.data.sync.AuthStateProvider
import com.example.loophabit.data.sync.SyncManager
import com.example.loophabit.data.sync.SupabaseSyncClientFactory
import com.example.loophabit.data.sync.SyncScheduler

class LoopHabitApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val preferences by lazy { LoopPreferences(this) }

    // Supabase
    val supabaseClient by lazy {
        SupabaseClient.initialize(this)
        SupabaseClient.instance
    }
    val authRepository by lazy { AuthRepository.getInstance(this) }

    // Sync
    val syncManager by lazy { 
        SyncManager(
            database = database,
            authStateProvider = authRepository as AuthStateProvider,
            supabaseSyncClient = SupabaseSyncClientFactory.create()
        )
    }

    val repository by lazy { HabitRepository(database.habitDao(), database.userDao(), preferences, syncManager) }

    override fun onCreate() {
        super.onCreate()
        // Schedule periodic background sync
        SyncScheduler.schedulePeriodicSync(this)
        // Register network callback for immediate sync on connectivity restore
        SyncScheduler.registerNetworkCallback(this)
        // Schedule daily habit reminder at 8:00 PM
        com.example.loophabit.data.sync.ReminderWorker.scheduleDailyReminder(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        // Clean up network callback
        SyncScheduler.unregisterNetworkCallback(this)
    }
}
