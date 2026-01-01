package com.reliablealarm.app.waketasks

enum class WakeTaskType(val key: String, val displayName: String) {
    MATH("task_math", "Math"),
    STEPS("task_steps", "Walk"),
    SHAKE("task_shake", "Shake"),
    QR("task_scanqr", "Scan QR"),
    TYPING("task_typing", "Typing"),
    TAP("task_tap", "Tap"),
    COLOR_BALLS("task_color_balls", "Color Balls"),
    POP_BALLOONS("task_pop_balloons", "Pop Balloons"),
    TARGET_TAP("task_target_tap", "Target Tap Game");

    companion object {
        fun fromKey(key: String): WakeTaskType? =
            values().firstOrNull { it.key == key }
    }
}
