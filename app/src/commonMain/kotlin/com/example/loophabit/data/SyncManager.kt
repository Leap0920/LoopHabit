package com.example.loophabit.data

import kotlinx.coroutines.flow.Flow

sealed interface SyncState {
    data object Idle : SyncState
    data class Syncing(val progress: Int = 0, val message: String = "") : SyncState
    data object Completed : SyncState
    data class Error(val message: String) : SyncState
}

interface SyncManager {
    val syncState: Flow<SyncState>
    suspend fun fullSync(): Boolean
    suspend fun pushLocalChanges(): Boolean
    suspend fun pullRemoteChanges(): Boolean
    fun isCurrentlySyncing(): Boolean
}
