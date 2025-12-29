package com.reliablealarm.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.reliablealarm.app.R
import com.reliablealarm.app.domain.AlarmRepository
import com.reliablealarm.app.domain.StreakRepository
import com.reliablealarm.app.domain.models.Alarm
import com.google.android.material.card.MaterialCardView

class StreakHistoryActivity : AppCompatActivity() {

    private lateinit var streakRepository: StreakRepository
    private lateinit var alarmRepository: AlarmRepository

    private lateinit var alarmSpinner: android.widget.Spinner
    private lateinit var heroStreakCard: MaterialCardView
    private lateinit var currentStreakNumber: TextView
    private lateinit var streakFireEmoji: TextView
    private lateinit var streakMessage: TextView
    private lateinit var longestStreakNumber: TextView
    private lateinit var successRateNumber: TextView
    private lateinit var historyGrid: GridLayout
    private lateinit var emptyStateLayout: LinearLayout

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streak_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Your Streak"
        supportActionBar?.elevation = 0f

        streakRepository = StreakRepository(this)
        alarmRepository = AlarmRepository(this)

        initializeViews()
        setupAlarmSpinner()
    }

    private fun initializeViews() {
        alarmSpinner = findViewById(R.id.alarmSpinner)
        heroStreakCard = findViewById(R.id.heroStreakCard)
        currentStreakNumber = findViewById(R.id.currentStreakNumber)
        streakFireEmoji = findViewById(R.id.streakFireEmoji)
        streakMessage = findViewById(R.id.streakMessage)
        longestStreakNumber = findViewById(R.id.longestStreakNumber)
        successRateNumber = findViewById(R.id.successRateNumber)
        historyGrid = findViewById(R.id.historyGrid)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupAlarmSpinner() {
        val alarms = alarmRepository.getAllAlarms()

        if (alarms.isEmpty()) {
            showEmptyState()
            return
        }

        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            alarms.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        alarmSpinner.adapter = adapter

        alarmSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadStreakForAlarm(alarms[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        if (alarms.isNotEmpty()) {
            loadStreakForAlarm(alarms[0])
        }
    }

    private fun loadStreakForAlarm(alarm: Alarm) {
        val streak = streakRepository.getStreak(alarm.id)
        val confirmations = streakRepository.getConfirmations(alarm.id)

        // Animate stats
        animateStreakCounter(streak.currentStreak)
        animateLongestStreak(streak.longestStreak)
        animateSuccessRate(streak.getSuccessRate())

        // Update motivational message
        updateStreakMessage(streak.currentStreak)

        // Animate fire emoji based on streak
        animateFireEmoji(streak.currentStreak)

        // Display history
        displayHistoryGrid(confirmations)

        // Apply card color based on streak
        applyStreakCardStyle(streak.currentStreak)
    }

    private fun animateStreakCounter(targetStreak: Int) {
        val animator = ValueAnimator.ofInt(0, targetStreak)
        animator.duration = 1000
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { animation ->
            currentStreakNumber.text = animation.animatedValue.toString()
        }
        animator.start()

        // Bounce animation
        currentStreakNumber.scaleX = 0f
        currentStreakNumber.scaleY = 0f
        currentStreakNumber.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(BounceInterpolator())
            .start()
    }

    private fun animateLongestStreak(longestStreak: Int) {
        handler.postDelayed({
            val animator = ValueAnimator.ofInt(0, longestStreak)
            animator.duration = 800
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener { animation ->
                longestStreakNumber.text = animation.animatedValue.toString()
            }
            animator.start()

            longestStreakNumber.alpha = 0f
            longestStreakNumber.animate()
                .alpha(1f)
                .setDuration(500)
                .start()
        }, 200)
    }

    private fun animateSuccessRate(successRate: Int) {
        handler.postDelayed({
            val animator = ValueAnimator.ofInt(0, successRate)
            animator.duration = 800
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.addUpdateListener { animation ->
                successRateNumber.text = "${animation.animatedValue}%"
            }
            animator.start()

            successRateNumber.alpha = 0f
            successRateNumber.animate()
                .alpha(1f)
                .setDuration(500)
                .start()
        }, 400)
    }

    private fun animateFireEmoji(streak: Int) {
        // Scale animation based on streak
        val scale = when {
            streak >= 30 -> 1.3f
            streak >= 14 -> 1.2f
            streak >= 7 -> 1.1f
            else -> 1.0f
        }

        streakFireEmoji.scaleX = 0.5f
        streakFireEmoji.scaleY = 0.5f
        streakFireEmoji.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Pulse animation for active streaks
        if (streak > 0) {
            startFirePulseAnimation()
        }
    }

    private fun startFirePulseAnimation() {
        val pulseAnimator = ObjectAnimator.ofFloat(streakFireEmoji, "scaleX", 1.0f, 1.1f, 1.0f)
        val pulseAnimatorY = ObjectAnimator.ofFloat(streakFireEmoji, "scaleY", 1.0f, 1.1f, 1.0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(pulseAnimator, pulseAnimatorY)
        animatorSet.duration = 1500
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.startDelay = 1000
        animatorSet.start()
    }

    private fun updateStreakMessage(streak: Int) {
        val message = when {
            streak == 0 -> "Start your journey today! 🌟"
            streak < 3 -> "Great start! Keep going! 💪"
            streak < 7 -> "You're building a habit! 🚀"
            streak < 14 -> "Impressive consistency! ⭐"
            streak < 30 -> "You're on fire! 🔥"
            streak < 60 -> "Unstoppable streak! 👑"
            streak < 100 -> "Legend in the making! 🏆"
            else -> "Absolute Champion! 🎯"
        }

        streakMessage.text = message
        streakMessage.alpha = 0f
        streakMessage.translationY = 20f
        streakMessage.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(500)
            .start()
    }

    private fun applyStreakCardStyle(streak: Int) {
        // Apply gradient background for high streaks
        if (streak >= 7) {
            heroStreakCard.setCardBackgroundColor(
                ContextCompat.getColor(this, android.R.color.transparent)
            )
            heroStreakCard.background = ContextCompat.getDrawable(this, R.drawable.gradient_streak)

            // Change text colors to white for better contrast
            currentStreakNumber.setTextColor(Color.WHITE)
            streakMessage.setTextColor(Color.WHITE)
            streakFireEmoji.alpha = 1f
        } else {
            heroStreakCard.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.surface_card)
            )
            currentStreakNumber.setTextColor(
                ContextCompat.getColor(this, R.color.streak_fire)
            )
        }
    }

    private fun displayHistoryGrid(confirmations: List<com.reliablealarm.app.domain.models.WakeConfirmation>) {
        historyGrid.removeAllViews()

        val cellSize = resources.getDimensionPixelSize(R.dimen.streak_cell_size)
        val cellMargin = resources.getDimensionPixelSize(R.dimen.streak_cell_margin)

        // Display last 91 days (13 weeks)
        for (week in 12 downTo 0) {
            for (day in 0 until 7) {
                val cell = View(this)
                val params = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    setMargins(cellMargin, cellMargin, cellMargin, cellMargin)
                    columnSpec = GridLayout.spec(12 - week)
                    rowSpec = GridLayout.spec(day)
                }
                cell.layoutParams = params

                val daysAgo = week * 7 + day
                val date = getDateDaysAgo(daysAgo)
                val confirmation = confirmations.firstOrNull { it.date == date }

                val drawable = when {
                    confirmation == null -> R.drawable.streak_cell_empty
                    confirmation.wokeOnTime == true -> R.drawable.streak_cell_success
                    confirmation.wokeOnTime == false -> R.drawable.streak_cell_missed
                    else -> R.drawable.streak_cell_pending
                }

                cell.background = ContextCompat.getDrawable(this, drawable)

                // Add scale animation with delay
                cell.scaleX = 0f
                cell.scaleY = 0f
                cell.alpha = 0f

                val delay = (week * 7 + day) * 15L // Stagger animation
                cell.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(200)
                    .setStartDelay(delay)
                    .setInterpolator(OvershootInterpolator())
                    .start()

                historyGrid.addView(cell)
            }
        }
    }

    private fun getDateDaysAgo(daysAgo: Int): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return format.format(calendar.time)
    }

    private fun showEmptyState() {
        emptyStateLayout.visibility = View.VISIBLE
        heroStreakCard.visibility = View.GONE

        // Animate empty state
        emptyStateLayout.alpha = 0f
        emptyStateLayout.translationY = 50f
        emptyStateLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}