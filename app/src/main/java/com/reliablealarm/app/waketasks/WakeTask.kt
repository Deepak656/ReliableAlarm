package com.reliablealarm.app.waketasks

import android.content.Context
import android.view.ViewGroup
import com.reliablealarm.app.domain.models.Alarm

/**
 * Interface for wake-up enforcement tasks.
 *
 * WHY: Plugin architecture allows flexible task combinations.
 * Each task is independent, testable, and can be enabled/disabled.
 *
 * Design pattern: Strategy pattern
 * - Each task implements same interface
 * - AlarmTriggerActivity doesn't know task details
 * - Tasks can be added/removed without changing core logic
 */
interface WakeTask {

    /**
     * Get task display name.
     * WHY: UI shows task name to user.
     */
    fun getName(): String

    /**
     * Get task instructions.
     * WHY: User needs to know what to do.
     */
    fun getInstructions(): String

    /**
     * Initialize task.
     * Called when task becomes active.
     *
     * @param context Application context
     * @param onComplete Callback when task completes successfully
     */
    fun initialize(context: Context, alarm: Alarm, onComplete: () -> Unit)

    /**
     * Create UI view for task.
     * WHY: Each task has custom UI.
     *
     * @param container Parent view to attach to
     * @return View for this task
     */
    fun createView(container: ViewGroup): android.view.View

    /**
     * Start task.
     * Called when task should begin accepting user input.
     */
    fun start()

    /**
     * Pause task.
     * Called when activity goes to background.
     * WHY: Save state, stop sensors to save battery.
     */
    fun pause()

    /**
     * Resume task.
     * Called when activity comes to foreground.
     */
    fun resume()

    /**
     * Clean up task resources.
     * Called when task is done or activity destroyed.
     * WHY: Release sensors, stop timers, prevent leaks.
     */
    fun cleanup()

    /**
     * Check if task is completed.
     * WHY: AlarmTriggerActivity needs to know when to enable dismiss.
     */
    fun isCompleted(): Boolean

    /**
     * Get current progress (0.0 to 1.0).
     * WHY: UI shows progress bar.
     */
    fun getProgress(): Float
}