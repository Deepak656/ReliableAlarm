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

class TargetTapConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_tap_config)

        alarmRepository = AlarmRepository(this)

        val id = intent.getStringExtra("alarm_id") ?: return
        alarm = alarmRepository.getAlarm(id) ?: return

        val roundSeek = findViewById<SeekBar>(R.id.targetRoundsSeek)
        val roundText = findViewById<TextView>(R.id.targetRoundsValue)

        val countSeek = findViewById<SeekBar>(R.id.targetCountSeek)
        val countText = findViewById<TextView>(R.id.targetCountValue)

        val preview = findViewById<Button>(R.id.previewButton)
        val save = findViewById<Button>(R.id.saveButton)

        val existing = alarm.taskSettings["task_target_tap"] as? TaskConfig.TargetTapConfig

        roundSeek.progress = existing?.rounds ?: 3
        countSeek.progress = existing?.targetsPerRound ?: 8

        roundText.text = "${roundSeek.progress} rounds"
        countText.text = "${countSeek.progress} targets"

        roundSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                roundText.text = "$p rounds"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        countSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                countText.text = "$p targets"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        preview.setOnClickListener {
            val intent = Intent(this, TaskPreviewActivity::class.java)
            intent.putExtra("alarm_id", alarm.id)
            intent.putExtra("task_key", "task_target_tap")
            startActivity(intent)
        }

        save.setOnClickListener {
            val cfg = TaskConfig.TargetTapConfig(
                rounds = roundSeek.progress.coerceAtLeast(1),
                targetsPerRound = countSeek.progress.coerceAtLeast(3)
            )

            val updated = alarm.copy(
                taskSettings = alarm.taskSettings + ("task_target_tap" to cfg)
            )

            alarmRepository.saveAlarm(updated)
            finish()
        }
    }
}
