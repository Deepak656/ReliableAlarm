package com.reliablealarm.app.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.AlarmScheduler
import com.reliablealarm.app.domain.WakeTaskManager
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.reliability.AlarmService
import com.reliablealarm.app.system.AudioController
import com.reliablealarm.app.waketasks.WakeTask

/**
 * Full-screen activity shown when alarm triggers.
 *
 * WHY: This is the user-facing alarm experience.
 * Must work over lock screen and handle wake tasks.
 *
 * Critical features:
 * - Shows over lock screen (showWhenLocked, turnScreenOn)
 * - Prevents dismissal until tasks complete
 * - Manages wake task lifecycle
 * - Reduces volume after task completion
 * - Prevents back button dismissal
 * - Handles configuration changes (rotation)
 * - Detects quick dismissal for auto re-ring
 *
 * Lifecycle:
 * 1. Started by AlarmService
 * 2. Creates and displays wake tasks
 * 3. Waits for all tasks to complete
 * 4. Enables dismiss button
 * 5. User dismisses
 * 6. Stops AlarmService
 * 7. Finishes activity
 */
class AlarmTriggerActivity : AppCompatActivity() {

    private lateinit var alarmNameText: TextView
    private lateinit var alarmTimeText: TextView
    private lateinit var taskProgressText: TextView
    private lateinit var taskContainer: LinearLayout
    private lateinit var dismissButton: Button

    private lateinit var alarm: Alarm
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var wakeTaskManager: WakeTaskManager

    private val activeTasks = mutableListOf<WakeTask>()
    private val completedTasks = mutableSetOf<Int>()

    private var startTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window to show over lock screen
        setupWindowFlags()

        setContentView(R.layout.activity_alarm_trigger)

        startTime = System.currentTimeMillis()

        // Initialize views
        alarmNameText = findViewById(R.id.alarmName)
        alarmTimeText = findViewById(R.id.alarmTime)
        taskProgressText = findViewById(R.id.taskProgress)
        taskContainer = findViewById(R.id.taskContainer)
        dismissButton = findViewById(R.id.dismissButton)

        // Initialize repository
        alarmRepository = AlarmRepository(this)

        // Get alarm ID from intent
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        if (alarmId == null) {
            android.util.Log.e(TAG, "No alarm ID provided")
            finish()
            return
        }

        // Load alarm details
        val loadedAlarm = alarmRepository.getAlarm(alarmId)
        if (loadedAlarm == null) {
            android.util.Log.e(TAG, "Alarm not found: $alarmId")
            finish()
            return
        }

        alarm = loadedAlarm

        // Display alarm info
        alarmNameText.text = alarm.name
        alarmTimeText.text = alarm.message

        // Initialize wake task manager with alarm
        wakeTaskManager = WakeTaskManager(this, alarm)
        setupWakeTasks()

        // Dismiss button initially disabled if tasks exist
        dismissButton.isEnabled = activeTasks.isEmpty()
        dismissButton.setOnClickListener {
            dismissAlarm()
        }

        updateTaskProgress()

        // Handle back button
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (dismissButton.isEnabled) {
                        dismissAlarm()
                    } else {
                        android.widget.Toast.makeText(
                            this@AlarmTriggerActivity,
                            "Complete wake tasks to dismiss alarm",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    /**
     * Configure window to show over lock screen and turn screen on.
     *
     * WHY: Alarm must wake device and show UI immediately.
     * User should not need to unlock phone to interact.
     */
    private fun setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Keep screen on while alarm is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Setup and initialize wake tasks.
     *
     * WHY: Tasks must be created, initialized, and displayed.
     * Each task gets its own view in the task container.
     *
     * NOW USES: WakeTaskManager to get enabled tasks (factory pattern)
     */
    private fun setupWakeTasks() {
        // Check if any tasks are enabled
        if (!wakeTaskManager.hasAnyTaskEnabled()) {
            taskProgressText.text = "No wake tasks configured"
            android.util.Log.d(TAG, "No wake tasks enabled for this alarm")
            return
        }

        // Get enabled tasks from manager
        val tasks = wakeTaskManager.getEnabledTasks()

        if (tasks.isEmpty()) {
            taskProgressText.text = "No wake tasks available"
            android.util.Log.w(TAG, "Wake tasks enabled but none could be created")
            return
        }

        android.util.Log.d(TAG, "Setting up ${tasks.size} wake task(s)")
        taskProgressText.text = "Complete ${tasks.size} task(s) to reduce alarm volume"

        // Initialize and display each task
        for ((index, task) in tasks.withIndex()) {
            // Initialize task with completion callback
            task.initialize(this, alarm) {
                onTaskCompleted(index)
            }

            // Create and add task view
            val view = task.createView(taskContainer)
            taskContainer.addView(view)

            // Add divider between tasks (except after last one)
            if (index < tasks.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    ).apply {
                        setMargins(0, 16, 0, 16)
                    }
                    setBackgroundColor(0xFFCCCCCC.toInt())
                }
                taskContainer.addView(divider)
            }

            activeTasks.add(task)
        }

        // Start all tasks
        activeTasks.forEach { task ->
            task.start()
            android.util.Log.d(TAG, "Started task: ${task.getName()}")
        }
    }

