package com.reliablealarm.app.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.ui.config.ColorBallsConfigActivity
import com.reliablealarm.app.ui.config.MathConfigActivity
import com.reliablealarm.app.ui.config.ShakeConfigActivity
import com.reliablealarm.app.ui.config.StepConfigActivity
import com.reliablealarm.app.ui.config.QrConfigActivity
import com.reliablealarm.app.ui.config.TapTaskConfigActivity
import com.reliablealarm.app.ui.config.TypingConfigActivity
import com.reliablealarm.app.waketasks.TaskConfig
import com.reliablealarm.app.waketasks.WakeTaskType

class AlarmEditActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private var existingAlarm: Alarm? = null

    private lateinit var timePicker: TimePicker
    private lateinit var labelEdit: EditText
    private lateinit var messageEdit: EditText
    private lateinit var saveButton: Button
    private val repeatDays = BooleanArray(7)
    private val repeatLabels = arrayOf("S","M","T","W","T","F","S")
    private val selectedTasks = mutableSetOf<String>()

    // Modern Activity Result API
    private val taskConfigLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Reload alarm to get updated config
            existingAlarm?.id?.let { id ->
                existingAlarm = alarmRepository.getAlarm(id)
                buildTaskGrid() // Refresh the task grid to show updated config
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_edit)

        alarmRepository = AlarmRepository(this)

        timePicker = findViewById(R.id.timePicker)
        labelEdit = findViewById(R.id.labelEdit)
        messageEdit = findViewById(R.id.messageEdit)
        saveButton = findViewById(R.id.saveButton)

        timePicker.setIs24HourView(false)

        intent.getStringExtra("alarm_id")?.let {
            loadAlarm(it)
        }
        saveButton.setOnClickListener { saveAlarm() }

        buildRepeatGrid()
        buildTaskGrid()
    }


    private fun loadAlarm(id: String) {
        existingAlarm = alarmRepository.getAlarm(id)
        existingAlarm?.let { alarm ->
            labelEdit.setText(alarm.name)
            messageEdit.setText(alarm.message)
            timePicker.hour = alarm.hour
            timePicker.minute = alarm.minute

            selectedTasks.clear()
            selectedTasks.addAll(alarm.wakeTasks)

            // Load repeat days
            repeatDays[0] = alarm.repeatSunday
            repeatDays[1] = alarm.repeatMonday
            repeatDays[2] = alarm.repeatTuesday
            repeatDays[3] = alarm.repeatWednesday
            repeatDays[4] = alarm.repeatThursday
            repeatDays[5] = alarm.repeatFriday
            repeatDays[6] = alarm.repeatSaturday
        }
    }

    private fun saveAlarm() {
        val alarm = (existingAlarm ?: Alarm()).copy(
            name = labelEdit.text.toString().ifBlank { "Alarm" },
            message = messageEdit.text.toString().ifBlank { "Wake up!" },
            hour = timePicker.hour,
            minute = timePicker.minute,
            wakeTasks = selectedTasks.toList(),
            repeatMonday = repeatDays[1],
            repeatTuesday = repeatDays[2],
            repeatWednesday = repeatDays[3],
            repeatThursday = repeatDays[4],
            repeatFriday = repeatDays[5],
            repeatSaturday = repeatDays[6],
            repeatSunday = repeatDays[0],
        )

        val saved = alarmRepository.saveAlarm(alarm)
        // Update existingAlarm so config activities have the right ID
        existingAlarm = saved
        Toast.makeText(this, "Alarm Saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun buildRepeatGrid() {
        val container = findViewById<LinearLayout>(R.id.repeatGrid)
        container.removeAllViews()

        repeatLabels.forEachIndexed { index, label ->
            val tv = TextView(this).apply {
                text = label
                textSize = 16f
                setPadding(24,24,24,24)
                setBackgroundResource(
                    if (repeatDays[index]) R.drawable.repeat_selected
                    else R.drawable.repeat_unselected
                )
                setTextColor(
                    if (repeatDays[index]) 0xFFFFFFFF.toInt()
                    else 0xFF000000.toInt()
                )
                setOnClickListener {
                    repeatDays[index] = !repeatDays[index]
                    updateRepeatButtonState(this, repeatDays[index])
                }
            }
            container.addView(tv)
        }
    }

    private fun updateRepeatButtonState(tv: TextView, selected: Boolean) {
        if (selected) {
            tv.setBackgroundResource(R.drawable.repeat_selected)
            tv.setTextColor(0xFFFFFFFF.toInt())
        } else {
            tv.setBackgroundResource(R.drawable.repeat_unselected)
            tv.setTextColor(0xFF000000.toInt())
        }
    }

    private fun buildTaskGrid() {
        val container = findViewById<LinearLayout>(R.id.taskGrid)
        container.removeAllViews()

        // Show all selected tasks
        selectedTasks.forEach { taskKey ->
            val card = createTaskCard(taskKey)
            container.addView(card)
        }

        // Always show the "+ Add Task" button
        val plus = TextView(this).apply {
            text = "+ Add Task"
            textSize = 20f
            setPadding(40,30,40,30)
            setBackgroundResource(R.drawable.task_plus)
            setOnClickListener { showTaskPicker() }
        }

        container.addView(plus)
    }

    private fun createTaskCard(taskKey: String): LinearLayout {
        val type = WakeTaskType.fromKey(taskKey)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(30, 30, 30, 30)
            setBackgroundResource(R.drawable.task_big_card)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 12, 0, 12)
            layoutParams = params
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openTaskConfig(taskKey)
            }
        }

        val iconView = ImageView(this).apply {
            setImageResource(getTaskIcon(taskKey))
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                setMargins(0, 0, 20, 0)
            }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            addView(TextView(this@AlarmEditActivity).apply {
                text = type?.displayName ?: "Task"
                textSize = 18f
                setTextColor(0xFF000000.toInt())
            })

            addView(TextView(this@AlarmEditActivity).apply {
                text = getTaskSummary(taskKey)
                textSize = 14f
                setTextColor(0xFF666666.toInt())
                setPadding(0, 4, 0, 0)
            })
        }

        // Add delete button with click handling
        val deleteButton = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            layoutParams = LinearLayout.LayoutParams(60, 60)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                // Remove task and rebuild grid
                selectedTasks.remove(taskKey)
                buildTaskGrid()
            }
        }

        card.addView(iconView)
        card.addView(textLayout)
        card.addView(deleteButton)

        return card
    }

    private fun showTaskPicker() {
        val all = WakeTaskType.values()
        val names = all.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Choose Wake Task")
            .setItems(names) { _, index ->
                val taskKey = all[index].key

                // Add task to selected list
                if (!selectedTasks.contains(taskKey)) {
                    selectedTasks.add(taskKey)

                    // Save alarm first to ensure ID exists
                    val tempAlarm = (existingAlarm ?: Alarm()).copy(
                        name = labelEdit.text.toString().ifBlank { "Alarm" },
                        message = messageEdit.text.toString().ifBlank { "Wake up!" },
                        hour = timePicker.hour,
                        minute = timePicker.minute,
                        wakeTasks = selectedTasks.toList(),
                        repeatMonday = repeatDays[1],
                        repeatTuesday = repeatDays[2],
                        repeatWednesday = repeatDays[3],
                        repeatThursday = repeatDays[4],
                        repeatFriday = repeatDays[5],
                        repeatSaturday = repeatDays[6],
                        repeatSunday = repeatDays[0],
                    )
                    val saved = alarmRepository.saveAlarm(tempAlarm)
                    existingAlarm = saved

                    // Immediately open config activity
                    openTaskConfig(taskKey)
                } else {
                    // Task already exists, just open config
                    openTaskConfig(taskKey)
                }

                buildTaskGrid()
            }
            .show()
    }

    private fun getTaskIcon(key: String): Int {
        return when (key) {
            "task_math" -> R.drawable.ic_math
            "task_steps" -> R.drawable.ic_walk
            "task_shake" -> R.drawable.ic_shake
            "task_scanqr" -> R.drawable.ic_qr
            "task_typing" -> R.drawable.ic_typing
            "task_tap" -> R.drawable.ic_tap
            "task_color_balls" -> R.drawable.ic_game
            else -> R.drawable.ic_game
        }
    }

    private fun getTaskSummary(key: String): String {
        val alarm = existingAlarm

        if (alarm == null) {
            return "Tap to configure"
        }

        return when (val cfg = alarm.taskSettings[key]) {
            is TaskConfig.MathConfig ->
                "${cfg.problemCount} problems · ${cfg.difficulty}"

            is TaskConfig.StepConfig ->
                "${cfg.stepsRequired} steps"

            is TaskConfig.ShakeConfig ->
                "Shake ${cfg.durationSeconds}s · Intensity ${cfg.intensity}"

            is TaskConfig.QrConfig ->
                "${cfg.qrCodesRequired} QR code${if (cfg.qrCodesRequired > 1) "s" else ""}"

            is TaskConfig.TypingConfig ->
                "${cfg.paragraphLength} paragraphs"

            is TaskConfig.TapTaskConfig ->
                "${cfg.tapsRequired} taps"

            is TaskConfig.ColorBallsConfig ->
                "${cfg.numberOfround} rounds"

            else -> "Tap to configure"
        }
    }

    private fun openTaskConfig(key: String) {
        val intent = when (key) {
            "task_math" -> Intent(this, MathConfigActivity::class.java)
            "task_steps" -> Intent(this, StepConfigActivity::class.java)
            "task_shake" -> Intent(this, ShakeConfigActivity::class.java)
            "task_scanqr" -> Intent(this, QrConfigActivity::class.java)
            "task_tap" -> Intent(this, TapTaskConfigActivity::class.java)
            "task_typing" -> Intent(this, TypingConfigActivity::class.java)
            "task_color_balls" -> Intent(this, ColorBallsConfigActivity::class.java)
            // Add other config activities as they're implemented
            else -> {
                Toast.makeText(this, "Configuration not available yet", Toast.LENGTH_SHORT).show()
                return
            }
        }

        intent.putExtra("alarm_id", existingAlarm?.id)
        taskConfigLauncher.launch(intent)
    }
}