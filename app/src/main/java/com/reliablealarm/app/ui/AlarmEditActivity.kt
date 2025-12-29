package com.reliablealarm.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.AlarmScheduler
import com.reliablealarm.app.domain.models.Alarm

class AlarmEditActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmScheduler: AlarmScheduler

    private var existingAlarm: Alarm? = null

    private lateinit var timePicker: TimePicker
    private lateinit var repeatRow: LinearLayout
    private lateinit var repeatSummary: TextView
    private lateinit var labelEdit: EditText
    private lateinit var messageEdit: EditText
    private lateinit var saveButton: Button

    private val repeatDays = BooleanArray(7) // Mon → Sun

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_edit)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        alarmRepository = AlarmRepository(this)
        alarmScheduler = AlarmScheduler(this)

        timePicker = findViewById(R.id.timePicker)
        repeatRow = findViewById(R.id.repeatRow)
        repeatSummary = findViewById(R.id.repeatSummary)
        labelEdit = findViewById(R.id.labelEdit)
        messageEdit = findViewById(R.id.messageEdit)
        saveButton = findViewById(R.id.saveButton)

        timePicker.setIs24HourView(false)

        intent.getStringExtra("alarm_id")?.let {
            supportActionBar?.title = "Edit Alarm"
            loadAlarm(it)
        } ?: run {
            supportActionBar?.title = "New Alarm"
        }

        repeatRow.setOnClickListener { showRepeatDialog() }
        saveButton.setOnClickListener { saveAlarm() }

        updateRepeatSummary()
    }

    private fun loadAlarm(id: String) {
        existingAlarm = alarmRepository.getAlarm(id)
        existingAlarm?.let { alarm ->
            labelEdit.setText(alarm.name)
            messageEdit.setText(alarm.message)
            timePicker.hour = alarm.hour
            timePicker.minute = alarm.minute

            repeatDays[0] = alarm.repeatMonday
            repeatDays[1] = alarm.repeatTuesday
            repeatDays[2] = alarm.repeatWednesday
            repeatDays[3] = alarm.repeatThursday
            repeatDays[4] = alarm.repeatFriday
            repeatDays[5] = alarm.repeatSaturday
            repeatDays[6] = alarm.repeatSunday
        }
    }

    private fun showRepeatDialog() {
        val days = arrayOf(
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
        )

        AlertDialog.Builder(this)
            .setTitle("Repeat")
            .setMultiChoiceItems(days, repeatDays) { _, which, checked ->
                repeatDays[which] = checked
            }
            .setPositiveButton("Done") { _, _ ->
                updateRepeatSummary()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRepeatSummary() {
        val selected = repeatDays.count { it }

        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        repeatSummary.text = when (selected) {
            0 -> "Never"
            7 -> "Every day"
            else -> {
                val chosen = mutableListOf<String>()
                for (i in repeatDays.indices) {
                    if (repeatDays[i]) chosen.add(names[i])
                }
                chosen.joinToString(", ")
            }
        }
    }



    private fun saveAlarm() {
        val alarm = (existingAlarm ?: Alarm()).copy(
            name = labelEdit.text.toString().ifBlank { "Alarm" },
            message = messageEdit.text.toString().ifBlank { "Wake up!" },
            hour = timePicker.hour,
            minute = timePicker.minute,
            repeatMonday = repeatDays[0],
            repeatTuesday = repeatDays[1],
            repeatWednesday = repeatDays[2],
            repeatThursday = repeatDays[3],
            repeatFriday = repeatDays[4],
            repeatSaturday = repeatDays[5],
            repeatSunday = repeatDays[6]
        )

        val saved = alarmRepository.saveAlarm(alarm)
        alarmScheduler.scheduleAlarm(saved)

        Toast.makeText(this, "Alarm saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
