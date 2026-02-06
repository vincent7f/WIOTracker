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
        val tag = "WifiScanner"
        DebugLogManager.d(tag, "scanAndMatch called with keyword: '$keyword'")
        
        if (keyword.isBlank()) {
            DebugLogManager.w(tag, "Keyword is blank, returning empty list")
            return emptyList()
        }

        val matchedWifis = mutableListOf<String>()
        val keywordLower = keyword.lowercase()
        DebugLogManager.d(tag, "Searching for WiFi containing: '$keywordLower'")

        try {
            if (wifiManager == null) {
                DebugLogManager.e(tag, "WifiManager is null!")
                return emptyList()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                DebugLogManager.d(tag, "Using API 33+ scan method")
                scanWifiApi33Plus(keywordLower, matchedWifis)
            } else {
                DebugLogManager.d(tag, "Using legacy scan method")
                scanWifiLegacy(keywordLower, matchedWifis)
            }
            
            DebugLogManager.d(tag, "Scan completed. Found ${matchedWifis.size} matching WiFi(s)")
        } catch (e: Exception) {
            DebugLogManager.e(tag, "Exception during WiFi scan", e)
            DebugLogManager.e(tag, "Exception type: ${e.javaClass.simpleName}")
            DebugLogManager.e(tag, "Exception message: ${e.message}")
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
        val tag = "WifiScanner"
        wifiManager?.let { manager ->
            // Always try to trigger a new scan first
            DebugLogManager.d(tag, "Triggering WiFi scan...")
            val scanStarted = try {
                manager.startScan()
            } catch (e: Exception) {
                DebugLogManager.w(tag, "startScan() failed: ${e.message}")
                false
            }
            DebugLogManager.d(tag, "startScan() returned: $scanStarted")
            
            // Read scan results (may be from previous scan if new scan hasn't completed yet)
            // Note: WiFi scanning is asynchronous, so results may not be immediately available
            val scanResults = manager.scanResults
            DebugLogManager.d(tag, "scanResults count: ${scanResults?.size ?: 0}")
            
            if (scanResults == null || scanResults.isEmpty()) {
                DebugLogManager.w(tag, "scanResults is null or empty. This could mean:")
                DebugLogManager.w(tag, "  1. WiFi scan hasn't completed yet (normal for async scan)")
                DebugLogManager.w(tag, "  2. No WiFi networks in range")
                DebugLogManager.w(tag, "  3. Location permission not granted")
                DebugLogManager.w(tag, "  4. WiFi scanning is disabled by system")
                // Return empty list - no results available
                return
            }
            
            DebugLogManager.d(tag, "Processing ${scanResults.size} scan results...")
            var totalScanned = 0
            var matchedCount = 0
            
            scanResults.forEach { result ->
                totalScanned++
                val ssid = result.SSID
                val ssidLower = ssid.lowercase()
                val matches = ssid.isNotBlank() && ssidLower.contains(keywordLower)
                
                if (matches) {
                    matchedCount++
                    DebugLogManager.d(tag, "  MATCH [$matchedCount]: '$ssid' contains '$keywordLower'")
                    matchedWifis.add(ssid)
                } else if (totalScanned <= 10) {
                    // Log first 10 SSIDs for debugging
                    DebugLogManager.d(tag, "  No match [$totalScanned]: '$ssid'")
                }
            }
            
            DebugLogManager.d(tag, "Processed $totalScanned scan results, found $matchedCount matches")
            
            if (matchedCount == 0 && totalScanned > 0) {
                DebugLogManager.d(tag, "No matches found. Sample SSIDs scanned:")
                scanResults.take(5).forEachIndexed { index, result ->
                    DebugLogManager.d(tag, "  ${index + 1}. '${result.SSID}'")
                }
            }
        } ?: run {
            DebugLogManager.e(tag, "WifiManager is null in scanWifiLegacy")
        }
    }

    /**
     * Check if WiFi is enabled
     */
    fun isWifiEnabled(): Boolean {
        val enabled = wifiManager?.isWifiEnabled == true
        DebugLogManager.d("WifiScanner", "isWifiEnabled() = $enabled")
        return enabled
    }
}
