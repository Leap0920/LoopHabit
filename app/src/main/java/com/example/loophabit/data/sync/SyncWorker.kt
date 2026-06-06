package com.example.loophabit.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that performs background synchronization.
 * Extends CoroutineWorker for Kotlin coroutines support.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncManager by lazy {
        val app = applicationContext as com.example.loophabit.LoopHabitApp
        app.syncManager
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Check if sync is already running (handled by ExistingWorkPolicy.KEEP too)
        if (syncManager.isCurrentlySyncing()) {
            return@withContext Result.success()
        }

        return@withContext try {
            val result = syncManager.fullSync()
            if (result.isSuccess) {
                val syncResult = result.getOrNull()
                // Log success
                android.util.Log.d("SyncWorker", "Sync completed: pushed ${syncResult?.pushedCount ?: 0}, pulled ${syncResult?.pulledCount ?: 0}")
                Result.success()
            } else {
                // Sync failed - signal retry with exponential backoff
                val exception = result.exceptionOrNull() ?: Exception("Sync failed")
                android.util.Log.e("SyncWorker", "Sync failed", exception)
                // Return retry to trigger exponential backoff
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Sync worker exception", e)
            // Return retry for transient failures
            Result.retry()
        }
    }

    companion object {
        /** Unique work name for this sync worker */
        const val WORK_NAME = "periodic_sync_work"

        /** Work tag for querying/cancelling */
        const val WORK_TAG = "sync_tag"
    }
}
