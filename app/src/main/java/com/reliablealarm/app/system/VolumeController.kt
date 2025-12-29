package com.reliablealarm.app.system

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Controls system volume and Do Not Disturb (DND) settings.
 *
 * WHY: Alarm must be audible even if phone is in silent/DND mode.
 * This controller temporarily overrides user's audio settings.
 *
 * Responsibilities:
 * - Detect current audio mode (normal, silent, vibrate, DND)
 * - Override silent/DND for alarm
 * - Restore original mode after alarm
 *
 * Android DND complexity:
 * - Pre-Android 5: RINGER_MODE controls all
 * - Android 5-6: Introduced interruption filter
 * - Android 7+: DND is separate from ringer mode
 * - Android 9+: Requires notification policy access permission
 */
class VolumeController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var originalRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
    private var originalInterruptionFilter: Int = 0

    /**
     * Override DND and Silent mode to ensure alarm is audible.
     *
     * WHY: User may have enabled DND before sleeping.
     * Alarm MUST still fire audibly.
     *
     * Steps:
     * 1. Save original ringer mode
     * 2. Save original DND filter (Android 6+)
     * 3. Set ringer to NORMAL
     * 4. Disable DND temporarily (Android 6+)
     * 5. Set alarm stream volume to reasonable level
     */
    fun overrideDndAndSilent() {
        Log.d(TAG, "Overriding DND and Silent mode")

        try {
            // Save original ringer mode
            originalRingerMode = audioManager.ringerMode
            Log.d(TAG, "Original ringer mode: ${getRingerModeName(originalRingerMode)}")

            // Handle DND (Android 6+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager

                // Check if app has DND access
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    originalInterruptionFilter = notificationManager.currentInterruptionFilter
                    Log.d(TAG, "Original DND filter: ${getFilterName(originalInterruptionFilter)}")

                    // Disable DND for alarm
                    if (originalInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                        notificationManager.setInterruptionFilter(
                            android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                        )
                        Log.d(TAG, "DND overridden for alarm")
                    }
                } else {
                    Log.w(TAG, "No DND access permission, cannot override DND")
                    // App will still try to play alarm at current volume
                }
            }

            // Override silent/vibrate mode
            if (originalRingerMode != AudioManager.RINGER_MODE_NORMAL) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Log.d(TAG, "Ringer mode set to NORMAL")
            }

            // Ensure alarm stream is not muted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (audioManager.isStreamMute(AudioManager.STREAM_ALARM)) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_ALARM,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    Log.d(TAG, "Unmuted alarm stream")
                }
            }

            // Set minimum alarm volume if current is too low
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val minAcceptableVolume = (maxVolume * 0.3f).toInt()

            if (currentVolume < minAcceptableVolume) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    minAcceptableVolume,
                    0
                )
                Log.d(TAG, "Increased alarm volume to minimum: $minAcceptableVolume")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error overriding audio settings", e)
        }
    }

    /**
     * Restore original audio mode after alarm dismissed.
     *
     * WHY: Respect user's original settings after alarm is done.
     */
    fun restoreAudioMode() {
        Log.d(TAG, "Restoring original audio mode")

        try {
            // Restore ringer mode
            if (audioManager.ringerMode != originalRingerMode) {
                audioManager.ringerMode = originalRingerMode
                Log.d(TAG, "Restored ringer mode to: ${getRingerModeName(originalRingerMode)}")
            }

            // Restore DND (Android 6+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && originalInterruptionFilter != 0) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as android.app.NotificationManager

                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(originalInterruptionFilter)
                    Log.d(TAG, "Restored DND filter to: ${getFilterName(originalInterruptionFilter)}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error restoring audio settings", e)
        }
    }

    /**
     * Check if device is in DND mode.
     * WHY: UI can show warning if DND is enabled.
     */
    fun isDndEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager

            val filter = notificationManager.currentInterruptionFilter
            return filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
        }
        return false
    }

    /**
     * Check if device is in silent mode.
     */
    fun isSilentMode(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    /**
     * Get user-friendly ringer mode name.
     */
    private fun getRingerModeName(mode: Int): String {
        return when (mode) {
            AudioManager.RINGER_MODE_NORMAL -> "Normal"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Unknown"
        }
    }

    /**
     * Get user-friendly DND filter name.
     */
    private fun getFilterName(filter: Int): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return when (filter) {
                android.app.NotificationManager.INTERRUPTION_FILTER_ALL -> "All"
                android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority only"
                android.app.NotificationManager.INTERRUPTION_FILTER_NONE -> "Total silence"
                android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms only"
                else -> "Unknown"
            }
        }
        return "N/A"
    }

    companion object {
        private const val TAG = "VolumeController"
    }
}