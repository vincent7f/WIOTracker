package com.wiotracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wiotracker.util.WorkManagerHelper

/**
 * BroadcastReceiver to restart WorkManager tasks after device reboot
 * This ensures the app continues to scan WiFi daily even after device restart
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule periodic scan after device reboot
            WorkManagerHelper.schedulePeriodicScan(context)
        }
    }
}
