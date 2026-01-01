package com.reliablealarm.app.waketasks

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm
import kotlin.random.Random

class ColorBallsTask : WakeTask {

    private lateinit var context: Context
    private lateinit var alarm: Alarm
    private var onComplete: (() -> Unit)? = null

    private var gridSize = 6
    private var totalRounds = 5
    private var currentRound = 0

    private lateinit var grid: GridLayout
    private lateinit var progress: ProgressBar
    private lateinit var roundText: TextView
    private lateinit var feedback: TextView

    private var pattern = mutableSetOf<Int>()
    private var user = mutableSetOf<Int>()

    private var showingPattern = true
    private var completed = false

    override fun getName() = "Memory Color Balls"
    override fun getInstructions() = "Memorize colored balls & tap same cells"

    override fun initialize(context: Context, alarm: Alarm, onComplete: () -> Unit) {
        this.context = context
        this.alarm = alarm
        this.onComplete = onComplete

        val cfg = alarm.taskSettings["task_color_balls"] as? TaskConfig.ColorBallsConfig
        totalRounds = cfg?.numberOfround ?: 5
    }

    override fun createView(container: ViewGroup): View {
        val v = LayoutInflater.from(container.context)
            .inflate(R.layout.task_color_balls, container, false)

        grid = v.findViewById(R.id.colorGrid)
        progress = v.findViewById(R.id.colorProgress)
        roundText = v.findViewById(R.id.colorRoundText)
        feedback = v.findViewById(R.id.colorFeedback)

        progress.max = totalRounds

        grid.post { setupGrid() }
        return v
    }

    private fun setupGrid() {
        grid.removeAllViews()
        grid.rowCount = gridSize
        grid.columnCount = gridSize

        val total = gridSize * gridSize

        repeat(total) { index ->
            val cell = View(context)
            cell.background = context.getDrawable(R.drawable.bubble_empty)

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            cell.layoutParams = params

            cell.setOnClickListener {
                if (!showingPattern && !completed) handleTap(index, cell)
            }

            grid.addView(cell)
        }

        startRound()
    }

    private fun startRound() {
        currentRound++
        if (currentRound > totalRounds) return completeGame()

        feedback.text = "Memorize..."
        roundText.text = "Round $currentRound"
        progress.progress = currentRound - 1

        user.clear()
        pattern.clear()
        showingPattern = true

        val totalCells = gridSize * gridSize
        val patternSize = Random.nextInt(4, 9)

        while (pattern.size < patternSize)
            pattern.add(Random.nextInt(totalCells))

        showPattern()
    }

    private fun showPattern() {
        for (i in 0 until grid.childCount) {
            val cell = grid.getChildAt(i)

            if (pattern.contains(i)) {
                animateBubbleAppear(cell, (i * 60).toLong())
            } else {
                cell.background = context.getDrawable(R.drawable.bubble_empty)
            }
        }

        grid.postDelayed({ hidePattern() }, 2600)
    }


    private fun hidePattern() {
        showingPattern = false
        feedback.text = "Now repeat the pattern!"

        for (i in 0 until grid.childCount) {
            val cell = grid.getChildAt(i)

            cell.animate()
                .alpha(0.3f)
                .setDuration(400)
                .withEndAction {
                    cell.background = context.getDrawable(R.drawable.bubble_empty)
                    cell.alpha = 1f
                }.start()
        }
    }


    private fun handleTap(index: Int, cell: View) {
        if (pattern.contains(index)) {
            user.add(index)
            cell.background = context.getDrawable(R.drawable.bubble_circle)

            if (user.size == pattern.size) {
                feedback.text = "Correct! Next round"
                grid.postDelayed({ startRound() }, 900)
            }
        } else {
            cell.setBackgroundColor(0xFFFF4444.toInt())
            feedback.text = "Wrong! Restarting round..."
            grid.postDelayed({ startRound() }, 900)
        }
        cell.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(120)
            .withEndAction {
                cell.scaleX = 1f
                cell.scaleY = 1f
            }.start()

    }

    private fun completeGame() {
        completed = true
        feedback.text = "Completed 🎉"
        progress.progress = totalRounds
        onComplete?.invoke()
    }

    override fun start() {}
    override fun pause() {}
    override fun resume() {}
    override fun cleanup() {}
    override fun isCompleted() = completed
    override fun getProgress(): Float =
        currentRound.toFloat() / totalRounds

    private fun animateBubbleAppear(view: View, delay: Long) {
        view.background = context.getDrawable(R.drawable.bubble_circle)
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f

        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260)
            .setStartDelay(delay)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

}
