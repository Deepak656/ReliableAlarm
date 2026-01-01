package com.reliablealarm.app.waketasks

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.models.Alarm
import kotlin.random.Random
class PopBalloonsTask : WakeTask {

    private lateinit var context: Context
    private lateinit var alarm: Alarm
    private var onComplete: (() -> Unit)? = null

    private lateinit var arena: FrameLayout
    private lateinit var progress: ProgressBar
    private lateinit var roundText: TextView
    private lateinit var status: TextView

    private var totalRounds = 3
    private var balloonsPerRound = 8
    private var currentRound = 0
    private var popped = 0
    private var completed = false

    override fun getName() = "Pop The Balloons"
    override fun getInstructions() = "Pop all balloons before they escape"

    override fun initialize(context: Context, alarm: Alarm, onComplete: () -> Unit) {
        this.context = context
        this.alarm = alarm
        this.onComplete = onComplete

        val cfg = alarm.taskSettings["task_pop_balloons"] as? TaskConfig.PopBalloonsConfig
        totalRounds = cfg?.rounds ?: 3
        balloonsPerRound = cfg?.balloonsPerRound ?: 8
    }

    override fun createView(parent: ViewGroup): View {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_pop_balloons, parent, false)

        arena = v.findViewById(R.id.popArena)
        progress = v.findViewById(R.id.popProgress)
        roundText = v.findViewById(R.id.popRoundText)
        status = v.findViewById(R.id.popStatus)

        progress.max = totalRounds
        arena.post { startRound() }

        return v
    }

    private fun startRound() {
        currentRound++
        if (currentRound > totalRounds) { winGame(); return }

        status.text = "Get Ready..."
        roundText.text = "Round $currentRound"
        popped = 0

        arena.removeAllViews()

        repeat(balloonsPerRound) {
            spawnBalloon()
        }
    }

    private fun spawnBalloon() {
        val balloon = View(context)
        balloon.background = context.getDrawable(R.drawable.balloon)
        balloon.layoutParams = FrameLayout.LayoutParams(120, 120)

        val startX = Random.nextInt(50, arena.width - 150)
        val startY = arena.height + 200

        balloon.x = startX.toFloat()
        balloon.y = startY.toFloat()

        balloon.setOnClickListener { pop(balloon) }

        arena.addView(balloon)

        balloon.animate()
            .translationY(-300f)
            .setDuration(Random.nextLong(2200, 4000))
            .withEndAction {
                if (arena.indexOfChild(balloon) != -1) failRound()
            }.start()
    }

    private fun pop(balloon: View) {
        popped++

        balloon.animate()
            .scaleX(1.4f)
            .scaleY(1.4f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                arena.removeView(balloon)
            }.start()

        if (popped == balloonsPerRound) {
            roundWin()
        }
    }

    private fun roundWin() {
        progress.progress = currentRound
        status.text = "Nice! 🎉"
        arena.postDelayed({ startRound() }, 1200)
    }

    private fun failRound() {
        status.text = "Oops! Try Again"
        arena.postDelayed({ startRound() }, 1200)
    }

    private fun winGame() {
        completed = true
        status.text = "Awesome! You did it 🎉"
        onComplete?.invoke()
    }

    override fun start() {}
    override fun pause() {}
    override fun resume() {}
    override fun cleanup() {}
    override fun isCompleted() = completed
    override fun getProgress() = currentRound.toFloat() / totalRounds
}
