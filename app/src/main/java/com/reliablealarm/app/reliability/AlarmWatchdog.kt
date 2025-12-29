package com.reliablealarm.app.reliability

import android.content.Context
import android.util.Log
import com.reliablealarm.app.config.ReliabilityConfig
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.AlarmScheduler

/**
 * Watchdog to verify all alarms are properly registered.
 *
 * WHY: Alarms can be silently cleared by:
 * - OS updates
 * - OEM battery optimization
 * - System maintenance tasks
 * - User clearing app cache (partially)
 *
 * This watchdog runs on EVERY app launch to detect and fix missing alarms.
 *
 * Design:
 * - Check each enabled alarm in repository
 * - Verify it's scheduled in AlarmManager
 * - If missing, re-register immediately
 * - Log all findings for debugging
 */
object AlarmWatchdog {

    private const val TAG = "AlarmWatchdog"

    /**
     * Verify all alarms and re-register if missing.
     *
     * WHY: Proactive protection against alarm loss.
     * Called from AlarmApplication.onCreate() on every app launch.
     *
     * @param context Application context
     */
    fun verify(context: Context) {
        val config = ReliabilityConfig(context)
        if (!config.alarmWatchdog) {
            Log.d(TAG, "Alarm watchdog disabled")
            return
        }

        Log.d(TAG, "Starting alarm watchdog verification")

        val repository = AlarmRepository(context)
        val scheduler = AlarmScheduler(context)

        val enabledAlarms = repository.getEnabledAlarms()
        Log.d(TAG, "Verifying ${enabledAlarms.size} enabled alarms")

        var missingCount = 0
        var registeredCount = 0

        for (alarm in enabledAlarms) {
            val isScheduled = scheduler.isAlarmScheduled(alarm.id)

            if (!isScheduled) {
                Log.w(TAG, "MISSING ALARM DETECTED: ${alarm.id} - ${alarm.name}")
                // Re-register immediately
                try {
                    scheduler.scheduleAlarm(alarm)
                    missingCount++
                    Log.d(TAG, "Re-registered missing alarm: ${alarm.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to re-register alarm ${alarm.id}", e)
                }
            } else {
                registeredCount++
            }
        }

        Log.d(TAG, "Watchdog verification complete:")
        Log.d(TAG, "  - Registered: $registeredCount")
        Log.d(TAG, "  - Missing (re-registered): $missingCount")

        if (missingCount > 0) {
            Log.w(TAG, "WARNING: Found and fixed $missingCount missing alarms")
        }
    }
}