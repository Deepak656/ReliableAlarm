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
import com.reliablealarm.app.config.WakeTaskConfig
import kotlin.math.sqrt

/**
 * Step counting wake task using accelerometer.
 *
 * WHY: Physical movement ensures user gets out of bed.
 * Forces user to walk around to dismiss alarm.
 *
 * Design:
 * - Uses accelerometer to detect steps
 * - Simple peak detection algorithm
 * - No step counter sensor dependency (more compatible)
 * - Counts steps from acceleration changes
 *
 * Algorithm:
 * - Measures total acceleration magnitude
 * - Detects peaks above threshold
 * - Requires minimum time between steps (debouncing)
 * - Tolerates some false positives (better than false negatives)
 */
class StepWakeTask : WakeTask, SensorEventListener {

    private var requiredSteps: Int = 20
    private var currentSteps: Int = 0
    private var completed: Boolean = false
    private var onComplete: (() -> Unit)? = null

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var progressBar: ProgressBar
    private lateinit var stepsText: TextView
    private lateinit var instructionText: TextView

    // Step detection parameters
    private var lastStepTime: Long = 0
    private var lastAcceleration: Float = 0f
    private val stepThreshold: Float = 2.5f // Acceleration threshold for step
    private val minStepInterval: Long = 300 // Minimum 300ms between steps

    override fun getName(): String = "Walking Challenge"

    override fun getInstructions(): String =
        "Take $requiredSteps steps to reduce alarm volume"

    override fun initialize(context: Context, onComplete: () -> Unit) {
        this.onComplete = onComplete

        val config = WakeTaskConfig(context)
        this.requiredSteps = config.stepCount

        currentSteps = 0
        completed = false
        lastStepTime = 0

        // Initialize sensor manager
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            android.util.Log.e(TAG, "No accelerometer sensor available")
            // Fallback: consider task completed if no sensor
            completed = true
            onComplete()
        }
    }

    override fun createView(container: ViewGroup): View {
        val inflater = LayoutInflater.from(container.context)
        val view = inflater.inflate(R.layout.task_step, container, false)

        progressBar = view.findViewById(R.id.stepProgress)
        stepsText = view.findViewById(R.id.stepCount)
        instructionText = view.findViewById(R.id.stepInstructions)

        progressBar.max = requiredSteps
        progressBar.progress = 0

        updateUI()

        return view
    }

    override fun start() {
        // Register accelerometer listener
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME // ~20ms updates, good balance
            )
            android.util.Log.d(TAG, "Accelerometer listener registered")
        }
    }

    override fun pause() {
        // Unregister sensor to save battery
        sensorManager.unregisterListener(this)
        android.util.Log.d(TAG, "Accelerometer listener paused")
    }

    override fun resume() {
        // Re-register sensor
        if (!completed) {
            start()
        }
    }

    override fun cleanup() {
        sensorManager.unregisterListener(this)
        onComplete = null
    }

    override fun isCompleted(): Boolean = completed

    override fun getProgress(): Float = currentSteps.toFloat() / requiredSteps.toFloat()

    /**
     * Handle accelerometer data.
     * Detects steps using peak detection on acceleration magnitude.
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || completed) return

        // Calculate acceleration magnitude (ignore gravity direction)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z)

        // Detect step as peak in acceleration
        // WHY: Walking creates rhythmic acceleration changes
        val now = System.currentTimeMillis()

        // Check for acceleration peak above threshold
        if (acceleration > SensorManager.GRAVITY_EARTH + stepThreshold) {
            // Debounce: require minimum time between steps
            if (now - lastStepTime > minStepInterval) {
                // Check that this is a peak (acceleration was lower recently)
                if (lastAcceleration < acceleration - 1.0f) {
                    registerStep()
                    lastStepTime = now
                }
            }
        }

        lastAcceleration = acceleration
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not needed for accelerometer
    }

    /**
     * Register a detected step.
     * Updates UI and checks for completion.
     */
    private fun registerStep() {
        currentSteps++
        android.util.Log.d(TAG, "Step detected: $currentSteps/$requiredSteps")

        updateUI()

        if (currentSteps >= requiredSteps) {
            completed = true
            sensorManager.unregisterListener(this)
            android.util.Log.d(TAG, "Step task completed!")
            onComplete?.invoke()
        }
    }

    /**
     * Update UI with current progress.
     */
    private fun updateUI() {
        progressBar.progress = currentSteps
        stepsText.text = "$currentSteps / $requiredSteps steps"

        if (completed) {
            instructionText.text = "Walking completed! Alarm volume reduced."
            instructionText.setTextColor(0xFF4CAF50.toInt())
        } else {
            val remaining = requiredSteps - currentSteps
            instructionText.text = "Keep walking! $remaining steps remaining"
        }
    }

    companion object {
        private const val TAG = "StepWakeTask"
    }
}