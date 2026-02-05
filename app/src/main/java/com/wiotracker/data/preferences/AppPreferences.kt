package com.wiotracker.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "wifi_tracker_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_TARGET_WIFI_NAME = "target_wifi_name"
        private const val KEY_SCAN_START_HOUR = "scan_start_hour"
        private const val KEY_SCAN_END_HOUR = "scan_end_hour"

        private const val DEFAULT_TARGET_WIFI_NAME = ""
        private const val DEFAULT_SCAN_START_HOUR = 8
        private const val DEFAULT_SCAN_END_HOUR = 20
    }

    var targetWifiName: String
        get() = prefs.getString(KEY_TARGET_WIFI_NAME, DEFAULT_TARGET_WIFI_NAME) ?: DEFAULT_TARGET_WIFI_NAME
        set(value) = prefs.edit().putString(KEY_TARGET_WIFI_NAME, value).apply()

    var scanStartHour: Int
        get() = prefs.getInt(KEY_SCAN_START_HOUR, DEFAULT_SCAN_START_HOUR)
        set(value) = prefs.edit().putInt(KEY_SCAN_START_HOUR, value).apply()

    var scanEndHour: Int
        get() = prefs.getInt(KEY_SCAN_END_HOUR, DEFAULT_SCAN_END_HOUR)
        set(value) = prefs.edit().putInt(KEY_SCAN_END_HOUR, value).apply()
}
