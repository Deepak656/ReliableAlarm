package com.reliablealarm.app.waketasks

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm
import kotlin.random.Random

class TypingWakeTask : WakeTask {

    private lateinit var context: Context
    private lateinit var alarm: Alarm
    private var onComplete: (() -> Unit)? = null

    private var targetText = ""
    private var completed = false

    override fun getName() = "Typing Challenge"

    override fun getInstructions() =
        "Carefully type the exact sentence shown to dismiss alarm"

    override fun initialize(context: Context, alarm: Alarm, onComplete: () -> Unit) {
        this.context = context
        this.alarm = alarm
        this.onComplete = onComplete

        val cfg = alarm.taskSettings["task_typing"] as? TaskConfig.TypingConfig
        val paraLength = cfg?.paragraphLength ?: 2

        targetText = generateSentence(paraLength)
    }

    override fun createView(container: ViewGroup): View {
        val view = LayoutInflater.from(container.context)
            .inflate(R.layout.task_typing, container, false)

        val sentenceText = view.findViewById<TextView>(R.id.typingSentence)
        val input = view.findViewById<EditText>(R.id.typingInput)
        val feedback = view.findViewById<TextView>(R.id.typingFeedback)

        sentenceText.text = targetText

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val user = s.toString()

                if (user == targetText) {
                    completed = true
                    feedback.text = "Perfect! Completed 🎉"
                    onComplete?.invoke()
                } else {
                    feedback.text = "Keep typing correctly..."
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return view
    }

    override fun start() {}
    override fun pause() {}
    override fun resume() {}
    override fun cleanup() {}

    override fun isCompleted() = completed

    override fun getProgress(): Float = 0f // typing is binary success

    private fun generateSentence(lines: Int): String {
        val options = listOf(
            "Waking up is the first win of the day.",
            "Discipline beats motivation every single morning.",
            "If you snooze you really do lose.",
            "Great days start with strong mornings.",
            "I am stronger than my sleep.",
        )

        return (1..lines)
            .joinToString(" ") { options.random() }
    }
}
