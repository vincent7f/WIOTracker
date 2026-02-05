package com.wiotracker.util

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi

class WifiScanner(private val context: Context) {
    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    /**
     * Scan for WiFi networks and return matched WiFi names
     * @param keyword The keyword to match (case-insensitive partial match)
     * @return List of matched WiFi names
     */
    fun scanAndMatch(keyword: String): List<String> {
        if (keyword.isBlank()) {
            return emptyList()
        }

        val matchedWifis = mutableListOf<String>()
        val keywordLower = keyword.lowercase()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                scanWifiApi33Plus(keywordLower, matchedWifis)
            } else {
                scanWifiLegacy(keywordLower, matchedWifis)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return matchedWifis
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun scanWifiApi33Plus(keywordLower: String, matchedWifis: MutableList<String>) {
        // For API 33+, we need to use WifiNetworkSpecifier or request user to enable location
        // For now, we'll use the legacy method as fallback
        scanWifiLegacy(keywordLower, matchedWifis)
    }

    @Suppress("DEPRECATION")
    private fun scanWifiLegacy(keywordLower: String, matchedWifis: MutableList<String>) {
        wifiManager?.let { manager ->
            val scanResults = manager.scanResults
            scanResults?.forEach { result ->
                val ssid = result.SSID
                if (ssid.isNotBlank() && ssid.lowercase().contains(keywordLower)) {
                    matchedWifis.add(ssid)
                }
            }
        }
    }

    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager?.isWifiEnabled == true
    }
}
