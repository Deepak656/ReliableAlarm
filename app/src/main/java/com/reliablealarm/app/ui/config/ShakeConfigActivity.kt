package com.reliablealarm.app.ui.config

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.ui.preview.TaskPreviewActivity
import com.reliablealarm.app.waketasks.TaskConfig

class ShakeConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shake_config)

        alarmRepository = AlarmRepository(this)

        val id = intent.getStringExtra("alarm_id") ?: return
        alarm = alarmRepository.getAlarm(id) ?: return

        val durationSeek = findViewById<SeekBar>(R.id.durationSeek)
        val intensitySeek = findViewById<SeekBar>(R.id.intensitySeek)

        val durationValue = findViewById<TextView>(R.id.durationValue)
        val intensityValue = findViewById<TextView>(R.id.intensityValue)

        val save = findViewById<Button>(R.id.saveButton)
        val preview = findViewById<Button>(R.id.previewButton)

        val existing = alarm.taskSettings["task_shake"] as? TaskConfig.ShakeConfig

        durationSeek.progress = existing?.durationSeconds ?: 10
        intensitySeek.progress = existing?.intensity ?: 5

        durationValue.text = "${durationSeek.progress} sec"
        intensityValue.text = "${intensitySeek.progress}"

        durationSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                durationValue.text = "$p sec"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        intensitySeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                intensityValue.text = "$p"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        save.setOnClickListener {
            saveConfig(durationSeek.progress, intensitySeek.progress)
            finish()
        }

        preview.setOnClickListener {
            saveConfig(durationSeek.progress, intensitySeek.progress)

            val intent = Intent(this, TaskPreviewActivity::class.java)
            intent.putExtra("alarm_id", alarm.id)
            intent.putExtra("task_key", "task_shake")
            startActivity(intent)
        }
    }

    private fun saveConfig(duration: Int, intensity: Int) {
        val cfg = TaskConfig.ShakeConfig(
            durationSeconds = duration,
            intensity = intensity
        )

        val updated = alarm.copy(
            taskSettings = alarm.taskSettings + ("task_shake" to cfg)
        )

        alarmRepository.saveAlarm(updated)
        alarm = updated
    }
}

