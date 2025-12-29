package com.reliablealarm.app.reliability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reliablealarm.app.config.ReliabilityConfig
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.AlarmScheduler

/**
 * BroadcastReceiver for device boot events.
 *
 * WHY: AlarmManager alarms are CLEARED on device reboot.
 * This receiver must re-register ALL enabled alarms.
 *
 * Handles multiple boot actions:
 * - BOOT_COMPLETED: Standard boot
 * - QUICKBOOT_POWERON: Some OEM quick boot
 * - LOCKED_BOOT_COMPLETED: Direct Boot mode (Android 7+)
 * - MY_PACKAGE_REPLACED: App update
 *
 * directBootAware=true: Runs before user unlocks device
 * WHY: Ensures alarms work even if device boots and stays locked
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "BootReceiver triggered: ${intent.action}")

        val config = ReliabilityConfig(context)
        if (!config.bootPersistence) {
            Log.d(TAG, "Boot persistence disabled, skipping re-registration")
            return
        }

        // Re-register all enabled alarms
        // WHY: System cleared all alarms on reboot
        reRegisterAllAlarms(context)
    }

    /**
     * Re-register all enabled alarms after boot.
     * WHY: System clears AlarmManager on reboot.
     *
     * Must be done as early as possible to minimize chance of missing alarm.
     */
    private fun reRegisterAllAlarms(context: Context) {
        Log.d(TAG, "Re-registering all alarms after boot")

        val repository = AlarmRepository(context)
        val scheduler = AlarmScheduler(context)

        val enabledAlarms = repository.getEnabledAlarms()
        Log.d(TAG, "Found ${enabledAlarms.size} enabled alarms to re-register")

        var successCount = 0
        for (alarm in enabledAlarms) {
            try {
                scheduler.scheduleAlarm(alarm)
                successCount++
                Log.d(TAG, "Re-registered alarm: ${alarm.id} - ${alarm.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-register alarm ${alarm.id}", e)
            }
        }

        Log.d(TAG, "Re-registration complete: $successCount/${enabledAlarms.size} alarms")
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}