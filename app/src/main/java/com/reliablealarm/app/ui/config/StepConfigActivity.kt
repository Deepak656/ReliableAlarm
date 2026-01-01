package com.reliablealarm.app.ui.config

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.ui.preview.TaskPreviewActivity
import com.reliablealarm.app.waketasks.TaskConfig

class StepConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_config)

        alarmRepository = AlarmRepository(this)

        val id = intent.getStringExtra("alarm_id")
        if (id == null) {
            Toast.makeText(this, "Error: No alarm ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val loadedAlarm = alarmRepository.getAlarm(id)
        if (loadedAlarm == null) {
            Toast.makeText(this, "Error: Alarm not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        alarm = loadedAlarm

        val seek = findViewById<SeekBar>(R.id.stepsSeek)
        val value = findViewById<TextView>(R.id.stepsValue)
        val save = findViewById<Button>(R.id.saveButton)

        // Load existing config or use defaults
        val existing = alarm.taskSettings["task_steps"] as? TaskConfig.StepConfig
        val currentSteps = existing?.stepsRequired ?: 20

        // Set initial values
        seek.max = 100
        seek.progress = currentSteps
        value.text = "$currentSteps steps"

        // Seekbar listener
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val steps = progress.coerceAtLeast(1)
                value.text = "$steps step${if (steps > 1) "s" else ""}"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        // Open Preview
        val intent = Intent(this, TaskPreviewActivity::class.java)
        intent.putExtra("alarm_id", alarm.id)
        intent.putExtra("task_key", "task_steps")
        startActivity(intent)

        save.setOnClickListener {
            val steps = seek.progress.coerceAtLeast(1)

            val cfg = TaskConfig.StepConfig(
                stepsRequired = steps
            )

            val updated = alarm.copy(
                taskSettings = alarm.taskSettings + ("task_steps" to cfg)
            )

            alarmRepository.saveAlarm(updated)

            setResult(RESULT_OK)
            Toast.makeText(this, "Step config saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}