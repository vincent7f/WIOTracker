package com.wiotracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wiotracker.data.database.entity.WifiScanRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiScanDao {
    @Insert
    suspend fun insert(record: WifiScanRecord)

    @Query("SELECT * FROM wifi_scan_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<WifiScanRecord>>

    @Query("SELECT * FROM wifi_scan_records WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp ORDER BY timestamp DESC")
    fun getRecordsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<WifiScanRecord>>

    @Query("SELECT COUNT(*) FROM wifi_scan_records WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp")
    suspend fun getCountByDateRange(startTimestamp: Long, endTimestamp: Long): Int

    @Query("SELECT DISTINCT scanSessionId FROM wifi_scan_records ORDER BY scanSessionId DESC")
    fun getAllScanSessionIds(): Flow<List<Long>>

    @Query("SELECT * FROM wifi_scan_records WHERE scanSessionId = :scanSessionId ORDER BY wifiName")
    suspend fun getRecordsBySessionId(scanSessionId: Long): List<WifiScanRecord>

    @Query("DELETE FROM wifi_scan_records")
    suspend fun deleteAll()
}
