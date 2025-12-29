package com.reliablealarm.app.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.reliablealarm.app.R
import com.reliablealarm.app.config.ReliabilityConfig

class ReliabilityOptionsActivity : AppCompatActivity() {

    private lateinit var config: ReliabilityConfig
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reliability_options)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reliability Options"

        config = ReliabilityConfig(this)
        container = findViewById(R.id.optionsContainer)

        createOptions()
    }

    private fun createOptions() {
        addOption("Dual Scheduling", "Use both AlarmManager and WorkManager",
            config.dualScheduling) { config.dualScheduling = it }

        addOption("Foreground Service", "Keep service alive during alarm",
            config.foregroundService) { config.foregroundService = it }

        addOption("Bluetooth Override", "Force speaker output over Bluetooth",
            config.bluetoothOverride) { config.bluetoothOverride = it }

        addOption("Volume Escalation", "Gradually increase volume",
            config.volumeEscalation) { config.volumeEscalation = it }

        addOption("Auto Re-Ring", "Re-ring if dismissed quickly",
            config.autoReRing) { config.autoReRing = it }

        addOption("Alarm Watchdog", "Verify alarms on app launch",
            config.alarmWatchdog) { config.alarmWatchdog = it }

        addOption("Boot Persistence", "Re-register after reboot",
            config.bootPersistence) { config.bootPersistence = it }

        addOption("Low Memory Survival", "Handle memory pressure",
            config.lowMemorySurvival) { config.lowMemorySurvival = it }

        addOption("OEM Battery Detection", "Show battery optimization guidance",
            config.oemBatteryDetection) { config.oemBatteryDetection = it }
    }

    private fun addOption(title: String, description: String,
                          enabled: Boolean, onToggle: (Boolean) -> Unit) {
        val view = layoutInflater.inflate(R.layout.item_option, container, false)
        val titleText = view.findViewById<TextView>(R.id.optionTitle)
        val descText = view.findViewById<TextView>(R.id.optionDescription)
        val switch = view.findViewById<Switch>(R.id.optionSwitch)

        titleText.text = title
        descText.text = description
        switch.isChecked = enabled
        switch.setOnCheckedChangeListener { _, isChecked ->
            onToggle(isChecked)
        }

        container.addView(view)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}