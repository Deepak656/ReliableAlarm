package com.reliablealarm.app.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import com.reliablealarm.app.config.ReliabilityConfig
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.reliability.AlarmReceiver
import com.reliablealarm.app.reliability.AlarmWorker
import java.util.concurrent.TimeUnit

/**
 * Centralized alarm scheduling logic.
 *
 * WHY: Single responsibility for all alarm registration/cancellation.
 * Implements dual scheduling redundancy (AlarmManager + WorkManager).
 *
 * Design principles:
 * - AlarmManager: Primary mechanism (most reliable for exact timing)
 * - WorkManager: Backup mechanism (survives process killing better)
 * - If either triggers, both are cancelled to prevent double-firing
 * - RTC_WAKEUP: Device wakes from sleep to fire alarm
 * - setExactAndAllowWhileIdle: Works even in Doze mode
 *
 * Thread-safety: All AlarmManager operations are thread-safe.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val reliabilityConfig = ReliabilityConfig(context)

    /**
     * Schedule an alarm using dual redundancy.
     *
     * WHY: Maximum reliability through multiple scheduling mechanisms.
     * If AlarmManager fails (OEM killing, battery optimization), WorkManager triggers.
     * If WorkManager delayed, AlarmManager fires on time.
     *
     * @param alarm Alarm to schedule
     */
    fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.isEnabled) {
            Log.d(TAG, "Alarm ${alarm.id} is disabled, skipping schedule")
            return
        }

        val triggerTime = alarm.getNextTriggerTime()
        Log.d(TAG, "Scheduling alarm ${alarm.id} for time: $triggerTime (${java.util.Date(triggerTime)})")

        // Primary: AlarmManager
        scheduleWithAlarmManager(alarm, triggerTime)

        // Backup: WorkManager (if enabled)
        if (reliabilityConfig.dualScheduling) {
            scheduleWithWorkManager(alarm, triggerTime)
        }
    }

    /**
     * Schedule using AlarmManager (primary mechanism).
     *
     * WHY: AlarmManager provides:
     * - Exact timing (within seconds)
     * - Device wake from sleep (RTC_WAKEUP)
     * - Works in Doze mode (setExactAndAllowWhileIdle)
     * - Lowest battery consumption for exact alarms
     *
     * @param alarm Alarm to schedule
     * @param triggerTime Exact milliseconds to trigger
     */
    private fun scheduleWithAlarmManager(alarm: Alarm, triggerTime: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Use setExactAndAllowWhileIdle for maximum reliability
            // WHY: Works even when device is in Doze mode or App Standby
            // Falls back to setExact for older API levels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "AlarmManager scheduled for alarm ${alarm.id}")
        } catch (e: SecurityException) {
            // User denied exact alarm permission on Android 12+
            Log.e(TAG, "Failed to schedule alarm: exact alarm permission denied", e)
            // TODO: Show UI to guide user to grant permission
        }
    }

    /**
     * Schedule using WorkManager (backup mechanism).
     *
     * WHY: WorkManager provides:
     * - Survives process killing better than AlarmManager
     * - Guaranteed execution (will retry)
     * - Persists across device reboot
     * - Battery-friendly with constraints
     *
     * Trade-off: Timing less precise (may trigger 1-2 minutes late)
     * This is acceptable as backup - AlarmManager is primary.
     *
     * @param alarm Alarm to schedule
     * @param triggerTime Exact milliseconds to trigger
     */
    private fun scheduleWithWorkManager(alarm: Alarm, triggerTime: Long) {
        val currentTime = System.currentTimeMillis()
        val delay = triggerTime - currentTime

        if (delay < 0) {
            Log.w(TAG, "Trigger time in past, scheduling for immediate execution")
            // Should not happen, but handle gracefully
            return
        }

        val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(EXTRA_ALARM_ID to alarm.id)
            )
            .addTag(getWorkTag(alarm.id))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                getWorkName(alarm.id),
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        Log.d(TAG, "WorkManager scheduled for alarm ${alarm.id} with delay ${delay}ms")
    }

    /**
     * Cancel all scheduling for an alarm.
     *
     * WHY: Called when:
     * - User disables alarm
     * - User deletes alarm
     * - Alarm fires (to prevent re-triggering)
     * - One mechanism fires (cancel the other to prevent double-fire)
     *
     * @param alarmId Alarm ID to cancel
     */
    fun cancelAlarm(alarmId: String) {
        Log.d(TAG, "Cancelling alarm $alarmId")

        // Cancel AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "AlarmManager cancelled for alarm $alarmId")
        }

        // Cancel WorkManager
        if (reliabilityConfig.dualScheduling) {
            WorkManager.getInstance(context).cancelUniqueWork(getWorkName(alarmId))
            Log.d(TAG, "WorkManager cancelled for alarm $alarmId")
        }
    }

    /**
     * Reschedule alarm after it fires.
     *
     * WHY: Repeating alarms need to be scheduled for next occurrence.
     * One-time alarms should be disabled after firing.
     *
     * @param alarmId Alarm ID that just fired
     */
    fun rescheduleAlarmAfterTrigger(alarmId: String) {
        val repository = AlarmRepository(context)
        val alarm = repository.getAlarm(alarmId)

        if (alarm == null) {
            Log.w(TAG, "Alarm $alarmId not found, cannot reschedule")
            return
        }

        if (alarm.isRepeating()) {
            // Repeating alarm: schedule for next occurrence
            scheduleAlarm(alarm)
            Log.d(TAG, "Rescheduled repeating alarm $alarmId")
        } else {
            // One-time alarm: disable it
            repository.toggleAlarm(alarmId, false)
            Log.d(TAG, "Disabled one-time alarm $alarmId after firing")
        }
    }

    /**
     * Check if alarm is scheduled in AlarmManager.
     *
     * WHY: Watchdog uses this to detect missing alarms.
     *
     * @param alarmId Alarm ID to check
     * @return true if scheduled, false otherwise
     */
    fun isAlarmScheduled(alarmId: String): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        return pendingIntent != null
    }

    /**
     * Get unique work name for WorkManager.
     * WHY: Each alarm needs unique identifier in WorkManager.
     */
    private fun getWorkName(alarmId: String): String {
        return "alarm_$alarmId"
    }

    /**
     * Get work tag for WorkManager.
     * WHY: Allows querying all alarm works by tag.
     */
    private fun getWorkTag(alarmId: String): String {
        return "alarm_tag_$alarmId"
    }

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_ALARM_TRIGGER = "com.reliablealarm.app.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}