package com.reliablealarm.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.config.WakeTaskConfig
import com.reliablealarm.app.ui.preview.TaskPreviewActivity

class WakeTaskOptionsActivity : AppCompatActivity() {

    private lateinit var config: WakeTaskConfig

    // Math
    private lateinit var mathEnabledSwitch: Switch
    private lateinit var mathCountSeek: SeekBar
    private lateinit var mathCountText: TextView
    private lateinit var mathPreviewBtn: Button

    // Steps
    private lateinit var stepEnabledSwitch: Switch
    private lateinit var stepCountSeek: SeekBar
    private lateinit var stepCountText: TextView
    private lateinit var stepPreviewBtn: Button

    // Shake
    private lateinit var shakeEnabledSwitch: Switch
    private lateinit var shakeDurationSeek: SeekBar
    private lateinit var shakeDurationText: TextView
    private lateinit var shakeIntensitySeek: SeekBar
    private lateinit var shakeIntensityText: TextView
    private lateinit var shakePreviewBtn: Button

    // QR
    private lateinit var qrEnabledSwitch: Switch
    private lateinit var qrCountSeek: SeekBar
    private lateinit var qrCountText: TextView
    private lateinit var qrPreviewBtn: Button

    // Typing
    private lateinit var typingEnabledSwitch: Switch
    private lateinit var typingParagraphSeek: SeekBar
    private lateinit var typingParagraphText: TextView
    private lateinit var typingPreviewBtn: Button

    // Tap
    private lateinit var tapEnabledSwitch: Switch
    private lateinit var tapCountSeek: SeekBar
    private lateinit var tapCountText: TextView
    private lateinit var tapPreviewBtn: Button

