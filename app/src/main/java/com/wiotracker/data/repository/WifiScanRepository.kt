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

    fun getDailyStatsFlow(month: Int, year: Int): Flow<Map<String, Int>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimestamp = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endTimestamp = calendar.timeInMillis

        return wifiScanDao.getRecordsByDateRange(startTimestamp, endTimestamp).map { records ->
            val statsMap = mutableMapOf<String, Int>()
            records.forEach { record ->
                val date = formatDate(record.timestamp)
                statsMap[date] = (statsMap[date] ?: 0) + 1
            }
            statsMap
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
        
        return wifiScanDao.getCountByDateRange(startOfDay, endOfDay)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(timestamp)
    }
}
