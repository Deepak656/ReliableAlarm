package com.reliablealarm.app.waketasks

enum class WakeTaskType(val key: String, val displayName: String) {
    MATH("task_math", "Math"),
    STEPS("task_steps", "Walk"),
    SHAKE("task_shake", "Shake"),
    QR("task_scanqr", "Scan QR"),
    TYPING("task_typing", "Typing"),
    TAP("task_tap", "Tap"),
    COLOR_BALLS("task_color_balls", "Color Balls");

    companion object {
        fun fromKey(key: String): WakeTaskType? =
            values().firstOrNull { it.key == key }
    }
}
