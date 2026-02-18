package com.wiotracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.wiotracker.R
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
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "wifi_scan_channel"
        private const val CHANNEL_NAME = "WiFi扫描服务"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WiFi扫描后台服务通知"
                setShowBadge(false)
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("WiFi扫描中")
            .setContentText("正在后台扫描WiFi网络...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true) // Don't make sound
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        val tag = "WifiScanWorker"
        DebugLogManager.d(tag, "========== WifiScanWorker started ==========")
        
        // Set foreground service to ensure background execution
        setForeground(createForegroundInfo())
        
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
            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val startTimeInMinutes = startHour * 60
            val endTimeInMinutes = endHour * 60
            
            val isInTimeRange = if (startHour < endHour) {
                // Normal range: startHour to endHour (e.g., 8:00 to 20:00)
                // Include start time, exclude end time (e.g., 8:00-20:00 means 8:00 to 19:59)
                currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
            } else if (startHour > endHour) {
                // Overnight range: startHour to endHour across midnight (e.g., 22:00 to 06:00)
                // This means from startHour to 23:59, or from 00:00 to endHour
                currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutes
            } else {
                // startHour == endHour: This should not happen based on validation, but handle it
                // If start and end are the same, allow all times (or none - we'll allow all for safety)
                DebugLogManager.w(tag, "WARNING: startHour == endHour ($startHour), allowing scan")
                true
            }
            
            DebugLogManager.d(tag, "Time check:")
            DebugLogManager.d(tag, "  - Current time: ${String.format("%02d:%02d", currentHour, currentMinute)}")
            DebugLogManager.d(tag, "  - Time range: ${String.format("%02d:00", startHour)} - ${String.format("%02d:00", endHour)}")
            DebugLogManager.d(tag, "  - Current time in minutes: $currentTimeInMinutes")
            DebugLogManager.d(tag, "  - Start time in minutes: $startTimeInMinutes")
            DebugLogManager.d(tag, "  - End time in minutes: $endTimeInMinutes")
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
                            scanSessionId = scanSessionId,
                            scanType = "periodic"
                        )
                        repository.insertRecord(record)
                        DebugLogManager.d(tag, "  Saved record: $wifiName (periodic)")
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
