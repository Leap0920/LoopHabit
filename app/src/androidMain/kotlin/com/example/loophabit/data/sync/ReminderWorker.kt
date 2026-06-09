package com.example.loophabit.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.loophabit.LoopHabitApp
import com.example.loophabit.MainActivity
import com.example.loophabit.data.AppDatabase
import com.example.loophabit.data.LoopPreferences
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? LoopHabitApp ?: return Result.failure()
        val userId = app.preferences.currentUserIdFlow.first()
        if (userId == 0L) return Result.success() // No logged-in user

        val todayStr = LocalDate.now().toString()
        val incompleteHabits = app.database.habitDao().getIncompleteHabits(userId, todayStr).first()

        if (incompleteHabits.isNotEmpty()) {
            sendNotification(incompleteHabits.size)
        }

        return Result.success()
    }

    private fun sendNotification(remainingCount: Int) {
        val channelId = "habit_reminder_channel"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when habits need completion to maintain streaks"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Don't break your loop!")
            .setContentText("You still have $remainingCount habit${if (remainingCount > 1) "s" else ""} remaining today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1002, notification)
    }

    companion object {
        fun scheduleDailyReminder(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Calculate delay until next 8:00 PM
            val now = java.time.LocalDateTime.now()
            var next8PM = now.toLocalDate().atTime(20, 0)
            if (now.isAfter(next8PM)) {
                next8PM = next8PM.plusDays(1)
            }
            val delayMinutes = java.time.Duration.between(now, next8PM).toMinutes()

            val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                24, TimeUnit.HOURS
            )
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

            workManager.enqueueUniquePeriodicWork(
                "daily_habit_reminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderRequest
            )
        }
    }
}
