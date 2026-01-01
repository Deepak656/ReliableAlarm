package com.reliablealarm.app.domain

import android.content.Context
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.waketasks.*

/**
 * Manager for creating and coordinating wake tasks.
 *
 * WHY: Centralized task management ensures consistency.
 * Acts as factory for creating enabled tasks from alarm configuration.
 *
 * REDESIGNED: Now works with Alarm's wakeTasks and taskSettings
 * instead of global WakeTaskConfig.
 *
 * Responsibilities:
 * - Read task list from Alarm model
 * - Instantiate tasks with their configurations
 * - Provide tasks to AlarmTriggerActivity
 * - Track task completion state
 */
class WakeTaskManager(
    private val context: Context,
    private val alarm: Alarm
) {

    /**
     * Check if any task is enabled for this alarm.
     *
     * WHY: If no tasks enabled, alarm can be dismissed immediately.
     * AlarmTriggerActivity uses this to determine behavior.
     */
    fun hasAnyTaskEnabled(): Boolean {
        return alarm.wakeTasks.isNotEmpty()
    }

    /**
     * Get list of enabled tasks for this alarm.
     *
     * WHY: AlarmTriggerActivity needs to instantiate and run these tasks.
     *
     * @return List of WakeTask instances, initialized and ready to use
     */
    fun getEnabledTasks(): List<WakeTask> {
        return alarm.wakeTasks.mapNotNull { taskKey ->
            createTask(taskKey)
        }
    }

    /**
     * Create a single task instance based on task key.
     *
     * WHY: Factory pattern for task creation.
     * Each task type has different implementation.
     *
     * @param taskKey Task identifier (e.g., "task_math", "task_steps")
     * @return WakeTask instance or null if task type not recognized
     */
    private fun createTask(taskKey: String): WakeTask? {
        return when (taskKey) {
            "task_math" -> MathWakeTask()
            "task_steps" -> StepWakeTask()
            "task_shake" -> ShakeWakeTask()
            // Add other task types as they're implemented
             "task_scanqr" -> QrWakeTask()
             "task_typing" -> TypingWakeTask()
             "task_tap" -> TapTask()
             "task_color_balls" -> ColorBallsTask()
            "task_pop_balloons" -> PopBalloonsTask()
            "task_target_tap" -> TargetTapTask()

            else -> {
                android.util.Log.w(TAG, "Unknown task type: $taskKey")
                null
            }
        }
    }

    /**
     * Get summary of enabled tasks for display.
     *
     * WHY: UI can show what tasks will be required before alarm triggers.
     * Helps user understand what they configured.
     *
     * @return Human-readable summary like "Math (3 problems), Walk (20 steps)"
     */
    fun getTaskSummary(): String {
        if (!hasAnyTaskEnabled()) {
            return "No tasks"
        }

        return alarm.wakeTasks.mapNotNull { taskKey ->
            val type = WakeTaskType.fromKey(taskKey)
            val config = alarm.taskSettings[taskKey]

            when (config) {
                is TaskConfig.MathConfig ->
                    "${type?.displayName} (${config.problemCount} problems)"

                is TaskConfig.StepConfig ->
                    "${type?.displayName} (${config.stepsRequired} steps)"

                is TaskConfig.ShakeConfig ->
                    "${type?.displayName} (${config.durationSeconds}s)"

                is TaskConfig.QrConfig ->
                    type?.displayName

                is TaskConfig.TypingConfig ->
                    "${type?.displayName} (${config.paragraphLength} paragraphs)"

                is TaskConfig.TapTaskConfig ->
                    "${type?.displayName} (${config.tapsRequired} taps)"

                is TaskConfig.ColorBallsConfig ->
                    "${type?.displayName} (${config.numberOfround} balls)"

                else -> type?.displayName
            }
        }.joinToString(", ")
    }

    /**
     * Get total number of enabled tasks.
     *
     * WHY: UI might need to show "Complete 3 tasks to dismiss" etc.
     */
    fun getEnabledTaskCount(): Int {
        return alarm.wakeTasks.size
    }

    /**
     * Get display name for a specific task.
     *
     * WHY: UI needs to show current task name.
     */
    fun getTaskName(taskKey: String): String {
        return WakeTaskType.fromKey(taskKey)?.displayName ?: "Task"
    }

    companion object {
        private const val TAG = "WakeTaskManager"
    }
}