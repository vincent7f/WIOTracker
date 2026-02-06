package com.wiotracker.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.wiotracker.data.preferences.AppPreferences
import com.wiotracker.service.WifiScanWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    private const val WORK_NAME = "wifi_scan_work"
    private const val TAG = "WorkManagerHelper"

    fun schedulePeriodicScan(context: Context) {
        DebugLogManager.d(TAG, "========== schedulePeriodicScan called ==========")
        
        val preferences = AppPreferences(context)
        // WorkManager requires minimum 15 minutes for periodic work
        val scanIntervalMinutes = preferences.scanIntervalMinutes.coerceAtLeast(15)
        
        DebugLogManager.d(TAG, "Configuration:")
        DebugLogManager.d(TAG, "  - scanIntervalMinutes: ${preferences.scanIntervalMinutes}")
        DebugLogManager.d(TAG, "  - actual interval (after min 15): $scanIntervalMinutes")
        DebugLogManager.d(TAG, "  - targetWifiName: '${preferences.targetWifiName}'")
        DebugLogManager.d(TAG, "  - startHour: ${preferences.scanStartHour}")
        DebugLogManager.d(TAG, "  - endHour: ${preferences.scanEndHour}")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WifiScanWorker>(
            scanIntervalMinutes.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        DebugLogManager.d(TAG, "Enqueuing periodic work: $WORK_NAME")
        DebugLogManager.d(TAG, "  - Interval: $scanIntervalMinutes minutes")
        DebugLogManager.d(TAG, "  - WorkRequest ID: ${workRequest.id}")
        
        try {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            DebugLogManager.d(TAG, "Successfully enqueued periodic work")
        } catch (e: Exception) {
            DebugLogManager.e(TAG, "Failed to enqueue periodic work", e)
        }
        
        DebugLogManager.d(TAG, "========== schedulePeriodicScan completed ==========")
    }

    fun cancelPeriodicScan(context: Context) {
        DebugLogManager.d(TAG, "Cancelling periodic work: $WORK_NAME")
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        DebugLogManager.d(TAG, "Periodic work cancelled")
    }
}
