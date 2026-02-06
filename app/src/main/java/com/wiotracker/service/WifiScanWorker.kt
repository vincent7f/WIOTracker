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
            // Supports both normal range (e.g., 8-20) and overnight range (e.g., 22-6)
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isInTimeRange = if (startHour < endHour) {
                // Normal range: startHour to endHour (e.g., 8:00 to 20:00)
                currentHour >= startHour && currentHour < endHour
            } else {
                // Overnight range: startHour to endHour across midnight (e.g., 22:00 to 06:00)
                currentHour >= startHour || currentHour < endHour
            }
            
            if (!isInTimeRange) {
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

            // Save records to database with the same scanSessionId for this scan
            if (matchedWifis.isNotEmpty()) {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = WifiScanRepository(database.wifiScanDao())
                val timestamp = System.currentTimeMillis()
                // Use timestamp as scanSessionId to group records from the same scan
                val scanSessionId = timestamp

                matchedWifis.forEach { wifiName ->
                    repository.insertRecord(
                        com.wiotracker.data.database.entity.WifiScanRecord(
                            timestamp = timestamp,
                            wifiName = wifiName,
                            matchedKeyword = targetWifiName,
                            scanSessionId = scanSessionId
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
