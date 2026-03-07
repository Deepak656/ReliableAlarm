package com.reliablealarm.app.config

import android.content.Context
import android.content.SharedPreferences

class WakeTaskConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wake_task_config", Context.MODE_PRIVATE)

    // ==================== MATH ====================

    var mathTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_MATH_ENABLED, true)
        set(v) = prefs.edit().putBoolean(KEY_MATH_ENABLED, v).apply()

    var mathProblemCount: Int
        get() = prefs.getInt(KEY_MATH_COUNT, 3)
        set(v) = prefs.edit().putInt(KEY_MATH_COUNT, v.coerceIn(1, 10)).apply()

    var mathDifficulty: String
        get() = prefs.getString(KEY_MATH_DIFFICULTY, "MEDIUM") ?: "MEDIUM"
        set(v) = prefs.edit().putString(KEY_MATH_DIFFICULTY, v).apply()

    // ==================== STEP ====================

    var stepTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_STEP_ENABLED, true)
        set(v) = prefs.edit().putBoolean(KEY_STEP_ENABLED, v).apply()

    var stepCount: Int
        get() = prefs.getInt(KEY_STEP_COUNT, 20)
        set(v) = prefs.edit().putInt(KEY_STEP_COUNT, v.coerceIn(5, 100)).apply()

    // ==================== SHAKE ====================

    var shakeTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_ENABLED, true)
        set(v) = prefs.edit().putBoolean(KEY_SHAKE_ENABLED, v).apply()

    var shakeDuration: Int
        get() = prefs.getInt(KEY_SHAKE_DURATION, 10)
        set(v) = prefs.edit().putInt(KEY_SHAKE_DURATION, v.coerceIn(5, 60)).apply()

    var shakeIntensity: Int
        get() = prefs.getInt(KEY_SHAKE_INTENSITY, 5)
        set(v) = prefs.edit().putInt(KEY_SHAKE_INTENSITY, v.coerceIn(1, 10)).apply()

    // ==================== QR / BARCODE ====================

    var qrTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_QR_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_QR_ENABLED, v).apply()

    var qrCount: Int
        get() = prefs.getInt(KEY_QR_COUNT, 1)
        set(v) = prefs.edit().putInt(KEY_QR_COUNT, v.coerceIn(1, 5)).apply()

    // ==================== TYPING ====================

    var typingTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_TYPING_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_TYPING_ENABLED, v).apply()

    var typingParagraphs: Int
        get() = prefs.getInt(KEY_TYPING_COUNT, 1)
        set(v) = prefs.edit().putInt(KEY_TYPING_COUNT, v.coerceIn(1, 5)).apply()

    // ==================== TAP ====================

    var tapTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_TAP_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_TAP_ENABLED, v).apply()

    var tapCount: Int
        get() = prefs.getInt(KEY_TAP_COUNT, 50)
        set(v) = prefs.edit().putInt(KEY_TAP_COUNT, v.coerceIn(5, 100)).apply()

    // ==================== COLOR BALLS ====================

    var colorBallsTaskEnabled: Boolean
        get() = prefs.getBoolean(KEY_COLOR_BALLS_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_COLOR_BALLS_ENABLED, v).apply()

    var colorBallRounds: Int
        get() = prefs.getInt(KEY_COLOR_BALLS_ROUNDS, 3)
        set(v) = prefs.edit().putInt(KEY_COLOR_BALLS_ROUNDS, v.coerceIn(1, 10)).apply()

    // ==================== HELPERS ====================

    fun getEnabledTasks(): List<String> {
        val enabled = mutableListOf<String>()

        if (mathTaskEnabled) enabled.add("MATH")
        if (stepTaskEnabled) enabled.add("STEP")
        if (shakeTaskEnabled) enabled.add("SHAKE")
        if (qrTaskEnabled) enabled.add("QR")
        if (typingTaskEnabled) enabled.add("TYPING")
        if (tapTaskEnabled) enabled.add("TAP")
        if (colorBallsTaskEnabled) enabled.add("COLOR_BALLS")

        return enabled
    }

    fun hasAnyTaskEnabled(): Boolean {
        return getEnabledTasks().isNotEmpty()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    fun getSummary(): String {

        val tasks = mutableListOf<String>()

        if (mathTaskEnabled) tasks.add("$mathProblemCount Math")
        if (stepTaskEnabled) tasks.add("$stepCount Steps")
        if (shakeTaskEnabled) tasks.add("${shakeDuration}s Shake")
        if (qrTaskEnabled) tasks.add("$qrCount QR")
        if (typingTaskEnabled) tasks.add("$typingParagraphs Typing")
        if (tapTaskEnabled) tasks.add("$tapCount Taps")
        if (colorBallsTaskEnabled) tasks.add("$colorBallRounds Balls")

        return if (tasks.isEmpty()) "No tasks enabled"
        else tasks.joinToString(", ")
    }

    companion object {

        private const val KEY_MATH_ENABLED = "math_enabled"
        private const val KEY_MATH_COUNT = "math_count"
        private const val KEY_MATH_DIFFICULTY = "math_difficulty"

        private const val KEY_STEP_ENABLED = "step_enabled"
        private const val KEY_STEP_COUNT = "step_count"

        private const val KEY_SHAKE_ENABLED = "shake_enabled"
        private const val KEY_SHAKE_DURATION = "shake_duration"
        private const val KEY_SHAKE_INTENSITY = "shake_intensity"

        private const val KEY_QR_ENABLED = "qr_enabled"
        private const val KEY_QR_COUNT = "qr_count"

        private const val KEY_TYPING_ENABLED = "typing_enabled"
        private const val KEY_TYPING_COUNT = "typing_count"

        private const val KEY_TAP_ENABLED = "tap_enabled"
        private const val KEY_TAP_COUNT = "tap_count"

        private const val KEY_COLOR_BALLS_ENABLED = "color_balls_enabled"
        private const val KEY_COLOR_BALLS_ROUNDS = "color_balls_rounds"
    }
}