package com.reliablealarm.app.reliability

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.reliablealarm.app.config.ReliabilityConfig
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.AlarmScheduler
import com.reliablealarm.app.domain.StreakRepository
import com.reliablealarm.app.system.AudioController
import com.reliablealarm.app.system.NotificationHelper
import kotlinx.coroutines.*
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Foreground service for alarm playback and enforcement.
 *
 * WHY: Foreground service has highest priority and won't be killed by system.
 * Holds partial wake lock to keep CPU running during alarm.
 *
 * Responsibilities:
 * - Start foreground notification
 * - Acquire wake lock
 * - Start alarm audio playback
 * - Launch AlarmTriggerActivity
 * - Schedule wake confirmation notification (30 min later)
 * - Handle auto re-ring if dismissed quickly
 * - Handle low memory situations
 * - Clean up resources on stop
 *
 * Lifecycle:
 * 1. Started by AlarmReceiver or AlarmWorker
 * 2. Immediately becomes foreground service
 * 3. Acquires wake lock
 * 4. Starts audio and UI
 * 5. Stays alive until alarm dismissed or snoozed
 * 6. Cleans up and stops
 */
class AlarmService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var audioController: AudioController
    private lateinit var reliabilityConfig: ReliabilityConfig
    private lateinit var notificationHelper: NotificationHelper

    private var alarmId: String? = null
    private var startTime: Long = 0
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService onCreate")

        reliabilityConfig = ReliabilityConfig(this)
        audioController = AudioController(this, reliabilityConfig)
        notificationHelper = NotificationHelper(this)

        // Acquire partial wake lock
        // WHY: Keeps CPU running even if screen off
        if (reliabilityConfig.foregroundService) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ReliableAlarm::AlarmWakeLock"
            )
            wakeLock.acquire(10 * 60 * 1000L) // 10 minutes max
            Log.d(TAG, "Wake lock acquired")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlarmService onStartCommand")

        alarmId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        val triggeredBy = intent?.getStringExtra(AlarmReceiver.EXTRA_TRIGGERED_BY) ?: "Unknown"

        if (alarmId == null) {
            Log.e(TAG, "No alarm ID provided")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d(TAG, "Alarm service started for: $alarmId (triggered by: $triggeredBy)")
        startTime = System.currentTimeMillis()

        // Start foreground service immediately
        // WHY: Prevents service from being killed
        if (reliabilityConfig.foregroundService) {
            val notification = notificationHelper.createForegroundNotification(alarmId!!)
            startForeground(NOTIFICATION_ID_FOREGROUND, notification)
            Log.d(TAG, "Started foreground service")
        }

        // Load alarm details
        val repository = AlarmRepository(this)
        val alarm = repository.getAlarm(alarmId!!)

        if (alarm == null) {
            Log.e(TAG, "Alarm not found: $alarmId")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start audio playback
        audioController.startAlarm(alarm)
        Log.d(TAG, "Audio started for alarm: ${alarm.name}")

        // Launch trigger activity
        startAlarmTriggerActivity(alarmId!!)

        // Create wake confirmation for tracking
        val streakRepository = StreakRepository(this)
        streakRepository.createConfirmation(alarmId!!, startTime, alarm.name)

        // Schedule wake confirmation notification (30 minutes later)
        scheduleWakeConfirmationNotification(alarmId!!)

        // Return START_NOT_STICKY so service doesn't restart if killed
        // WHY: Alarm should only play once per trigger
        return START_NOT_STICKY
    }

    /**
     * Launch AlarmTriggerActivity over lock screen.
     * WHY: User needs UI to interact with alarm and complete wake tasks.
     */
    private fun startAlarmTriggerActivity(alarmId: String) {
        val intent = Intent(this, com.reliablealarm.app.ui.AlarmTriggerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }

        try {
            startActivity(intent)
            Log.d(TAG, "Launched AlarmTriggerActivity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch AlarmTriggerActivity", e)
        }
    }

    /**
     * Schedule notification 30 minutes later asking if user woke up.
     * WHY: Track wake-up success for streak system.
     */
    private fun scheduleWakeConfirmationNotification(alarmId: String) {
        val workRequest = OneTimeWorkRequestBuilder<WakeConfirmationWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(workDataOf("alarm_id" to alarmId))
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "wake_confirm_$alarmId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        Log.d(TAG, "Scheduled wake confirmation notification for 30 minutes")
    }

    /**
     * Stop alarm and clean up.
     * Called when alarm is dismissed from AlarmTriggerActivity.
     */
    fun stopAlarm(quickDismissal: Boolean = false) {
        Log.d(TAG, "Stopping alarm service")

        // Check for quick dismissal (within 10 seconds)
        // WHY: Prevent accidental dismissal while asleep
        if (quickDismissal && reliabilityConfig.autoReRing) {
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 10000) {
                Log.w(TAG, "Quick dismissal detected (${elapsedTime}ms), scheduling re-ring")
                scheduleReRing()
                return
            }
        }

        // Stop audio
        audioController.stopAlarm()

        // Release wake lock
        if (this::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
            Log.d(TAG, "Wake lock released")
        }

        // Stop service
        stopForeground(true)
        stopSelf()
    }

    /**
     * Schedule re-ring after 60 seconds for quick dismissal.
     * WHY: User may have accidentally dismissed while asleep.
     */
    private fun scheduleReRing() {
        Log.d(TAG, "Scheduling re-ring in 60 seconds")

        serviceScope.launch {
            delay(60000) // 60 seconds

            if (alarmId != null) {
                // Re-trigger alarm
                val intent = Intent(this@AlarmService, AlarmReceiver::class.java).apply {
                    action = AlarmScheduler.ACTION_ALARM_TRIGGER
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                sendBroadcast(intent)
                Log.d(TAG, "Re-ring triggered")
            }
        }
    }

    /**
     * Handle low memory situations.
     * WHY: System may try to kill service under memory pressure.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "onTrimMemory called with level: $level")

        if (reliabilityConfig.lowMemorySurvival) {
            when (level) {
                TRIM_MEMORY_RUNNING_CRITICAL,
                TRIM_MEMORY_COMPLETE -> {
                    Log.w(TAG, "Critical memory situation, ensuring service survival")
                    // Service is already foreground, which is best protection
                    // Could implement additional measures here if needed
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlarmService onDestroy")

        // Clean up
        audioController.stopAlarm()

        if (this::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }

        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID_FOREGROUND = 1001
    }
}