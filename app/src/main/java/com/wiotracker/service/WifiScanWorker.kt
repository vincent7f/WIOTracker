package com.wiotracker.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wiotracker.data.database.AppDatabase
import com.wiotracker.data.preferences.AppPreferences
import com.wiotracker.data.repository.WifiScanRepository
import com.wiotracker.util.DebugLogManager
import com.wiotracker.util.WifiScanner
import java.util.Calendar

class WifiScanWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val tag = "WifiScanWorker"
        DebugLogManager.d(tag, "========== WifiScanWorker started ==========")
        
        return try {
            val preferences = AppPreferences(applicationContext)
            val targetWifiName = preferences.targetWifiName
            val startHour = preferences.scanStartHour
            val endHour = preferences.scanEndHour

            DebugLogManager.d(tag, "Configuration loaded:")
            DebugLogManager.d(tag, "  - targetWifiName: '$targetWifiName'")
            DebugLogManager.d(tag, "  - startHour: $startHour")
            DebugLogManager.d(tag, "  - endHour: $endHour")

            // Check if target WiFi name is configured
            if (targetWifiName.isBlank()) {
                DebugLogManager.w(tag, "SKIP: targetWifiName is blank, skipping scan")
                return Result.success() // No target configured, skip
            }

            // Check if we're in the scan time range
            // Supports both normal range (e.g., 8-20) and overnight range (e.g., 22-6)
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val isInTimeRange = if (startHour < endHour) {
                // Normal range: startHour to endHour (e.g., 8:00 to 20:00)
                currentHour >= startHour && currentHour < endHour
            } else {
                // Overnight range: startHour to endHour across midnight (e.g., 22:00 to 06:00)
                currentHour >= startHour || currentHour < endHour
            }
            
            DebugLogManager.d(tag, "Time check:")
            DebugLogManager.d(tag, "  - Current time: ${String.format("%02d:%02d", currentHour, currentMinute)}")
            DebugLogManager.d(tag, "  - Time range: ${String.format("%02d:00", startHour)} - ${String.format("%02d:00", endHour)}")
            DebugLogManager.d(tag, "  - Is in time range: $isInTimeRange")
            
            if (!isInTimeRange) {
                DebugLogManager.d(tag, "SKIP: Not in scan time range")
                return Result.success() // Not in scan time range, skip
            }

            // Check if WiFi is enabled
            val wifiScanner = WifiScanner(applicationContext)
            val isWifiEnabled = wifiScanner.isWifiEnabled()
            DebugLogManager.d(tag, "WiFi status: enabled=$isWifiEnabled")
            
            if (!isWifiEnabled) {
                DebugLogManager.w(tag, "SKIP: WiFi is not enabled")
                return Result.success() // WiFi not enabled, skip silently
            }

            // Scan and match WiFi
            DebugLogManager.d(tag, "Starting WiFi scan for keyword: '$targetWifiName'")
            val matchedWifis = wifiScanner.scanAndMatch(targetWifiName)
            DebugLogManager.d(tag, "Scan completed. Found ${matchedWifis.size} matching WiFi(s):")
            matchedWifis.forEachIndexed { index, wifiName ->
                DebugLogManager.d(tag, "  ${index + 1}. $wifiName")
            }

            // Save records to database with the same scanSessionId for this scan
            if (matchedWifis.isNotEmpty()) {
                DebugLogManager.d(tag, "Saving ${matchedWifis.size} record(s) to database...")
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = WifiScanRepository(database.wifiScanDao())
                val timestamp = System.currentTimeMillis()
                // Use timestamp as scanSessionId to group records from the same scan
                val scanSessionId = timestamp

                try {
                    matchedWifis.forEach { wifiName ->
                        val record = com.wiotracker.data.database.entity.WifiScanRecord(
                            timestamp = timestamp,
                            wifiName = wifiName,
                            matchedKeyword = targetWifiName,
                            scanSessionId = scanSessionId
                        )
                        repository.insertRecord(record)
                        DebugLogManager.d(tag, "  Saved record: $wifiName")
                    }
                    DebugLogManager.d(tag, "Successfully saved ${matchedWifis.size} record(s) to database")
                } catch (e: Exception) {
                    DebugLogManager.e(tag, "Failed to save records to database", e)
                    throw e
                }
            } else {
                DebugLogManager.d(tag, "No matching WiFi found, nothing to save")
            }

            DebugLogManager.d(tag, "========== WifiScanWorker completed successfully ==========")
            Result.success()
        } catch (e: SecurityException) {
            // Permission issue - don't retry, just skip
            DebugLogManager.e(tag, "========== ERROR: Permission denied for WiFi scan ==========", e)
            DebugLogManager.e(tag, "Exception details: ${e.message}", e)
            Result.success()
        } catch (e: Exception) {
            DebugLogManager.e(tag, "========== ERROR: Exception during WiFi scan ==========", e)
            DebugLogManager.e(tag, "Exception type: ${e.javaClass.simpleName}")
            DebugLogManager.e(tag, "Exception message: ${e.message}")
            e.printStackTrace()
            // Retry for other errors
            Result.retry()
        }
    }
}
