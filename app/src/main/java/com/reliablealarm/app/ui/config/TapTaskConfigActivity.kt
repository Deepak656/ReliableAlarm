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

class TapTaskConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tap_config)

        alarmRepository = AlarmRepository(this)

        val id = intent.getStringExtra("alarm_id") ?: return
        alarm = alarmRepository.getAlarm(id) ?: return

        val seek = findViewById<SeekBar>(R.id.tapSeek)
        val txt = findViewById<TextView>(R.id.tapValue)
        val save = findViewById<Button>(R.id.saveButton)
        val preview = findViewById<Button>(R.id.previewButton)

        val existing = alarm.taskSettings["task_tap"] as? TaskConfig.TapTaskConfig

        seek.progress = existing?.tapsRequired ?: 30
        txt.text = "${seek.progress} taps"

        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                txt.text = "$p taps"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        save.setOnClickListener {
            saveConfig(seek.progress)
            finish()
        }

        preview.setOnClickListener {
            saveConfig(seek.progress)

            val intent = Intent(this, TaskPreviewActivity::class.java)
            intent.putExtra("alarm_id", alarm.id)
            intent.putExtra("task_key", "task_tap")
            startActivity(intent)
        }
    }

    private fun saveConfig(taps: Int) {
        val cfg = TaskConfig.TapTaskConfig(tapsRequired = taps)

        val updated = alarm.copy(
            taskSettings = alarm.taskSettings + ("task_tap" to cfg)
        )

        alarmRepository.saveAlarm(updated)
        alarm = updated
    }
}

