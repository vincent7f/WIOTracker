package com.wiotracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "wifi_scan_records",
    indices = [Index(value = ["scanSessionId"])]
)
data class WifiScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val wifiName: String,
    val matchedKeyword: String,
    val scanSessionId: Long = 0  // Same session ID for records from the same scan
)
