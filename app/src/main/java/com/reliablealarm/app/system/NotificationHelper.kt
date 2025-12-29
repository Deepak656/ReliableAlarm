package com.reliablealarm.app.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.WakeConfirmation
import com.reliablealarm.app.ui.MainActivity

/**
 * Centralized notification management.
 *
 * WHY: Notifications are used for:
 * - Foreground service (keeps AlarmService alive)
 * - Wake confirmation (30 min after alarm)
 * - Battery optimization warnings
 *
 * Creates notification channels on Android 8+.
 * Handles all notification styling and actions.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        // Create notification channels on Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }
    }

    /**
     * Create notification channels for Android 8+.
     *
     * WHY: Android 8+ requires channels for categorizing notifications.
     * Different channels have different priorities and behaviors.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Foreground service channel (HIGH priority)
            val foregroundChannel = NotificationChannel(
                CHANNEL_FOREGROUND_SERVICE,
                "Alarm Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows when alarm is ringing"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Wake confirmation channel (DEFAULT priority)
            val confirmationChannel = NotificationChannel(
                CHANNEL_WAKE_CONFIRMATION,
                "Wake Confirmation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Asks if you woke up on time"
                setShowBadge(true)
            }

            // Battery warning channel (HIGH priority)
            val batteryChannel = NotificationChannel(
                CHANNEL_BATTERY_WARNING,
                "Battery Optimization",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnings about battery optimization"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(
                foregroundChannel,
                confirmationChannel,
                batteryChannel
            ))
        }
    }

    /**
     * Create foreground notification for AlarmService.
     *
     * WHY: Foreground services must show notification.
     * This notification indicates alarm is currently ringing.
     *
     * @param alarmId ID of alarm that's ringing
     * @return Notification object
     */
    fun createForegroundNotification(alarmId: String): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND_SERVICE)
            .setContentTitle("Alarm Ringing")
            .setContentText("Tap to view alarm")
            .setSmallIcon(R.drawable.ic_alarm)
            .setOngoing(true) // Cannot be dismissed by user
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()
    }

    /**
     * Show wake confirmation notification 30 minutes after alarm.
     *
     * WHY: Track if user actually woke up for streak system.
     *
     * Actions:
     * - YES: User woke up on time
     * - NO: User missed/overslept
     *
     * @param confirmation WakeConfirmation to ask about
     */
    fun showWakeConfirmationNotification(confirmation: WakeConfirmation) {
        // Create YES action
        val yesIntent = Intent(context, WakeConfirmationReceiver::class.java).apply {
            action = ACTION_WAKE_YES
            putExtra(EXTRA_CONFIRMATION_ID, confirmation.id)
        }
        val yesPendingIntent = PendingIntent.getBroadcast(
            context,
            confirmation.id.hashCode(),
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create NO action
        val noIntent = Intent(context, WakeConfirmationReceiver::class.java).apply {
            action = ACTION_WAKE_NO
            putExtra(EXTRA_CONFIRMATION_ID, confirmation.id)
        }
        val noPendingIntent = PendingIntent.getBroadcast(
            context,
            confirmation.id.hashCode() + 1,
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WAKE_CONFIRMATION)
            .setContentTitle("Did you wake up on time?")
            .setContentText("Alarm: ${confirmation.alarmName}")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Yes, on time", yesPendingIntent)
            .addAction(R.drawable.ic_close, "No, missed it", noPendingIntent)
            .build()

        notificationManager.notify(
            NOTIFICATION_ID_WAKE_CONFIRMATION_BASE + confirmation.id.hashCode(),
            notification
        )
    }

    /**
     * Cancel wake confirmation notification.
     * Called after user responds.
     */
    fun cancelWakeConfirmationNotification(confirmationId: String) {
        notificationManager.cancel(
            NOTIFICATION_ID_WAKE_CONFIRMATION_BASE + confirmationId.hashCode()
        )
    }

    /**
     * Show battery optimization warning.
     *
     * WHY: Alert user if battery optimization will prevent alarms.
     *
     * @param message Warning message
     * @param onTap Intent to open when tapped
     */
    fun showBatteryWarning(message: String, onTap: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            onTap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BATTERY_WARNING)
            .setContentTitle("Alarm Reliability Warning")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(NOTIFICATION_ID_BATTERY_WARNING, notification)
    }

    companion object {
        // Channel IDs
        private const val CHANNEL_FOREGROUND_SERVICE = "alarm_foreground"
        private const val CHANNEL_WAKE_CONFIRMATION = "wake_confirmation"
        private const val CHANNEL_BATTERY_WARNING = "battery_warning"

        // Notification IDs
        private const val NOTIFICATION_ID_WAKE_CONFIRMATION_BASE = 2000
        private const val NOTIFICATION_ID_BATTERY_WARNING = 3000

        // Actions
        const val ACTION_WAKE_YES = "com.reliablealarm.app.WAKE_YES"
        const val ACTION_WAKE_NO = "com.reliablealarm.app.WAKE_NO"
        const val EXTRA_CONFIRMATION_ID = "confirmation_id"
    }
}

/**
 * BroadcastReceiver for wake confirmation notification actions.
 * Handles YES/NO button taps.
 */
class WakeConfirmationReceiver : android.content.BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val confirmationId = intent.getStringExtra(NotificationHelper.EXTRA_CONFIRMATION_ID)
        if (confirmationId == null) {
            android.util.Log.e(TAG, "No confirmation ID in intent")
            return
        }

        val wokeOnTime = when (intent.action) {
            NotificationHelper.ACTION_WAKE_YES -> true
            NotificationHelper.ACTION_WAKE_NO -> false
            else -> {
                android.util.Log.e(TAG, "Unknown action: ${intent.action}")
                return
            }
        }

        android.util.Log.d(TAG, "Wake confirmation: $wokeOnTime for $confirmationId")

        // Update confirmation in repository
        val streakRepository = com.reliablealarm.app.domain.StreakRepository(context)
        streakRepository.updateConfirmation(confirmationId, wokeOnTime)

        // Cancel notification
        val notificationHelper = NotificationHelper(context)
        notificationHelper.cancelWakeConfirmationNotification(confirmationId)

        // Show toast feedback
        val message = if (wokeOnTime) "Great! Streak updated ✓" else "Streak reset. Try again tomorrow!"
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "WakeConfirmationReceiver"
    }
}