    // Color Balls
    private lateinit var colorBallsEnabledSwitch: Switch
    private lateinit var colorBallsRoundsSeek: SeekBar
    private lateinit var colorBallsRoundsText: TextView
    private lateinit var colorBallsPreviewBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_task_options)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Wake Task Options"

        config = WakeTaskConfig(this)

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {

        mathEnabledSwitch = findViewById(R.id.mathEnabledSwitch)
        mathCountSeek = findViewById(R.id.mathCountSeek)
        mathCountText = findViewById(R.id.mathCountText)
        mathPreviewBtn = findViewById(R.id.mathPreviewBtn)

        stepEnabledSwitch = findViewById(R.id.stepEnabledSwitch)
        stepCountSeek = findViewById(R.id.stepCountSeek)
        stepCountText = findViewById(R.id.stepCountText)
        stepPreviewBtn = findViewById(R.id.stepPreviewBtn)

        shakeEnabledSwitch = findViewById(R.id.shakeEnabledSwitch)
        shakeDurationSeek = findViewById(R.id.shakeDurationSeek)
        shakeDurationText = findViewById(R.id.shakeDurationText)
        shakeIntensitySeek = findViewById(R.id.shakeIntensitySeek)
        shakeIntensityText = findViewById(R.id.shakeIntensityText)
        shakePreviewBtn = findViewById(R.id.shakePreviewBtn)

        qrEnabledSwitch = findViewById(R.id.qrEnabledSwitch)
        qrCountSeek = findViewById(R.id.qrCountSeek)
        qrCountText = findViewById(R.id.qrCountText)
        qrPreviewBtn = findViewById(R.id.qrPreviewBtn)

        typingEnabledSwitch = findViewById(R.id.typingEnabledSwitch)
        typingParagraphSeek = findViewById(R.id.typingParagraphSeek)
        typingParagraphText = findViewById(R.id.typingParagraphText)
        typingPreviewBtn = findViewById(R.id.typingPreviewBtn)

        tapEnabledSwitch = findViewById(R.id.tapEnabledSwitch)
        tapCountSeek = findViewById(R.id.tapCountSeek)
        tapCountText = findViewById(R.id.tapCountText)
        tapPreviewBtn = findViewById(R.id.tapPreviewBtn)

        colorBallsEnabledSwitch = findViewById(R.id.colorBallsEnabledSwitch)
        colorBallsRoundsSeek = findViewById(R.id.colorBallsRoundsSeek)
        colorBallsRoundsText = findViewById(R.id.colorBallsRoundsText)
        colorBallsPreviewBtn = findViewById(R.id.colorBallsPreviewBtn)
    }

    private fun loadSettings() {

        mathEnabledSwitch.isChecked = config.mathTaskEnabled
        mathCountSeek.progress = config.mathProblemCount - 1
        mathCountText.text = "${config.mathProblemCount} problems"

        stepEnabledSwitch.isChecked = config.stepTaskEnabled
        stepCountSeek.progress = (config.stepCount / 5) - 1
        stepCountText.text = "${config.stepCount} steps"

        shakeEnabledSwitch.isChecked = config.shakeTaskEnabled
        shakeDurationSeek.progress = (config.shakeDuration / 5) - 1
        shakeDurationText.text = "${config.shakeDuration} seconds"
        shakeIntensitySeek.progress = config.shakeIntensity - 1
        shakeIntensityText.text = "Level ${config.shakeIntensity}"

        qrEnabledSwitch.isChecked = config.qrTaskEnabled
        qrCountSeek.progress = config.qrCount - 1
        qrCountText.text = "${config.qrCount} codes"

        typingEnabledSwitch.isChecked = config.typingTaskEnabled
        typingParagraphSeek.progress = config.typingParagraphs - 1
        typingParagraphText.text = "${config.typingParagraphs} paragraphs"

        tapEnabledSwitch.isChecked = config.tapTaskEnabled
        tapCountSeek.progress = config.tapCount - 5
        tapCountText.text = "${config.tapCount} taps"

        colorBallsEnabledSwitch.isChecked = config.colorBallsTaskEnabled
        colorBallsRoundsSeek.progress = config.colorBallRounds - 1
        colorBallsRoundsText.text = "${config.colorBallRounds} rounds"
    }

    private fun setupListeners() {

        mathEnabledSwitch.setOnCheckedChangeListener { _, b -> config.mathTaskEnabled = b }

        mathCountSeek.onChange { value ->
            val count = value + 1
            config.mathProblemCount = count
            mathCountText.text = "$count problems"
        }

        stepEnabledSwitch.setOnCheckedChangeListener { _, b -> config.stepTaskEnabled = b }

        stepCountSeek.onChange {
            val steps = (it + 1) * 5
            config.stepCount = steps
            stepCountText.text = "$steps steps"
        }

        shakeEnabledSwitch.setOnCheckedChangeListener { _, b -> config.shakeTaskEnabled = b }

        shakeDurationSeek.onChange {
            val duration = (it + 1) * 5
            config.shakeDuration = duration
            shakeDurationText.text = "$duration seconds"
        }

        shakeIntensitySeek.onChange {
            val level = it + 1
            config.shakeIntensity = level
            shakeIntensityText.text = "Level $level"
        }

        qrEnabledSwitch.setOnCheckedChangeListener { _, b -> config.qrTaskEnabled = b }

        qrCountSeek.onChange {
            val count = it + 1
            config.qrCount = count
            qrCountText.text = "$count codes"
        }

        typingEnabledSwitch.setOnCheckedChangeListener { _, b -> config.typingTaskEnabled = b }

        typingParagraphSeek.onChange {
            val count = it + 1
            config.typingParagraphs = count
            typingParagraphText.text = "$count paragraphs"
        }

        tapEnabledSwitch.setOnCheckedChangeListener { _, b -> config.tapTaskEnabled = b }

        tapCountSeek.onChange {
            val taps = it + 5
            config.tapCount = taps
            tapCountText.text = "$taps taps"
        }

        colorBallsEnabledSwitch.setOnCheckedChangeListener { _, b ->
            config.colorBallsTaskEnabled = b
        }

        colorBallsRoundsSeek.onChange {
            val rounds = it + 1
            config.colorBallRounds = rounds
            colorBallsRoundsText.text = "$rounds rounds"
        }

        // PREVIEW BUTTONS

        mathPreviewBtn.setOnClickListener { preview("task_math") }
        stepPreviewBtn.setOnClickListener { preview("task_steps") }
        shakePreviewBtn.setOnClickListener { preview("task_shake") }
        qrPreviewBtn.setOnClickListener { preview("task_scanqr") }
        typingPreviewBtn.setOnClickListener { preview("task_typing") }
        tapPreviewBtn.setOnClickListener { preview("task_tap") }
        colorBallsPreviewBtn.setOnClickListener { preview("task_color_balls") }
    }

    private fun preview(taskKey: String) {
        val intent = Intent(this, TaskPreviewActivity::class.java)
        intent.putExtra(TaskPreviewActivity.EXTRA_TASK_KEY, taskKey)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

/* ---------- EXTENSION ---------- */

fun SeekBar.onChange(action: (Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            action(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
}