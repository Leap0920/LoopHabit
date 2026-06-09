package com.example.loophabit.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.loophabit.MainActivity
import com.example.loophabit.LoopHabitApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class FocusService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var baseTimeMillis: Long = 0L

    companion object {
        const val CHANNEL_ID = "focus_service_channel"
        const val NOTIFICATION_ID = 1003

        const val EXTRA_MODE = "mode"
        const val EXTRA_HABIT_TITLE = "habit_title"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val EXTRA_SECONDS_LEFT = "seconds_left"
        const val EXTRA_SECONDS_ELAPSED = "seconds_elapsed"

        const val ACTION_PAUSE = "com.example.loophabit.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.loophabit.ACTION_RESUME"
        const val ACTION_RESET = "com.example.loophabit.ACTION_RESET"

        // Static flows for the UI components to collect in real-time
        val isServiceRunning = MutableStateFlow(false)
        val isPaused = MutableStateFlow(false)
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
        isPaused.value = false
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            handleAction(action)
            return START_NOT_STICKY
        }

        val inputMode = intent?.getStringExtra(EXTRA_MODE) ?: "TIMER"
        val inputHabitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: "Focus Session"
        val inputDuration = intent?.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60) ?: (25 * 60)
        val inputSecLeft = intent?.getIntExtra(EXTRA_SECONDS_LEFT, 25 * 60) ?: (25 * 60)
        val inputSecElapsed = intent?.getIntExtra(EXTRA_SECONDS_ELAPSED, 0) ?: 0

        mode.value = inputMode
        habitTitle.value = inputHabitTitle

        if (inputMode == "TIMER") {
            secondsLeft.value = inputSecLeft
            baseTimeMillis = SystemClock.elapsedRealtime() + (inputSecLeft * 1000L)
        } else {
            secondsElapsed.value = inputSecElapsed
            baseTimeMillis = SystemClock.elapsedRealtime() - (inputSecElapsed * 1000L)
        }

        startForeground(NOTIFICATION_ID, buildNotification(getNotificationText()))
        startTicking(inputMode)

        return START_NOT_STICKY
    }

    private fun handleAction(action: String) {
        serviceScope.launch {
            val app = applicationContext as? LoopHabitApp ?: return@launch
            val prefState = app.repository.focusStateFlow.first()
            when (action) {
                ACTION_PAUSE -> {
                    isPaused.value = true
                    app.repository.saveFocusState(prefState.copy(
                        isRunning = false,
                        pausedSeconds = if (mode.value == "TIMER") secondsLeft.value else secondsElapsed.value,
                        baseTimestamp = System.currentTimeMillis()
                    ))
                    updateNotification(getNotificationText())
                }
                ACTION_RESUME -> {
                    isPaused.value = false
                    val currentSecs = if (mode.value == "TIMER") secondsLeft.value else secondsElapsed.value
                    if (mode.value == "TIMER") {
                        baseTimeMillis = SystemClock.elapsedRealtime() + (currentSecs * 1000L)
                    } else {
                        baseTimeMillis = SystemClock.elapsedRealtime() - (currentSecs * 1000L)
                    }
                    app.repository.saveFocusState(prefState.copy(
                        isRunning = true,
                        pausedSeconds = currentSecs,
                        baseTimestamp = System.currentTimeMillis()
                    ))
                    updateNotification(getNotificationText())
                }
                ACTION_RESET -> {
                    isPaused.value = false
                    val defaultSecs = if (mode.value == "TIMER") prefState.initialDurationMinutes * 60 else 0
                    app.repository.saveFocusState(prefState.copy(
                        isRunning = false,
                        pausedSeconds = defaultSecs
                    ))
                    stopSelf()
                }
            }
        }
    }

    private fun startTicking(modeStr: String) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            val now = SystemClock.elapsedRealtime()
            if (modeStr == "TIMER") {
                baseTimeMillis = now + (secondsLeft.value * 1000L)
            } else {
                baseTimeMillis = now - (secondsElapsed.value * 1000L)
            }

            while (isActive) {
                delay(1000L)
                if (isPaused.value) {
                    continue
                }
                val currentRealtime = SystemClock.elapsedRealtime()
                if (modeStr == "TIMER") {
                    val remaining = ((baseTimeMillis - currentRealtime) / 1000L).toInt()
                    secondsLeft.value = Math.max(0, remaining)
                    updateNotification(getNotificationText())
                    if (secondsLeft.value == 0) {
                        updateNotification("Focus Session Complete!")
                        // Persist completion state so recovery logic doesn't double-log
                        val app = applicationContext as? LoopHabitApp
                        if (app != null) {
                            val prefState = app.repository.focusStateFlow.first()
                            app.repository.saveFocusState(prefState.copy(isRunning = false))
                        }
                        // Stop the service when timer completes
                        delay(500L) // Brief delay so notification is visible
                        stopSelf()
                        break
                    }
                } else {
                    val elapsed = ((currentRealtime - baseTimeMillis) / 1000L).toInt()
                    secondsElapsed.value = elapsed
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

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Active Focus Session")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)

        // Add Pause or Resume action button
        if (isPaused.value) {
            val resumeIntent = Intent(this, FocusService::class.java).apply { action = ACTION_RESUME }
            val pendingResume = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pendingResume)
        } else {
            val pauseIntent = Intent(this, FocusService::class.java).apply { action = ACTION_PAUSE }
            val pendingPause = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pendingPause)
        }

        // Add Reset action button
        val resetIntent = Intent(this, FocusService::class.java).apply { action = ACTION_RESET }
        val pendingReset = PendingIntent.getService(this, 3, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", pendingReset)

        return builder.build()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        isServiceRunning.value = false
        isPaused.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
