package com.reliablealarm.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.config.WakeTaskConfig

class WakeTaskOptionsActivity : AppCompatActivity() {

    private lateinit var config: WakeTaskConfig

    private lateinit var mathEnabledSwitch: Switch
    private lateinit var mathCountSeek: SeekBar
    private lateinit var mathCountText: TextView

    private lateinit var stepEnabledSwitch: Switch
    private lateinit var stepCountSeek: SeekBar
    private lateinit var stepCountText: TextView

    private lateinit var shakeEnabledSwitch: Switch
    private lateinit var shakeDurationSeek: SeekBar
    private lateinit var shakeDurationText: TextView
    private lateinit var shakeIntensitySeek: SeekBar
    private lateinit var shakeIntensityText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_task_options)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Wake Task Options"

        config = WakeTaskConfig(this)

        initializeViews()
        loadSettings()
        setupListeners()
    }

    private fun initializeViews() {
        mathEnabledSwitch = findViewById(R.id.mathEnabledSwitch)
        mathCountSeek = findViewById(R.id.mathCountSeek)
        mathCountText = findViewById(R.id.mathCountText)

        stepEnabledSwitch = findViewById(R.id.stepEnabledSwitch)
        stepCountSeek = findViewById(R.id.stepCountSeek)
        stepCountText = findViewById(R.id.stepCountText)

        shakeEnabledSwitch = findViewById(R.id.shakeEnabledSwitch)
        shakeDurationSeek = findViewById(R.id.shakeDurationSeek)
        shakeDurationText = findViewById(R.id.shakeDurationText)
        shakeIntensitySeek = findViewById(R.id.shakeIntensitySeek)
        shakeIntensityText = findViewById(R.id.shakeIntensityText)
    }

    private fun loadSettings() {
        mathEnabledSwitch.isChecked = config.mathTaskEnabled
        mathCountSeek.progress = config.mathProblemCount - 1
        mathCountText.text = "${config.mathProblemCount} problems"

        stepEnabledSwitch.isChecked = config.stepTaskEnabled
        stepCountSeek.progress = (config.stepCount - 5) / 5
        stepCountText.text = "${config.stepCount} steps"

        shakeEnabledSwitch.isChecked = config.shakeTaskEnabled
        shakeDurationSeek.progress = (config.shakeDuration - 5) / 5
        shakeDurationText.text = "${config.shakeDuration} seconds"
        shakeIntensitySeek.progress = config.shakeIntensity - 1
        shakeIntensityText.text = "Level ${config.shakeIntensity}"
    }

    private fun setupListeners() {
        mathEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            config.mathTaskEnabled = isChecked
        }

        mathCountSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val count = progress + 1
                config.mathProblemCount = count
                mathCountText.text = "$count problems"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        stepEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            config.stepTaskEnabled = isChecked
        }

        stepCountSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val count = 5 + progress * 5
                config.stepCount = count
                stepCountText.text = "$count steps"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        shakeEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            config.shakeTaskEnabled = isChecked
        }

        shakeDurationSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = 5 + progress * 5
                config.shakeDuration = duration
                shakeDurationText.text = "$duration seconds"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        shakeIntensitySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val intensity = progress + 1
                config.shakeIntensity = intensity
                shakeIntensityText.text = "Level $intensity"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}