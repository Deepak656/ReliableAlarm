package com.reliablealarm.app.ui.preview

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.config.ReliabilityConfig
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.system.AudioController
import com.reliablealarm.app.waketasks.ColorBallsTask
import com.reliablealarm.app.waketasks.MathWakeTask
import com.reliablealarm.app.waketasks.QrWakeTask
import com.reliablealarm.app.waketasks.ShakeWakeTask
import com.reliablealarm.app.waketasks.StepWakeTask
import com.reliablealarm.app.waketasks.TapTask
import com.reliablealarm.app.waketasks.TypingWakeTask
import com.reliablealarm.app.waketasks.WakeTask

class TaskPreviewActivity : AppCompatActivity() {

    private lateinit var alarmRepository: AlarmRepository
    private lateinit var reliabilityConfig: ReliabilityConfig
    private lateinit var audioController: AudioController

    private var alarm: Alarm? = null
    private var wakeTask: WakeTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_preview)

        // 👇 Initialize here — NOT in constructor
        reliabilityConfig = ReliabilityConfig(applicationContext)
        audioController = AudioController(this, reliabilityConfig)

        alarmRepository = AlarmRepository(this)

        val alarmId = intent.getStringExtra("alarm_id")
        val taskKey = intent.getStringExtra("task_key")

        if (alarmId == null || taskKey == null) {
            finish()
            return
        }

        alarm = alarmRepository.getAlarm(alarmId)
        if (alarm == null) {
            finish()
            return
        }

        val container = findViewById<FrameLayout>(R.id.previewContainer)
        val title = findViewById<TextView>(R.id.previewTitle)

        // Create task
        wakeTask = createTask(taskKey)
        if (wakeTask == null) {
            finish()
            return
        }

        wakeTask!!.initialize(
            this,
            alarm!!
        ) {
            finish()
        }

        val view = wakeTask!!.createView(container)
        container.addView(view)

        title.text = "Preview: ${wakeTask!!.getName()}"

        // Close button
        findViewById<Button>(R.id.btnClosePreview).setOnClickListener {
            finish()
        }

        // 🔇 Stop alarm sound during preview
        try {
            audioController.stopAlarm()
        } catch (_: Exception) {}

        wakeTask!!.start()
    }

    private fun createTask(taskKey: String): WakeTask? {
        return when (taskKey) {
            "task_math" -> MathWakeTask()
            "task_steps" -> StepWakeTask()
            "task_shake" -> ShakeWakeTask()
            "task_scanqr" -> QrWakeTask()
            "task_typing" -> TypingWakeTask()
            "task_tap" -> TapTask()
            "task_color_balls" -> ColorBallsTask()
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeTask?.cleanup()
    }
}