    /**
     * Handle task completion.
     *
     * WHY: Track progress and enable dismissal when all done.
     * Reduce volume as reward for completing tasks.
     *
     * @param taskIndex Index of completed task
     */
    private fun onTaskCompleted(taskIndex: Int) {
        if (completedTasks.contains(taskIndex)) {
            // Already completed, avoid duplicate processing
            return
        }

        completedTasks.add(taskIndex)
        android.util.Log.d(TAG, "Task $taskIndex completed (${completedTasks.size}/${activeTasks.size})")

        updateTaskProgress()

        // Reduce volume after first task completion
        if (completedTasks.size == 1) {
            try {
                val audioController = AudioController(
                    this,
                    com.reliablealarm.app.config.ReliabilityConfig(this)
                )
                audioController.reduceVolumeAfterTask()
                android.util.Log.d(TAG, "Volume reduced after task completion")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to reduce volume", e)
            }
        }

        // Enable dismiss button when all tasks complete
        if (completedTasks.size >= activeTasks.size) {
            dismissButton.isEnabled = true
            dismissButton.text = "Dismiss Alarm"
            taskProgressText.text = "All tasks completed! ✓"
            taskProgressText.setTextColor(0xFF4CAF50.toInt())
            android.util.Log.d(TAG, "All tasks completed!")
        }
    }

    /**
     * Update task progress display.
     */
    private fun updateTaskProgress() {
        if (activeTasks.isEmpty()) return

        taskProgressText.text = "Progress: ${completedTasks.size}/${activeTasks.size} tasks completed"
    }

    /**
     * Dismiss alarm and stop service.
     *
     * WHY: User has completed tasks (or no tasks configured).
     * Stop alarm audio and close activity.
     *
     * Detects quick dismissal for auto re-ring protection.
     */
    private fun dismissAlarm() {
        val elapsedTime = System.currentTimeMillis() - startTime
        val quickDismissal = elapsedTime < 10000 // Less than 10 seconds

        android.util.Log.d(TAG, "Alarm dismissed after ${elapsedTime}ms (quick: $quickDismissal)")

        // Stop alarm service
        stopAlarmService(quickDismissal)

        finish()
    }

    /**
     * Stop AlarmService.
     *
     * @param quickDismissal Whether alarm was dismissed quickly (for auto re-ring)
     */
    private fun stopAlarmService(quickDismissal: Boolean) {
        // Stop the service - it will handle cleanup
        stopService(android.content.Intent(this, AlarmService::class.java))
    }

    /**
     * Handle activity pause.
     * Pause sensors to save battery.
     */
    override fun onPause() {
        super.onPause()
        activeTasks.forEach { it.pause() }
        android.util.Log.d(TAG, "Tasks paused")
    }

    /**
     * Handle activity resume.
     * Resume sensors.
     */
    override fun onResume() {
        super.onResume()
        activeTasks.forEach { it.resume() }
        android.util.Log.d(TAG, "Tasks resumed")
    }

    /**
     * Clean up tasks on destroy.
     *
     * WHY: Release sensors and prevent memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d(TAG, "Cleaning up ${activeTasks.size} tasks")
        activeTasks.forEach { it.cleanup() }
        activeTasks.clear()
        completedTasks.clear()
    }

    companion object {
        private const val TAG = "AlarmTriggerActivity"
    }
}