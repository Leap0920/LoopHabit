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
            val success = syncManager.fullSync()
            if (success) {
                android.util.Log.d("SyncWorker", "Sync completed successfully")
                Result.success()
            } else {
                android.util.Log.e("SyncWorker", "Sync failed")
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Sync worker exception", e)
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
