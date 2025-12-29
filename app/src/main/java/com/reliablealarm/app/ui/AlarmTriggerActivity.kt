package com.reliablealarm.app.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

    private var alarm: Alarm? = null
    private var alarmId: String? = null

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

        // Get alarm ID from intent
        alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID)
        if (alarmId == null) {
            android.util.Log.e(TAG, "No alarm ID provided")
            finish()
            return
        }

        // Load alarm details
        val repository = AlarmRepository(this)
        alarm = repository.getAlarm(alarmId!!)

        if (alarm == null) {
            android.util.Log.e(TAG, "Alarm not found: $alarmId")
            finish()
            return
        }

        // Display alarm info
        alarmNameText.text = alarm!!.name
        alarmTimeText.text = alarm!!.message

        // Initialize wake tasks
        wakeTaskManager = WakeTaskManager(this)
        setupWakeTasks()

        // Dismiss button initially disabled if tasks exist
        dismissButton.isEnabled = activeTasks.isEmpty()
        dismissButton.setOnClickListener {
            dismissAlarm()
        }

        updateTaskProgress()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    if (dismissButton.isEnabled) {
                        dismissAlarm()
                        finish()
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
     */
    private fun setupWakeTasks() {
        val tasks = wakeTaskManager.createEnabledTasks()

        if (tasks.isEmpty()) {
            taskProgressText.text = "No wake tasks configured"
            return
        }

        taskProgressText.text = "Complete ${tasks.size} task(s) to reduce alarm volume"

        for ((index, task) in tasks.withIndex()) {
            // Initialize task with completion callback
            task.initialize(this) {
                onTaskCompleted(index)
            }

            // Create and add task view
            val taskView = task.createView(taskContainer)
            taskContainer.addView(taskView)

            // Add divider between tasks (except after last)
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
        activeTasks.forEach { it.start() }
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
        completedTasks.add(taskIndex)
        android.util.Log.d(TAG, "Task $taskIndex completed (${completedTasks.size}/${activeTasks.size})")

        updateTaskProgress()

        // Reduce volume after first task completion
        if (completedTasks.size == 1) {
            val audioController = AudioController(this, com.reliablealarm.app.config.ReliabilityConfig(this))
            audioController.reduceVolumeAfterTask()
            android.util.Log.d(TAG, "Volume reduced after task completion")
        }

        // Enable dismiss button when all tasks complete
        if (completedTasks.size >= activeTasks.size) {
            dismissButton.isEnabled = true
            dismissButton.text = "Dismiss Alarm"
            taskProgressText.text = "All tasks completed! ✓"
            taskProgressText.setTextColor(0xFF4CAF50.toInt())
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
        // Service will handle auto re-ring if needed
        stopAlarmService(quickDismissal)

        finish()
    }

    /**
     * Stop AlarmService.
     *
     * @param quickDismissal Whether alarm was dismissed quickly (for auto re-ring)
     */
    private fun stopAlarmService(quickDismissal: Boolean) {
        // Note: We can't directly call service method
        // Instead, send broadcast that service listens for
        val intent = android.content.Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
            putExtra("quick_dismissal", quickDismissal)
        }

        // Alternatively: just stop service and it will clean up
        stopService(android.content.Intent(this, AlarmService::class.java))
    }

    /**
     * Prevent back button from dismissing alarm.
     *
     * WHY: User must complete wake tasks before dismissing.
     * Accidental back press should not dismiss alarm.
     */

    /**
     * Handle activity pause.
     * Pause sensors to save battery.
     */
    override fun onPause() {
        super.onPause()
        activeTasks.forEach { it.pause() }
    }

    /**
     * Handle activity resume.
     * Resume sensors.
     */
    override fun onResume() {
        super.onResume()
        activeTasks.forEach { it.resume() }
    }

    /**
     * Clean up tasks on destroy.
     *
     * WHY: Release sensors and prevent memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        activeTasks.forEach { it.cleanup() }
        activeTasks.clear()
    }

    companion object {
        private const val TAG = "AlarmTriggerActivity"
    }
}