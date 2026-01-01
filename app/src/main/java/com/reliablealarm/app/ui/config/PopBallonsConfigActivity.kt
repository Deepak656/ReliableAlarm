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


class PopBalloonsConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pop_balloons_config)

        alarmRepository = AlarmRepository(this)

        val id = intent.getStringExtra("alarm_id") ?: return
        alarm = alarmRepository.getAlarm(id) ?: return

        val roundSeek = findViewById<SeekBar>(R.id.popRoundsSeek)
        val roundText = findViewById<TextView>(R.id.popRoundsValue)

        val countSeek = findViewById<SeekBar>(R.id.popCountSeek)
        val countText = findViewById<TextView>(R.id.popCountValue)

        val preview = findViewById<Button>(R.id.previewButton)
        val save = findViewById<Button>(R.id.saveButton)

        val existing = alarm.taskSettings["task_pop_balloons"] as? TaskConfig.PopBalloonsConfig

        roundSeek.progress = existing?.rounds ?: 3
        countSeek.progress = existing?.balloonsPerRound ?: 8

        roundText.text = "${roundSeek.progress} rounds"
        countText.text = "${countSeek.progress} balloons"

        roundSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                roundText.text = "$p rounds"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        countSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, b: Boolean) {
                countText.text = "$p balloons"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        preview.setOnClickListener {
            val intent = Intent(this, TaskPreviewActivity::class.java)
            intent.putExtra("alarm_id", alarm.id)
            intent.putExtra("task_key", "task_pop_balloons")
            startActivity(intent)
        }

        save.setOnClickListener {
            val cfg = TaskConfig.PopBalloonsConfig(
                rounds = roundSeek.progress.coerceAtLeast(1),
                balloonsPerRound = countSeek.progress.coerceAtLeast(3)
            )

            val updated = alarm.copy(
                taskSettings = alarm.taskSettings + ("task_pop_balloons" to cfg)
            )

            alarmRepository.saveAlarm(updated)
            finish()
        }
    }
}
