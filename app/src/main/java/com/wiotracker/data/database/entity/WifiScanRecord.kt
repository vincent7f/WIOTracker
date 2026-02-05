package com.wiotracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_scan_records")
data class WifiScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val wifiName: String,
    val matchedKeyword: String
)
