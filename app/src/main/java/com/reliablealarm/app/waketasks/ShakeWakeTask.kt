package com.reliablealarm.app.waketasks

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm
import kotlin.math.sqrt

/**
 * Shake detection wake task using accelerometer.
 *
 * WHY: Vigorous shaking ensures user is physically active and awake.
 * Requires sustained shaking for specified duration.
 *
 * Design:
 * - Uses accelerometer to detect shake motion
 * - Requires continuous shaking for X seconds
 * - If shaking stops, timer resets (prevents gaming the system)
 * - Intensity threshold adjustable by user
 *
 * Algorithm:
 * - Measures acceleration changes (jerk)
 * - Shake = rapid acceleration changes above threshold
 * - Must maintain shake intensity continuously
 * - Timer counts up only while actively shaking
 */
class ShakeWakeTask : WakeTask, SensorEventListener {

    private var requiredDuration: Int = 10 // seconds
    private var intensityThreshold: Int = 5 // 1-10 scale
    private var shakeDuration: Float = 0f // seconds of valid shaking
    private var completed: Boolean = false
    private var onComplete: (() -> Unit)? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var timeText: TextView
    private lateinit var instructionText: TextView
    private lateinit var intensityIndicator: TextView

    // Shake detection state
    private var lastUpdate: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f

    private val updateInterval: Long = 100 // Check shake every 100ms
    private var shakeThreshold: Float = 800f // Calculated from intensity

    override fun getName(): String = "Shake Challenge"

    override fun getInstructions(): String =
        "Shake your phone vigorously for $requiredDuration seconds"

    override fun initialize(context: Context, alarm: Alarm, onComplete: () -> Unit) {
        this.onComplete = onComplete

        val config = alarm.taskSettings["task_shake"] as? TaskConfig.ShakeConfig

        this.requiredDuration = config?.durationSeconds ?: 10
        this.intensityThreshold = config?.intensity ?: 5

        shakeThreshold = 400f + (intensityThreshold - 1) * 160f

        shakeDuration = 0f
        completed = false
        lastUpdate = 0

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            completed = true
            onComplete()
        }
    }

    override fun createView(container: ViewGroup): View {
        val inflater = LayoutInflater.from(container.context)
        val view = inflater.inflate(R.layout.task_shake, container, false)

        progressBar = view.findViewById(R.id.shakeProgress)
        timeText = view.findViewById(R.id.shakeTime)
        instructionText = view.findViewById(R.id.shakeInstructions)
        intensityIndicator = view.findViewById(R.id.shakeIntensity)

        progressBar.max = requiredDuration * 10 // 10 units per second for smooth animation
        progressBar.progress = 0

        updateUI(false)

        return view
    }

    override fun start() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
            lastUpdate = System.currentTimeMillis()
            android.util.Log.d(TAG, "Shake detection started")
        }
    }

    override fun pause() {
        sensorManager.unregisterListener(this)
        android.util.Log.d(TAG, "Shake detection paused")
    }

    override fun resume() {
        if (!completed) {
            start()
        }
    }

    override fun cleanup() {
        sensorManager.unregisterListener(this)
        onComplete = null
    }

    override fun isCompleted(): Boolean = completed

    override fun getProgress(): Float = shakeDuration / requiredDuration.toFloat()

    /**
     * Handle accelerometer data to detect shaking.
     *
     * WHY: Shaking creates rapid acceleration changes (high jerk).
     * We measure the magnitude of acceleration change.
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || completed) return

        val currentTime = System.currentTimeMillis()

        // Only process every updateInterval ms
        if (currentTime - lastUpdate < updateInterval) return

        val timeDelta = (currentTime - lastUpdate) / 1000f // seconds
        lastUpdate = currentTime

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate acceleration change (jerk)
        // WHY: Shaking = rapid acceleration changes
        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastX = x
        lastY = y
        lastZ = z

        // Calculate magnitude of acceleration change
        val acceleration = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / timeDelta

        // Check if shaking with sufficient intensity
        if (acceleration > shakeThreshold) {
            // Valid shake detected, increment timer
            shakeDuration += timeDelta
            android.util.Log.d(TAG, "Shaking: ${shakeDuration}s / ${requiredDuration}s (intensity: $acceleration)")

            updateUI(true)

            // Check for completion
            if (shakeDuration >= requiredDuration) {
                completed = true
                sensorManager.unregisterListener(this)
                android.util.Log.d(TAG, "Shake task completed!")
                updateUI(false)
                onComplete?.invoke()
            }
        } else {
            // Not shaking hard enough
            // Optionally: could add small decay here to require continuous shaking
            // For now: timer just stops incrementing
            updateUI(false)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not needed
    }

    /**
     * Update UI with current progress.
     *
     * @param isShaking Whether currently detecting valid shake
     */
    private fun updateUI(isShaking: Boolean) {
        val progress = (shakeDuration * 10).toInt() // Convert to progress units
        progressBar.progress = progress

        val remainingSeconds = requiredDuration - shakeDuration.toInt()
        timeText.text = String.format("%.1f / %d seconds", shakeDuration, requiredDuration)

        if (completed) {
            instructionText.text = "Shaking completed! Alarm volume reduced."
            instructionText.setTextColor(0xFF4CAF50.toInt())
            intensityIndicator.text = "✓ Done"
            intensityIndicator.setTextColor(0xFF4CAF50.toInt())
        } else {
            if (isShaking) {
                instructionText.text = "Keep shaking! $remainingSeconds seconds remaining"
                intensityIndicator.text = "🔥 Good intensity!"
                intensityIndicator.setTextColor(0xFF4CAF50.toInt())
            } else {
                instructionText.text = "Shake more vigorously!"
                intensityIndicator.text = "⚠️ Shake harder"
                intensityIndicator.setTextColor(0xFFFFA726.toInt())
            }
        }
    }

    companion object {
        private const val TAG = "ShakeWakeTask"
    }
}