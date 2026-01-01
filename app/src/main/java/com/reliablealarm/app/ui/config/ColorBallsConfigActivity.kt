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

class ColorBallsConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_balls_config)

        alarmRepository = AlarmRepository(this)

        val id = intent.getStringExtra("alarm_id") ?: return
        alarm = alarmRepository.getAlarm(id) ?: return

        val seek = findViewById<SeekBar>(R.id.ballsSeek)
        val txt = findViewById<TextView>(R.id.ballsValue)
        val save = findViewById<Button>(R.id.saveButton)
        val preview = findViewById<Button>(R.id.previewButton)

        val existing = alarm.taskSettings["task_color_balls"] as? TaskConfig.ColorBallsConfig

        seek.progress = existing?.numberOfround ?: 5
        txt.text = "${seek.progress} rounds"

        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                txt.text = "$p rounds"
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
            intent.putExtra("task_key", "task_color_balls")
            startActivity(intent)
        }
    }

    private fun saveConfig(rounds: Int) {
        val cfg = TaskConfig.ColorBallsConfig(numberOfround = rounds)

        val updated = alarm.copy(
            taskSettings = alarm.taskSettings + ("task_color_balls" to cfg)
        )

        alarmRepository.saveAlarm(updated)
        alarm = updated
    }
}
