package com.reliablealarm.app.waketasks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm

class TapTask : WakeTask {

    private lateinit var context: Context
    private lateinit var alarm: Alarm
    private var onComplete: (() -> Unit)? = null

    private var requiredTaps = 30
    private var currentTaps = 0
    private var completed = false

    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var instructions: TextView? = null

    override fun getName() = "Tap Challenge"

    override fun getInstructions() = "Tap the button repeatedly until completed"

    override fun initialize(context: Context, alarm: Alarm, onComplete: () -> Unit) {
        this.context = context
        this.alarm = alarm
        this.onComplete = onComplete

        val cfg = alarm.taskSettings["task_tap"] as? TaskConfig.TapTaskConfig
        requiredTaps = cfg?.tapsRequired ?: 30
    }

    override fun createView(container: ViewGroup): View {
        val view = LayoutInflater.from(container.context)
            .inflate(R.layout.task_tap, container, false)

        progressBar = view.findViewById(R.id.tapProgress)
        progressText = view.findViewById(R.id.tapProgressText)
        instructions = view.findViewById(R.id.tapInstructions)

        progressBar?.max = requiredTaps
        updateUI()

        val button = view.findViewById<Button>(R.id.tapButton)
        button.setOnClickListener {
            if (completed) return@setOnClickListener

            currentTaps++
            updateUI()

            if (currentTaps >= requiredTaps) {
                completed = true
                onComplete?.invoke()
            }
        }

        return view
    }

    private fun updateUI() {
        progressBar?.progress = currentTaps
        progressText?.text = "$currentTaps / $requiredTaps taps"
    }

    override fun start() {}
    override fun pause() {}
    override fun resume() {}
    override fun cleanup() {}

    override fun isCompleted() = completed

    override fun getProgress(): Float {
        return currentTaps.toFloat() / requiredTaps
    }
}
