package com.reliablealarm.app.ui.preview

import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.config.ReliabilityConfig
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.system.AudioController
import com.reliablealarm.app.waketasks.*

class TaskPreviewActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var audioController: AudioController

    private var alarm: Alarm? = null
    private var wakeTask: WakeTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_preview)

        alarmRepository = AlarmRepository(this)

        val taskKey = intent.getStringExtra(EXTRA_TASK_KEY) ?: run {
            finish(); return
        }

        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        alarm = alarmId?.let { alarmRepository.getAlarm(it) } ?: Alarm() // 👈 preview mode fallback

        val container = findViewById<FrameLayout>(R.id.previewContainer)
        val title = findViewById<TextView>(R.id.previewTitle)

        wakeTask = WakeTaskFactory.create(taskKey) ?: run {
            finish(); return
        }

        title.text = "Preview: ${wakeTask!!.getName()}"

        wakeTask!!.initialize(
            context = this,
            alarm = alarm!!
        ) {
            finish()
        }

        val view = wakeTask!!.createView(container)
        container.addView(view)

        // start AFTER layout pass (important for sensors & camera tasks)
        view.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    wakeTask?.start()
                }
            }
        )

        findViewById<Button>(R.id.btnClosePreview).setOnClickListener { finish() }

        stopAlarmSoundIfAny()
    }

    private fun stopAlarmSoundIfAny() {
        val reliabilityConfig = ReliabilityConfig(applicationContext)
        audioController = AudioController(this, reliabilityConfig)
        audioController.stopAlarm()
    }

    override fun onDestroy() {
        wakeTask?.cleanup()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TASK_KEY = "task_key"
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}