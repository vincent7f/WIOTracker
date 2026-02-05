package com.wiotracker.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.preferences.AppPreferences
import com.wiotracker.data.repository.WifiScanRepository
import com.wiotracker.util.WifiScanner
import java.util.Calendar

class WifiScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val preferences = AppPreferences(applicationContext)
            val targetWifiName = preferences.targetWifiName
            val startHour = preferences.scanStartHour
            val endHour = preferences.scanEndHour

            // Check if we're in the scan time range
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (currentHour < startHour || currentHour >= endHour) {
                return Result.success() // Not in scan time range, skip
            }

            // Check if target WiFi name is configured
            if (targetWifiName.isBlank()) {
                return Result.success() // No target configured, skip
            }

            // Check if WiFi is enabled
            val wifiScanner = WifiScanner(applicationContext)
            if (!wifiScanner.isWifiEnabled()) {
                return Result.success() // WiFi not enabled, skip silently
            }

            // Scan and match WiFi
            val matchedWifis = wifiScanner.scanAndMatch(targetWifiName)

            // Save records to database
            if (matchedWifis.isNotEmpty()) {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = WifiScanRepository(database.wifiScanDao())
                val timestamp = System.currentTimeMillis()

                matchedWifis.forEach { wifiName ->
                    repository.insertRecord(
                        com.wiotracker.data.database.entity.WifiScanRecord(
                            timestamp = timestamp,
                            wifiName = wifiName,
                            matchedKeyword = targetWifiName
                        )
                    )
                }
            }

            Result.success()
        } catch (e: SecurityException) {
            // Permission issue - don't retry, just skip
            android.util.Log.e("WifiScanWorker", "Permission denied for WiFi scan", e)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("WifiScanWorker", "Error during WiFi scan", e)
            // Retry for other errors
            Result.retry()
        }
    }
}
