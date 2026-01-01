package com.reliablealarm.app.ui.config

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.ui.preview.TaskPreviewActivity
import com.reliablealarm.app.waketasks.TaskConfig

class MathConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm
    private lateinit var questionCountValueText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_config)

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

        val difficultySpinner = findViewById<Spinner>(R.id.difficulty)
        val countSeek = findViewById<SeekBar>(R.id.questionCount)
        questionCountValueText = findViewById(R.id.questionCountValue)
        val save = findViewById<Button>(R.id.saveButton)

        // Setup difficulty spinner
        val difficulties = arrayOf("EASY", "MEDIUM", "HARD")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        difficultySpinner.adapter = adapter

        // Load existing config or use defaults
        val existingConfig = alarm.taskSettings["task_math"] as? TaskConfig.MathConfig
        val currentCount = existingConfig?.problemCount ?: 3
        val currentDifficulty = existingConfig?.difficulty ?: "MEDIUM"

        // Set initial values
        countSeek.max = 20
        countSeek.progress = currentCount
        questionCountValueText.text = "$currentCount problem${if (currentCount > 1) "s" else ""}"

        val difficultyIndex = difficulties.indexOf(currentDifficulty)
        if (difficultyIndex >= 0) {
            difficultySpinner.setSelection(difficultyIndex)
        }

        // Update text as seekbar changes
        countSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val count = progress.coerceAtLeast(1)
                questionCountValueText.text = "$count problem${if (count > 1) "s" else ""}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        val preview = findViewById<Button>(R.id.previewButton)

        preview.setOnClickListener {

            val count = countSeek.progress.coerceAtLeast(1)

            val cfg = TaskConfig.MathConfig(
                problemCount = count,
                difficulty = difficultySpinner.selectedItem.toString()
            )

            val updated = alarm.copy(
                taskSettings = alarm.taskSettings + ("task_math" to cfg)
            )

            alarmRepository.saveAlarm(updated)
            alarm = updated

            val intent = Intent(this, TaskPreviewActivity::class.java)
            intent.putExtra("alarm_id", alarm.id)
            intent.putExtra("task_key", "task_math")
            startActivity(intent)
        }


        save.setOnClickListener {
            val count = countSeek.progress.coerceAtLeast(1)
            val newCfg = TaskConfig.MathConfig(
                problemCount = count,
                difficulty = difficultySpinner.selectedItem.toString()
            )

            val updated = alarm.copy(
                taskSettings = alarm.taskSettings + ("task_math" to newCfg)
            )

            alarmRepository.saveAlarm(updated)

            setResult(RESULT_OK)
            Toast.makeText(this, "Math config saved", Toast.LENGTH_SHORT).show()
            finish()

        }
    }
}