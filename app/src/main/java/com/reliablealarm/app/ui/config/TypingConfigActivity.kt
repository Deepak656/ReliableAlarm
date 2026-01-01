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

class TypingConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_typing_config)

        alarmRepository = AlarmRepository(this)

        val id = intent.getStringExtra("alarm_id") ?: return
        alarm = alarmRepository.getAlarm(id) ?: return

        val seek = findViewById<SeekBar>(R.id.lengthSeek)
        val txt = findViewById<TextView>(R.id.lengthValue)
        val save = findViewById<Button>(R.id.saveButton)
        val preview = findViewById<Button>(R.id.previewButton)

        val existing = alarm.taskSettings["task_typing"] as? TaskConfig.TypingConfig

        seek.progress = existing?.paragraphLength ?: 2
        txt.text = "${seek.progress} paragraphs"

        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                txt.text = "$p paragraphs"
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
            intent.putExtra("task_key", "task_typing")
            startActivity(intent)
        }
    }

    private fun saveConfig(length: Int) {
        val cfg = TaskConfig.TypingConfig(paragraphLength = length)

        val updated = alarm.copy(
            taskSettings = alarm.taskSettings + ("task_typing" to cfg)
        )

        alarmRepository.saveAlarm(updated)
        alarm = updated
    }
}

