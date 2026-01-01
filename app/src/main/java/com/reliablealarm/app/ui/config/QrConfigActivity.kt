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

/**
 * Configuration screen for QR Code wake task.
 *
 * WHY: User sets how many QR codes they must scan to dismiss alarm.
 * Can scan ANY QR code - forces user to walk around and find things.
 *
 * Design: Simple count-based system
 * User must scan X different QR codes (product barcodes, posters, books, etc.)
 */
class QrConfigActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarm: Alarm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_config)

        alarmRepository = AlarmRepository(this)

        val preview = findViewById<Button>(R.id.previewButton)
        val countSeek = findViewById<SeekBar>(R.id.qrCountSeek)
        val countValue = findViewById<TextView>(R.id.qrCountValue)
        val save = findViewById<Button>(R.id.saveButton)

        preview.setOnClickListener {

            // Get current selection from seekbar
            val count = countSeek.progress.coerceAtLeast(1)

            // Build new config
            val cfg = TaskConfig.QrConfig(
                qrCodesRequired = count
            )

            // Save updated QR config into alarm (without closing screen)
            val updated = alarm.copy(
                taskSettings = alarm.taskSettings + ("task_scanqr" to cfg)
            )

            alarmRepository.saveAlarm(updated)
            alarm = updated   // update reference

            // Open Preview
            val intent = Intent(this, TaskPreviewActivity::class.java)
            intent.putExtra("alarm_id", alarm.id)
            intent.putExtra("task_key", "task_scanqr")
            startActivity(intent)

        }




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

        // Load existing config or use defaults
        val existing = alarm.taskSettings["task_scanqr"] as? TaskConfig.QrConfig
        val currentCount = existing?.qrCodesRequired ?: 1

        // Set initial values
        countSeek.max = 10
        countSeek.progress = currentCount
        countValue.text = "$currentCount QR code${if (currentCount > 1) "s" else ""}"

        // Seekbar listener
        countSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val count = progress.coerceAtLeast(1)
                countValue.text = "$count QR code${if (count > 1) "s" else ""}"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        save.setOnClickListener {
            val count = countSeek.progress.coerceAtLeast(1)

            val cfg = TaskConfig.QrConfig(
                qrCodesRequired = count
            )

            val updated = alarm.copy(
                taskSettings = alarm.taskSettings + ("task_scanqr" to cfg)
            )

            alarmRepository.saveAlarm(updated)

            setResult(RESULT_OK)
            Toast.makeText(this, "QR config saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}