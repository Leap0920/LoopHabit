package com.example.loophabit.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.loophabit.LoopHabitApp
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? LoopHabitApp ?: return Result.failure()
        val userId = app.preferences.currentUserIdFlow.first()
        if (userId == 0L) return Result.success()

        try {
            val repository = app.repository
            val habits = repository.getAllHabits(userId).first()
            val completions = repository.getAllCompletionsForUser(userId).first()
            val focusSessions = repository.getAllFocusSessions(userId).first()

            val backupMap = mapOf(
                "habits" to habits,
                "completions" to completions,
                "focusSessions" to focusSessions
            )

            val jsonString = GsonBuilder().setPrettyPrinting().create().toJson(backupMap)

            val backupFolder = File(applicationContext.getExternalFilesDir(null), "backups")
            if (!backupFolder.exists()) {
                backupFolder.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupFolder, "LoopHabit_AutoBackup_$timestamp.json")
            backupFile.writeText(jsonString)

            // Keep only the 5 most recent auto-backups to prevent storage bloat
            val files = backupFolder.listFiles { _, name -> name.startsWith("LoopHabit_AutoBackup_") && name.endsWith(".json") }
            if (files != null && files.size > 5) {
                val sortedFiles = files.sortedBy { it.lastModified() }
                val toDeleteCount = sortedFiles.size - 5
                for (i in 0 until toDeleteCount) {
                    sortedFiles[i].delete()
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    companion object {
        fun scheduleAutoBackup(context: Context, intervalHours: Int) {
            val workManager = WorkManager.getInstance(context)
            if (intervalHours <= 0) {
                workManager.cancelUniqueWork("auto_backup_work")
                return
            }

            val request = PeriodicWorkRequestBuilder<BackupWorker>(
                intervalHours.toLong(), TimeUnit.HOURS
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "auto_backup_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelAutoBackup(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("auto_backup_work")
        }
    }
}
