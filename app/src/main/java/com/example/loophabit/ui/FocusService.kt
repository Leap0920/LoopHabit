package com.example.loophabit.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.loophabit.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class FocusService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "focus_service_channel"
        const val NOTIFICATION_ID = 1003

        const val EXTRA_MODE = "mode"
        const val EXTRA_HABIT_TITLE = "habit_title"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val EXTRA_SECONDS_LEFT = "seconds_left"
        const val EXTRA_SECONDS_ELAPSED = "seconds_elapsed"

        // Static flows for the UI components to collect in real-time
        val isServiceRunning = MutableStateFlow(false)
        val mode = MutableStateFlow("TIMER")
        val secondsLeft = MutableStateFlow(0)
        val secondsElapsed = MutableStateFlow(0)
        val habitTitle = MutableStateFlow("")

        fun startService(
            context: Context,
            mode: String,
            habitTitle: String,
            durationSeconds: Int,
            secondsLeft: Int,
            secondsElapsed: Int
        ) {
            val intent = Intent(context, FocusService::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_HABIT_TITLE, habitTitle)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_SECONDS_LEFT, secondsLeft)
                putExtra(EXTRA_SECONDS_ELAPSED, secondsElapsed)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning.value = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val inputMode = intent?.getStringExtra(EXTRA_MODE) ?: "TIMER"
        val inputHabitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: "Focus Session"
        val inputDuration = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60) ?: (25 * 60)
        val inputSecLeft = intent?.getIntExtra(EXTRA_SECONDS_LEFT, 25 * 60) ?: (25 * 60)
        val inputSecElapsed = intent?.getIntExtra(EXTRA_SECONDS_ELAPSED, 0) ?: 0

        mode.value = inputMode
        habitTitle.value = inputHabitTitle

        if (inputMode == "TIMER") {
            secondsLeft.value = inputSecLeft
        } else {
            secondsElapsed.value = inputSecElapsed
        }

        startForeground(NOTIFICATION_ID, buildNotification(getNotificationText()))
        startTicking(inputMode)

        return START_NOT_STICKY
    }

    private fun startTicking(modeStr: String) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (modeStr == "TIMER") {
                    if (secondsLeft.value > 0) {
                        secondsLeft.value -= 1
                        updateNotification(getNotificationText())
                        if (secondsLeft.value == 0) {
                            updateNotification("Focus Session Complete!")
                            break
                        }
                    }
                } else {
                    secondsElapsed.value += 1
                    updateNotification(getNotificationText())
                }
            }
        }
    }

    private fun getNotificationText(): String {
        val label = if (habitTitle.value.isNotEmpty()) "Working on: ${habitTitle.value}" else "Focusing"
        val timeStr = if (mode.value == "TIMER") {
            val displaySecs = secondsLeft.value
            String.format("%02d:%02d", displaySecs / 60, displaySecs % 60)
        } else {
            val displaySecs = secondsElapsed.value
            String.format("%02d:%02d", displaySecs / 60, displaySecs % 60)
        }
        return "$label • $timeStr"
    }

    private fun buildNotification(contentText: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Active Focus Session")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        isServiceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
