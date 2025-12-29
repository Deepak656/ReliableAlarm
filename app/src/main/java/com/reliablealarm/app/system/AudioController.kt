package com.reliablealarm.app.system

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.reliablealarm.app.config.ReliabilityConfig
import com.reliablealarm.app.domain.models.Alarm
import kotlinx.coroutines.*

/**
 * Controls alarm audio playback with reliability features.
 *
 * WHY: Centralized audio control ensures consistent behavior.
 * Implements:
 * - Bluetooth override (force speaker)
 * - Progressive volume escalation
 * - DND/Silent mode override
 * - Looping alarm sound
 * - Volume restoration after alarm
 *
 * Design:
 * - Uses MediaPlayer for looping playback
 * - AudioManager for volume control
 * - Coroutines for progressive escalation
 * - Saves original volume to restore later
 */
class AudioController(
    private val context: Context,
    private val reliabilityConfig: ReliabilityConfig
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val volumeController = VolumeController(context)

    private var mediaPlayer: MediaPlayer? = null
    private var originalVolume: Int = 0
    private var currentVolume: Int = 0
    private var escalationJob: Job? = null

    private val audioScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Start alarm playback with all reliability features.
     *
     * WHY: This is the critical moment - alarm MUST be audible.
     *
     * Steps:
     * 1. Save original volume
     * 2. Override DND/Silent mode
     * 3. Force speaker output (override Bluetooth)
     * 4. Set initial volume (70%)
     * 5. Start media playback (looping)
     * 6. Begin progressive volume escalation
     *
     * @param alarm Alarm that's triggering
     */
    fun startAlarm(alarm: Alarm) {
        Log.d(TAG, "Starting alarm audio for: ${alarm.name}")

        try {
            // Save original volume for restoration
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            Log.d(TAG, "Original volume: $originalVolume")

            // Override DND and silent mode
            volumeController.overrideDndAndSilent()

            // Force speaker output (override Bluetooth)
            if (reliabilityConfig.bluetoothOverride) {
                forceSpeakerOutput()
            }

            // Set initial volume to 70%
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            currentVolume = (maxVolume * 0.7f).toInt()
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                currentVolume,
                0 // No UI flags
            )
            Log.d(TAG, "Set initial volume to: $currentVolume (70% of $maxVolume)")

            // Start media player
            startMediaPlayer()

            // Begin progressive volume escalation
            if (reliabilityConfig.volumeEscalation) {
                startVolumeEscalation()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm audio", e)
        }
    }

    /**
     * Force audio output to speaker, overriding Bluetooth.
     *
     * WHY: User may not hear alarm through Bluetooth headphones if not worn.
     * Phone speaker is more reliable for waking up.
     *
     * Note: This is a best-effort override. Some OEMs may not honor it.
     */
    private fun forceSpeakerOutput() {
        try {
            // Set speaker on for alarm
            // WHY: Routes audio to device speaker instead of Bluetooth/headphones
            if (!audioManager.isSpeakerphoneOn) {
                audioManager.isSpeakerphoneOn = true
                Log.d(TAG, "Forced speaker output (override Bluetooth)")
            }

            // Set mode to NORMAL to ensure speaker routing
            audioManager.mode = AudioManager.MODE_NORMAL

        } catch (e: Exception) {
            Log.e(TAG, "Failed to force speaker output", e)
        }
    }

    /**
     * Start MediaPlayer for alarm sound.
     *
     * WHY: MediaPlayer provides looping, volume control, and reliability.
     * Uses default alarm sound from system.
     *
     * AudioAttributes ensure:
     * - CONTENT_TYPE_SONIFICATION: This is an alarm/notification
     * - USAGE_ALARM: Highest priority audio
     * - FLAG_AUDIBILITY_ENFORCED: System tries harder to make it audible
     */
    private fun startMediaPlayer() {
        try {
            // Release any existing player
            stopMediaPlayer()

            // Get default alarm sound URI
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: Uri.parse("android.resource://${context.packageName}/raw/default_alarm")

            Log.d(TAG, "Alarm URI: $alarmUri")

            // Create MediaPlayer with AudioAttributes
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)

                // Set audio attributes for maximum audibility
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                        .build()
                )

                // Loop continuously until dismissed
                isLooping = true

                // Prepare and start
                prepare()
                start()

                Log.d(TAG, "MediaPlayer started (looping)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaPlayer", e)
            // Fallback: try to use Ringtone API
            playFallbackAlarm()
        }
    }

    /**
     * Fallback alarm playback using Ringtone API.
     * WHY: If MediaPlayer fails, still need to make sound.
     */
    private fun playFallbackAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
            Log.d(TAG, "Fallback ringtone playing")
        } catch (e: Exception) {
            Log.e(TAG, "Fallback alarm also failed", e)
        }
    }

    /**
     * Start progressive volume escalation.
     *
     * WHY: Gentle wake-up with safety net.
     * - Starts at 70% (comfortable)
     * - Increases 10% every 10 seconds
     * - Caps at 100% (maximum audibility)
     *
     * If user doesn't respond within 30 seconds, volume is maximum.
     */
    private fun startVolumeEscalation() {
        escalationJob = audioScope.launch {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVolume = maxVolume

            Log.d(TAG, "Starting volume escalation from $currentVolume to $targetVolume")

            while (currentVolume < targetVolume && isActive) {
                delay(10000) // Wait 10 seconds

                // Increase by 10%
                currentVolume = minOf(
                    currentVolume + (maxVolume * 0.1f).toInt(),
                    targetVolume
                )

                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    currentVolume,
                    0
                )

                Log.d(TAG, "Volume escalated to: $currentVolume")
            }

            Log.d(TAG, "Volume escalation complete at: $currentVolume")
        }
    }

    /**
     * Reduce volume to 50% after wake task completion.
     *
     * WHY: Reward user for completing tasks by lowering volume.
     * Still audible but less annoying.
     */
    fun reduceVolumeAfterTask() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val halfVolume = (maxVolume * 0.5f).toInt()

        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            halfVolume,
            0
        )

        Log.d(TAG, "Volume reduced to 50%: $halfVolume")
    }

    /**
     * Stop alarm and restore original state.
     *
     * WHY: Clean up audio resources and restore user's volume settings.
     */
    fun stopAlarm() {
        Log.d(TAG, "Stopping alarm audio")

        // Cancel volume escalation
        escalationJob?.cancel()
        escalationJob = null

        // Stop media player
        stopMediaPlayer()

        // Restore original volume
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                originalVolume,
                0
            )
            Log.d(TAG, "Restored original volume: $originalVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore volume", e)
        }

        // Turn off speakerphone
        if (audioManager.isSpeakerphoneOn) {
            audioManager.isSpeakerphoneOn = false
            Log.d(TAG, "Disabled speakerphone")
        }

        // Restore DND/Silent mode
        volumeController.restoreAudioMode()
    }

    /**
     * Stop MediaPlayer and release resources.
     */
    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "MediaPlayer stopped and released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        }
    }

    /**
     * Check if alarm is currently playing.
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    companion object {
        private const val TAG = "AudioController"
    }
}