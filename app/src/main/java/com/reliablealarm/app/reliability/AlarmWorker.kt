package com.reliablealarm.app.reliability

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.reliablealarm.app.domain.AlarmScheduler
import com.reliablealarm.app.domain.AlarmRepository

/**
 * WorkManager Worker as backup alarm trigger mechanism.
 *
 * WHY: Redundancy layer if AlarmManager fails or is killed by OEM.
 * WorkManager is more resilient to:
 * - Process killing
 * - Battery optimization
 * - Doze mode restrictions
 *
 * Trade-off: Less precise timing (may be 1-2 min late)
 * This is acceptable as BACKUP - AlarmManager is primary.
 *
 * Design:
 * - Scheduled at same time as AlarmManager
 * - If AlarmManager fires first, this is cancelled
 * - If this fires, cancel AlarmManager to prevent double-fire
 * - Start same AlarmService as AlarmReceiver
 */
class AlarmWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d(TAG, "AlarmWorker triggered")

        val alarmId = inputData.getString(AlarmScheduler.EXTRA_ALARM_ID)
        if (alarmId == null) {
            Log.e(TAG, "No alarm ID in work data")
            return Result.failure()
        }

        Log.d(TAG, "Processing alarm: $alarmId")

        // Verify alarm still exists and is enabled
        val repository = AlarmRepository(context)
        val alarm = repository.getAlarm(alarmId)

        if (alarm == null) {
            Log.w(TAG, "Alarm $alarmId not found")
            return Result.failure()
        }

        if (!alarm.isEnabled) {
            Log.d(TAG, "Alarm $alarmId is disabled")
            return Result.success()
        }

        // Cancel AlarmManager to prevent double-firing
        // WHY: WorkManager fired, so AlarmManager didn't (or hasn't yet)
        val scheduler = AlarmScheduler(context)
        scheduler.cancelAlarm(alarmId)

        // Start AlarmService
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_TRIGGERED_BY, "WorkManager")
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "Started AlarmService from Worker")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AlarmService from Worker", e)
            return Result.failure()
        }

        // Reschedule if repeating
        scheduler.rescheduleAlarmAfterTrigger(alarmId)

        return Result.success()
    }

    companion object {
        private const val TAG = "AlarmWorker"
    }
}