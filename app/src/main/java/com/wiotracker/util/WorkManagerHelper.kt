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

    fun schedulePeriodicScan(context: Context) {
        val preferences = AppPreferences(context)
        // WorkManager requires minimum 15 minutes for periodic work
        val scanIntervalMinutes = preferences.scanIntervalMinutes.coerceAtLeast(15)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<WifiScanWorker>(
            scanIntervalMinutes.toLong(), TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelPeriodicScan(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
