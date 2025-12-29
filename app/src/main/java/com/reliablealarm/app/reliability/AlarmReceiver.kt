package com.reliablealarm.app.reliability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reliablealarm.app.domain.AlarmScheduler
import com.reliablealarm.app.domain.AlarmRepository

/**
 * BroadcastReceiver for alarm triggers from AlarmManager.
 *
 * WHY: This is the PRIMARY entry point when AlarmManager fires.
 * Must be ultra-reliable and fast to start AlarmService.
 *
 * Responsibilities:
 * - Receive alarm broadcast from system
 * - Start AlarmService immediately (foreground service)
 * - Cancel WorkManager backup to prevent double-firing
 * - Reschedule alarm if repeating
 *
 * Design considerations:
 * - BroadcastReceiver has ~10 seconds before system kills it
 * - Must delegate to Service for long-running operations
 * - Must not block main thread
 * - Must handle being called while app is dead
 *
 * Thread-safety: onReceive runs on main thread, keep it FAST.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered: ${intent.action}")

        if (intent.action != AlarmScheduler.ACTION_ALARM_TRIGGER) {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }

        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        if (alarmId == null) {
            Log.e(TAG, "No alarm ID in intent")
            return
        }

        Log.d(TAG, "Alarm triggered: $alarmId")

        // Verify alarm still exists and is enabled
        // WHY: User may have deleted/disabled alarm after scheduling
        val repository = AlarmRepository(context)
        val alarm = repository.getAlarm(alarmId)

        if (alarm == null) {
            Log.w(TAG, "Alarm $alarmId not found in repository")
            return
        }

        if (!alarm.isEnabled) {
            Log.d(TAG, "Alarm $alarmId is disabled, ignoring trigger")
            return
        }

        // Cancel WorkManager backup to prevent double-firing
        // WHY: AlarmManager fired successfully, don't need backup anymore
        val scheduler = AlarmScheduler(context)
        scheduler.cancelAlarm(alarmId)

        // Start foreground service to play alarm
        // WHY: BroadcastReceiver has limited lifetime (~10 seconds)
        // Service can run longer and hold wake lock
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_TRIGGERED_BY, "AlarmManager")
        }

        // Start as foreground service for maximum reliability
        // WHY: Foreground services are protected from being killed
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "Started AlarmService for alarm $alarmId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmService", e)
            // Fallback: try to show activity directly
            startAlarmActivity(context, alarmId)
        }

        // Reschedule alarm if repeating
        // WHY: Repeating alarms need next occurrence scheduled
        scheduler.rescheduleAlarmAfterTrigger(alarmId)

        // Keep receiver alive until service starts
        // WHY: Without this, receiver may be killed before service starts
        val pendingResult = goAsync()
        Thread {
            // Give service time to start
            Thread.sleep(1000)
            pendingResult.finish()
        }.start()
    }

    /**
     * Fallback method to start alarm activity directly.
     * WHY: If service fails to start, at least show UI to user.
     *
     * This should rarely happen, but handles edge cases like:
     * - Service startup restrictions on some OEMs
     * - System under extreme memory pressure
     */
    private fun startAlarmActivity(context: Context, alarmId: String) {
        try {
            val activityIntent = Intent(context, com.reliablealarm.app.ui.AlarmTriggerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            context.startActivity(activityIntent)
            Log.d(TAG, "Started AlarmTriggerActivity as fallback")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmTriggerActivity", e)
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_TRIGGERED_BY = "triggered_by"
    }
}