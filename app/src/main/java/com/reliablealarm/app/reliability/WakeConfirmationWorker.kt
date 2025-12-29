package com.reliablealarm.app.reliability

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.reliablealarm.app.domain.StreakRepository
import com.reliablealarm.app.system.NotificationHelper

/**
 * Worker to show wake confirmation notification 30 minutes after alarm.
 *
 * WHY: Track if user actually woke up for streak system.
 * Delayed notification ensures user has had time to wake properly.
 *
 * Design:
 * - Scheduled by AlarmService when alarm triggers
 * - Fires 30 minutes after alarm time
 * - Shows notification asking "Did you wake up?"
 * - User responds: YES (on time) or NO (missed/overslept)
 */
class WakeConfirmationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d(TAG, "WakeConfirmationWorker executing")

        val alarmId = inputData.getString("alarm_id")
        if (alarmId == null) {
            Log.e(TAG, "No alarm ID provided")
            return Result.failure()
        }

        // Check if user already confirmed
        // WHY: Don't spam if they already responded
        val streakRepository = StreakRepository(context)
        val pending = streakRepository.getPendingConfirmations()
        val confirmation = pending.firstOrNull { it.alarmId == alarmId }

        if (confirmation == null) {
            Log.d(TAG, "No pending confirmation found for alarm $alarmId")
            return Result.success()
        }

        if (confirmation.isConfirmed()) {
            Log.d(TAG, "Confirmation already answered")
            return Result.success()
        }

        // Show notification
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showWakeConfirmationNotification(confirmation)

        Log.d(TAG, "Wake confirmation notification shown")
        return Result.success()
    }

    companion object {
        private const val TAG = "WakeConfirmationWorker"
    }
}