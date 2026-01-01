package com.reliablealarm.app.waketasks

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm
import kotlin.random.Random

/**
 * Math problem solving wake task.
 *
 * WHY: Forces cognitive engagement to dismiss alarm.
 * Difficult to solve while groggy, ensuring user is awake.
 *
 * Difficulty levels:
 * - EASY: Single digit addition/subtraction (e.g., 5 + 3)
 * - MEDIUM: Two digit addition/subtraction, single digit multiplication (e.g., 23 + 17, 6 * 7)
 * - HARD: Two digit multiplication, three digit addition (e.g., 12 * 15, 234 + 567)
 *
 * Progressive difficulty: Each problem slightly harder than last.
 */
class MathWakeTask : WakeTask {

    private var problemCount: Int = 3
    private var difficulty: String = "MEDIUM"
    private var currentProblem: Int = 0
    private var completed: Boolean = false
    private var onComplete: (() -> Unit)? = null

    private lateinit var problemText: TextView
    private lateinit var answerInput: EditText
    private lateinit var feedbackText: TextView
    private lateinit var progressText: TextView

    private var currentAnswer: Int = 0

    override fun getName(): String = "Math Challenge"

    override fun getInstructions(): String =
        "Solve $problemCount math problems to reduce alarm volume"

    override fun initialize(context: Context, alarm: Alarm, onComplete: () -> Unit) {
        this.onComplete = onComplete

        val config = alarm.taskSettings["task_math"] as? TaskConfig.MathConfig

        this.problemCount = config?.problemCount ?: 3
        this.difficulty = config?.difficulty ?: "MEDIUM"

        currentProblem = 0
        completed = false
    }


    override fun createView(container: ViewGroup): View {
        val inflater = LayoutInflater.from(container.context)
        val view = inflater.inflate(R.layout.task_math, container, false)

        problemText = view.findViewById(R.id.mathProblem)
        answerInput = view.findViewById(R.id.mathAnswer)
        feedbackText = view.findViewById(R.id.mathFeedback)
        progressText = view.findViewById(R.id.mathProgress)
        val submitButton = view.findViewById<Button>(R.id.mathSubmit)

        answerInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                feedbackText.text = ""
            }
        })

        submitButton.setOnClickListener {
            checkAnswer()
        }

        generateProblem()

        return view
    }

    override fun start() {
        // Already started in createView
    }

    override fun pause() {
        // No resources to pause
    }

    override fun resume() {
        // No resources to resume
    }

    override fun cleanup() {
        onComplete = null
    }

    override fun isCompleted(): Boolean = completed

    override fun getProgress(): Float = currentProblem.toFloat() / problemCount.toFloat()

    /**
     * Generate new math problem based on difficulty and progress.
     * WHY: Progressive difficulty keeps user engaged.
     */
    private fun generateProblem() {
        progressText.text = "Problem ${currentProblem + 1} of $problemCount"

        // Increase difficulty as problems progress
        val effectiveDifficulty = when (difficulty) {
            "EASY" -> if (currentProblem >= problemCount - 1) "MEDIUM" else "EASY"
            "MEDIUM" -> if (currentProblem >= problemCount - 1) "HARD" else "MEDIUM"
            else -> "HARD"
        }

        val (problem, answer) = when (effectiveDifficulty) {
            "EASY" -> generateEasyProblem()
            "HARD" -> generateHardProblem()
            else -> generateMediumProblem()
        }

        problemText.text = problem
        currentAnswer = answer
        answerInput.text?.clear()
        feedbackText.text = ""
    }

    private fun generateEasyProblem(): Pair<String, Int> {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        val operation = if (Random.nextBoolean()) "+" else "-"

        return if (operation == "+") {
            "$a + $b = ?" to (a + b)
        } else {
            val larger = maxOf(a, b)
            val smaller = minOf(a, b)
            "$larger - $smaller = ?" to (larger - smaller)
        }
    }

    private fun generateMediumProblem(): Pair<String, Int> {
        val choice = Random.nextInt(3)

        return when (choice) {
            0 -> {
                // Two digit addition
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(10, 50)
                "$a + $b = ?" to (a + b)
            }
            1 -> {
                // Two digit subtraction
                val a = Random.nextInt(20, 99)
                val b = Random.nextInt(10, a)
                "$a - $b = ?" to (a - b)
            }
            else -> {
                // Single digit multiplication
                val a = Random.nextInt(2, 10)
                val b = Random.nextInt(2, 10)
                "$a × $b = ?" to (a * b)
            }
        }
    }

    private fun generateHardProblem(): Pair<String, Int> {
        val choice = Random.nextInt(2)

        return when (choice) {
            0 -> {
                // Two digit multiplication
                val a = Random.nextInt(11, 20)
                val b = Random.nextInt(11, 20)
                "$a × $b = ?" to (a * b)
            }
            else -> {
                // Three digit addition
                val a = Random.nextInt(100, 500)
                val b = Random.nextInt(100, 500)
                "$a + $b = ?" to (a + b)
            }
        }
    }

    /**
     * Check user's answer.
     * WHY: Validate and provide feedback.
     */
    private fun checkAnswer() {
        val userAnswer = answerInput.text.toString().toIntOrNull()

        if (userAnswer == null) {
            feedbackText.text = "Please enter a number"
            return
        }

        if (userAnswer == currentAnswer) {
            feedbackText.text = "Correct! ✓"
            feedbackText.setTextColor(0xFF4CAF50.toInt())

            currentProblem++

            if (currentProblem >= problemCount) {
                // All problems solved
                completed = true
                feedbackText.text = "All problems solved! Alarm volume reduced."
                onComplete?.invoke()
            } else {
                // Next problem
                problemText.postDelayed({
                    generateProblem()
                }, 1000)
            }
        } else {
            feedbackText.text = "Incorrect, try again"
            feedbackText.setTextColor(0xFFF44336.toInt())
        }
    }
}