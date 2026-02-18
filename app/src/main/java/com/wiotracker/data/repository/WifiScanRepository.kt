package com.wiotracker.data.repository

import com.wiotracker.data.database.dao.WifiScanDao
import com.wiotracker.data.database.entity.WifiScanRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WifiScanRepository(
    private val wifiScanDao: WifiScanDao
) {
    fun getAllRecords(): Flow<List<WifiScanRecord>> = wifiScanDao.getAllRecords()

    suspend fun insertRecord(record: WifiScanRecord) {
        wifiScanDao.insert(record)
    }

    fun getAllScanSessions(): Flow<List<Long>> = wifiScanDao.getAllScanSessionIds()

    suspend fun getRecordsBySessionId(scanSessionId: Long): List<WifiScanRecord> {
        return wifiScanDao.getRecordsBySessionId(scanSessionId)
    }

    fun getDailyStatsFlow(month: Int, year: Int): Flow<Map<String, Pair<Int, Int>>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimestamp = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endTimestamp = calendar.timeInMillis

        return wifiScanDao.getRecordsByDateRange(startTimestamp, endTimestamp).map { records ->
            // Group by scanSessionId to count unique scan sessions per date
            // Separate periodic and manual scans
            val periodicSessionsMap = mutableMapOf<String, MutableSet<Long>>()
            val totalSessionsMap = mutableMapOf<String, MutableSet<Long>>()
            
            records.forEach { record ->
                val date = formatDate(record.timestamp)
                
                // Add to total sessions
                val totalSessionSet = totalSessionsMap.getOrPut(date) { mutableSetOf() }
                totalSessionSet.add(record.scanSessionId)
                
                // Add to periodic sessions if scanType is "periodic"
                if (record.scanType == "periodic") {
                    val periodicSessionSet = periodicSessionsMap.getOrPut(date) { mutableSetOf() }
                    periodicSessionSet.add(record.scanSessionId)
                }
            }
            
            // Convert to count map: Pair(periodicCount, totalCount)
            val result = mutableMapOf<String, Pair<Int, Int>>()
            totalSessionsMap.keys.forEach { date ->
                val periodicCount = periodicSessionsMap[date]?.size ?: 0
                val totalCount = totalSessionsMap[date]?.size ?: 0
                result[date] = Pair(periodicCount, totalCount)
            }
            result
        }
    }

    suspend fun getCountForDate(date: String): Int {
        // Parse date string (yyyy-MM-dd) to timestamp range
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateObj = sdf.parse(date) ?: return 0
        val calendar = Calendar.getInstance()
        calendar.time = dateObj
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        // Count unique scan sessions for this date
        // Note: This method is not currently used, but kept for compatibility
        // The calendar view uses getDailyStatsFlow instead
        return wifiScanDao.getCountByDateRange(startOfDay, endOfDay)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(timestamp)
    }
}
