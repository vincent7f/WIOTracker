package com.wiotracker.ui.log

import com.wiotracker.data.database.entity.WifiScanRecord

/**
 * Represents a single scan session with summary information
 */
data class ScanSession(
    val scanSessionId: Long,
    val timestamp: Long,
    val matchedKeyword: String,
    val wifiCount: Int,
    val wifiNames: List<String>
) {
    companion object {
        fun fromRecords(records: List<WifiScanRecord>): ScanSession? {
            if (records.isEmpty()) return null
            
            val firstRecord = records.first()
            return ScanSession(
                scanSessionId = firstRecord.scanSessionId,
                timestamp = firstRecord.timestamp,
                matchedKeyword = firstRecord.matchedKeyword,
                wifiCount = records.size,
                wifiNames = records.map { it.wifiName }.sorted()
            )
        }
    }
}
