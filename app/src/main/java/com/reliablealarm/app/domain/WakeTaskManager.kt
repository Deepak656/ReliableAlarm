package com.reliablealarm.app.domain

import android.content.Context
import com.reliablealarm.app.config.WakeTaskConfig
import com.reliablealarm.app.waketasks.MathWakeTask
import com.reliablealarm.app.waketasks.ShakeWakeTask
import com.reliablealarm.app.waketasks.StepWakeTask
import com.reliablealarm.app.waketasks.WakeTask

/**
 * Manager for creating and coordinating wake tasks.
 *
 * WHY: Centralized task management ensures consistency.
 * Acts as factory for creating enabled tasks.
 *
 * Responsibilities:
 * - Read task configuration
 * - Instantiate enabled tasks
 * - Provide tasks to AlarmTriggerActivity
 * - Track task completion state
 */
class WakeTaskManager(private val context: Context) {

    private val config = WakeTaskConfig(context)

    /**
     * Create list of enabled wake tasks.
     *
     * WHY: AlarmTriggerActivity needs to know which tasks to show.
     * Tasks are created fresh for each alarm trigger.
     *
     * @return List of enabled WakeTask instances
     */
    fun createEnabledTasks(): List<WakeTask> {
        val tasks = mutableListOf<WakeTask>()

        // Add tasks in order: Math, Step, Shake
        // WHY: Math first (cognitive), then physical tasks

        if (config.mathTaskEnabled) {
            tasks.add(MathWakeTask())
        }

        if (config.stepTaskEnabled) {
            tasks.add(StepWakeTask())
        }

        if (config.shakeTaskEnabled) {
            tasks.add(ShakeWakeTask())
        }

        android.util.Log.d(TAG, "Created ${tasks.size} enabled wake tasks")
        return tasks
    }

    /**
     * Check if any task is enabled.
     *
     * WHY: If no tasks enabled, alarm can be dismissed immediately.
     * AlarmTriggerActivity uses this to determine behavior.
     */
    fun hasAnyTaskEnabled(): Boolean {
        return config.hasAnyTaskEnabled()
    }

    /**
     * Get summary of enabled tasks for display.
     *
     * WHY: UI can show what tasks will be required.
     */
    fun getTaskSummary(): String {
        return config.getSummary()
    }

    /**
     * Get total number of enabled tasks.
     */
    fun getEnabledTaskCount(): Int {
        var count = 0
        if (config.mathTaskEnabled) count++
        if (config.stepTaskEnabled) count++
        if (config.shakeTaskEnabled) count++
        return count
    }

    companion object {
        private const val TAG = "WakeTaskManager"
    }
}