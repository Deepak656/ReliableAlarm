package com.reliablealarm.app.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration for wake-up enforcement tasks.
 * User can enable/disable each task and configure parameters.
 *
 * WHY: Plugin system allows flexible combinations of wake tasks.
 * Users with different needs can customize wake-up difficulty.
 *
 * DEFAULT: All tasks ENABLED with moderate difficulty.
 *
 * Storage: SharedPreferences for instant access.
 */
class WakeTaskConfig(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "wake_task_config",
        Context.MODE_PRIVATE
    )

    // ==================== Math Task ====================

    /**
     * Enable/disable math task.
     * WHY: Forces cognitive engagement to dismiss alarm.
     */
    var mathTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_MATH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MATH_ENABLED, value).apply()

    /**
     * Number of math problems to solve.
     * Range: 1-10
     * WHY: More problems = harder to dismiss while groggy.
     */
    var mathProblemCount: Int
        get() = prefs.getInt(KEY_MATH_COUNT, 3)
        set(value) = prefs.edit().putInt(KEY_MATH_COUNT, value.coerceIn(1, 10)).apply()

    /**
     * Difficulty level: EASY, MEDIUM, HARD
     * WHY: Allows user to scale cognitive challenge.
     */
    var mathDifficulty: String
        get() = prefs.getString(KEY_MATH_DIFFICULTY, "MEDIUM") ?: "MEDIUM"
        set(value) = prefs.edit().putString(KEY_MATH_DIFFICULTY, value).apply()

    // ==================== Step Task ====================

    /**
     * Enable/disable step walking task.
     * WHY: Physical movement ensures user gets out of bed.
     */
    var stepTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_STEP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_STEP_ENABLED, value).apply()

    /**
     * Required step count.
     * Range: 5-100
     * WHY: Enough to get user moving, not excessive.
     */
    var stepCount: Int
        get() = prefs.getInt(KEY_STEP_COUNT, 20)
        set(value) = prefs.edit().putInt(KEY_STEP_COUNT, value.coerceIn(5, 100)).apply()

    // ==================== Shake Task ====================

    /**
     * Enable/disable shake task.
     * WHY: Vigorous shaking ensures user is physically active.
     */
    var shakeTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_ENABLED, value).apply()

    /**
     * Required shake duration in seconds.
     * Range: 5-60
     * WHY: Sustained shaking prevents easy dismissal.
     */
    var shakeDuration: Int
        get() = prefs.getInt(KEY_SHAKE_DURATION, 10)
        set(value) = prefs.edit().putInt(KEY_SHAKE_DURATION, value.coerceIn(5, 60)).apply()

    /**
     * Shake intensity threshold.
     * Range: 1-10 (higher = more vigorous shaking required)
     * WHY: Prevents weak shaking from counting.
     */
    var shakeIntensity: Int
        get() = prefs.getInt(KEY_SHAKE_INTENSITY, 5)
        set(value) = prefs.edit().putInt(KEY_SHAKE_INTENSITY, value.coerceIn(1, 10)).apply()

    // ==================== General ====================

    /**
     * Get list of enabled tasks.
     * WHY: AlarmTriggerActivity needs to know which tasks to show.
     */
    fun getEnabledTasks(): List<String> {
        val enabled = mutableListOf<String>()
        if (mathTaskEnabled) enabled.add("MATH")
        if (stepTaskEnabled) enabled.add("STEP")
        if (shakeTaskEnabled) enabled.add("SHAKE")
        return enabled
    }

    /**
     * Check if any task is enabled.
     * WHY: If no tasks enabled, alarm can be dismissed immediately.
     */
    fun hasAnyTaskEnabled(): Boolean {
        return mathTaskEnabled || stepTaskEnabled || shakeTaskEnabled
    }

    /**
     * Reset all settings to default.
     * WHY: Easy recovery if user breaks configuration.
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /**
     * Get summary string for display.
     * WHY: Shows active tasks in UI.
     */
    fun getSummary(): String {
        val tasks = mutableListOf<String>()
        if (mathTaskEnabled) tasks.add("$mathProblemCount Math Problems")
        if (stepTaskEnabled) tasks.add("$stepCount Steps")
        if (shakeTaskEnabled) tasks.add("${shakeDuration}s Shake")

        return if (tasks.isEmpty()) {
            "No tasks enabled"
        } else {
            tasks.joinToString(", ")
        }
    }

    companion object {
        // Math task keys
        private const val KEY_MATH_ENABLED = "math_enabled"
        private const val KEY_MATH_COUNT = "math_count"
        private const val KEY_MATH_DIFFICULTY = "math_difficulty"

        // Step task keys
        private const val KEY_STEP_ENABLED = "step_enabled"
        private const val KEY_STEP_COUNT = "step_count"

        // Shake task keys
        private const val KEY_SHAKE_ENABLED = "shake_enabled"
        private const val KEY_SHAKE_DURATION = "shake_duration"
        private const val KEY_SHAKE_INTENSITY = "shake_intensity"
    }
}