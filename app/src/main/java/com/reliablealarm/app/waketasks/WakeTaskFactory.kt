package com.reliablealarm.app.waketasks

object WakeTaskFactory {

    fun create(taskKey: String): WakeTask? {
        return when (taskKey) {
            "task_math" -> MathWakeTask()
            "task_steps" -> StepWakeTask()
            "task_shake" -> ShakeWakeTask()
            "task_scanqr" -> QrWakeTask()
            "task_typing" -> TypingWakeTask()
            "task_tap" -> TapTask()
            "task_color_balls" -> ColorBallsTask()
            else -> null
        }
    }
}