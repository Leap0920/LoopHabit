package com.example.loophabit.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.loophabit.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * Schedules and manages periodic background synchronization using WorkManager.
 * Handles constraints: network required, not roaming, battery not low.
 * Uses ExistingWorkPolicy.KEEP to ensure only one sync runs at a time.
 */
object SyncScheduler {

    /** Minimum interval for periodic work (15 minutes = WorkManager minimum) */
    private const val SYNC_INTERVAL_MINUTES = 15L
    private const val FLEX_INTERVAL_MINUTES = 5L

    /**
     * Initializes and schedules the periodic sync work.
     * Should be called once during application startup.
     *
     * @param context Application context
     */
    fun schedulePeriodicSync(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Build constraints: network required (not roaming), battery not low
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_ROAMING)
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(false) // Allow sync when device is not idle
            .build()

        // Create periodic work request with exponential backoff
        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
            FLEX_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_TAG)
            .setInitialDelay(0, TimeUnit.MILLISECONDS) // Run immediately on first schedule
            .build()

        // Enqueue with KEEP policy - won't replace existing pending work with same name
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        if (BuildConfig.DEBUG) {
            android.util.Log.d("SyncScheduler", "Periodic sync scheduled: every ${SYNC_INTERVAL_MINUTES} min with constraints")
        }
    }

    /**
     * Schedules a one-time immediate sync (e.g., on network change or app foreground).
     * Respects the same constraints and won't run if periodic work is pending.
     *
     * @param context Application context
     */
    fun scheduleImmediateSync(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_ROAMING)
            .setRequiresBatteryNotLow(true)
            .build()

        val oneTimeWorkRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_TAG)
            .build()

        // Use KEEP to avoid duplicate immediate syncs
        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.KEEP,
            oneTimeWorkRequest
        )

        if (BuildConfig.DEBUG) {
            android.util.Log.d("SyncScheduler", "Immediate sync scheduled")
        }
    }

    /**
     * Cancels all pending and running sync work.
     * Useful for sign-out or when user disables sync.
     *
     * @param context Application context
     */
    fun cancelAllSyncWork(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(SyncWorker.WORK_TAG)
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
        workManager.cancelUniqueWork("immediate_sync")

        if (BuildConfig.DEBUG) {
            android.util.Log.d("SyncScheduler", "All sync work cancelled")
        }
    }

    /**
     * Checks if there's any pending or running sync work.
     *
     * @param context Application context
     * @return true if sync work is scheduled/running
     */
    fun isSyncWorkScheduled(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosByTagLiveData(SyncWorker.WORK_TAG).value
        return workInfos?.any { it.state.isFinished == false } ?: false
    }

    /** Private network callback for monitoring connectivity changes */
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    /**
     * Registers a network callback to trigger immediate sync when connectivity is restored.
     * Call this once during app startup (e.g., in Application.onCreate).
     *
     * @param context Application context
     */
    fun registerNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("SyncScheduler", "Network available - scheduling immediate sync")
                }
                // Trigger immediate sync when network becomes available
                scheduleImmediateSync(context)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("SyncScheduler", "Network lost")
                }
            }
        }

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)

        if (BuildConfig.DEBUG) {
            android.util.Log.d("SyncScheduler", "Network callback registered")
        }
    }

    /**
     * Unregisters the network callback.
     * Call this when the app is shutting down or sync is disabled.
     *
     * @param context Application context
     */
    fun unregisterNetworkCallback(context: Context) {
        if (networkCallback != null) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback!!)
            networkCallback = null

            if (BuildConfig.DEBUG) {
                android.util.Log.d("SyncScheduler", "Network callback unregistered")
            }
        }
    }
}
