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
        // ── Scheduling ───────────────────────────────────
        addSectionHeader("Scheduling")
        addCardGroup {
            addOption(it, "Dual Scheduling", "Use both AlarmManager and WorkManager",
                config.dualScheduling) { v -> config.dualScheduling = v }
            addDivider(it)
            addOption(it, "Foreground Service", "Keep service alive during alarm",
                config.foregroundService) { v -> config.foregroundService = v }
            addDivider(it)
            addOption(it, "Boot Persistence", "Re-register after reboot",
                config.bootPersistence) { v -> config.bootPersistence = v }
        }

        // ── Audio ────────────────────────────────────────
        addSectionHeader("Audio")
        addCardGroup {
            addOption(it, "Bluetooth Override", "Force speaker output over Bluetooth",
                config.bluetoothOverride) { v -> config.bluetoothOverride = v }
            addDivider(it)
            addOption(it, "Volume Escalation", "Gradually increase volume",
                config.volumeEscalation) { v -> config.volumeEscalation = v }
            addDivider(it)
            addOption(it, "Auto Re-Ring", "Re-ring if dismissed quickly",
                config.autoReRing) { v -> config.autoReRing = v }
        }

        // ── System ───────────────────────────────────────
        addSectionHeader("System")
        addCardGroup {
            addOption(it, "Alarm Watchdog", "Verify alarms on app launch",
                config.alarmWatchdog) { v -> config.alarmWatchdog = v }
            addDivider(it)
            addOption(it, "Low Memory Survival", "Handle memory pressure",
                config.lowMemorySurvival) { v -> config.lowMemorySurvival = v }
            addDivider(it)
            addOption(it, "OEM Battery Detection", "Show battery optimization guidance",
                config.oemBatteryDetection) { v -> config.oemBatteryDetection = v }
        }
    }

    private fun addSectionHeader(title: String) {
        val tv = TextView(this).apply {
            text = title
            textSize = 11f
            setTextColor(getColor(R.color.text_tertiary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val p = resources.getDimensionPixelSize(R.dimen.spacing_s)
            val m = resources.getDimensionPixelSize(R.dimen.spacing_m)
            setPadding(p, m, p, p)
            isAllCaps = true
            letterSpacing = 0.08f
        }
        container.addView(tv)
    }

    private fun addCardGroup(block: (android.widget.LinearLayout) -> Unit) {
        val card = com.google.android.material.card.MaterialCardView(this).apply {
            radius = resources.getDimension(R.dimen.card_radius)
            cardElevation = resources.getDimension(R.dimen.card_elevation)
            setCardBackgroundColor(getColor(R.color.surface_card))
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_s)
            }
            layoutParams = params
        }
        val inner = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        block(inner)
        card.addView(inner)
        container.addView(card)
    }

    private fun addDivider(parent: android.widget.LinearLayout) {
        val divider = android.view.View(this).apply {
            setBackgroundColor(getColor(R.color.background_main))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.spacing_m)
                marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_m)
            }
        }
        parent.addView(divider)
    }

    private fun addOption(
        parent: android.widget.LinearLayout,
        title: String,
        description: String,
        enabled: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        val view = layoutInflater.inflate(R.layout.item_option, parent, false)
        view.findViewById<TextView>(R.id.optionTitle).text = title
        view.findViewById<TextView>(R.id.optionDescription).text = description
        val switch = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.optionSwitch)
        switch.isChecked = enabled
        switch.setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
        parent.addView(view)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}