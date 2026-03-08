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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class StreakHistoryActivity : AppCompatActivity() {

    private lateinit var streakRepository: StreakRepository
    private lateinit var alarmRepository: AlarmRepository

    private lateinit var alarmChipGroup: ChipGroup
    private lateinit var heroStreakCard: MaterialCardView
    private lateinit var currentStreakNumber: TextView
    private lateinit var streakFireEmoji: android.widget.ImageView
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
        setupAlarmChips()
    }

    private fun initializeViews() {
        alarmChipGroup = findViewById(R.id.alarmChipGroup)
        heroStreakCard = findViewById(R.id.heroStreakCard)
        currentStreakNumber = findViewById(R.id.currentStreakNumber)
        streakFireEmoji = findViewById(R.id.streakFireEmoji)
        streakMessage = findViewById(R.id.streakMessage)
        longestStreakNumber = findViewById(R.id.longestStreakNumber)
        successRateNumber = findViewById(R.id.successRateNumber)
        historyGrid = findViewById(R.id.historyGrid)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }

    private fun setupAlarmChips() {
        val alarms = alarmRepository.getAllAlarms()

        if (alarms.isEmpty()) {
            showEmptyState()
            return
        }

        alarms.forEachIndexed { index, alarm ->
            val chip = Chip(this).apply {
                text = alarm.name
                isCheckable = true
                isChecked = index == 0
                chipCornerRadius = resources.getDimensionPixelSize(R.dimen.spacing_xl).toFloat()
                setChipBackgroundColorResource(
                    if (index == 0) R.color.accent_orange_light else R.color.surface_elevated
                )
                setTextColor(
                    ContextCompat.getColor(
                        this@StreakHistoryActivity,
                        if (index == 0) R.color.accent_orange else R.color.text_secondary
                    )
                )
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        setChipBackgroundColorResource(R.color.accent_orange_light)
                        setTextColor(ContextCompat.getColor(
                            this@StreakHistoryActivity, R.color.accent_orange
                        ))
                        loadStreakForAlarm(alarm)
                    } else {
                        setChipBackgroundColorResource(R.color.surface_elevated)
                        setTextColor(ContextCompat.getColor(
                            this@StreakHistoryActivity, R.color.text_secondary
                        ))
                    }
                }
            }
            alarmChipGroup.addView(chip)
        }

        // Load first alarm by default
        loadStreakForAlarm(alarms[0])
    }

    private fun loadStreakForAlarm(alarm: Alarm) {
        val streak = streakRepository.getStreak(alarm.id)
        val confirmations = streakRepository.getConfirmations(alarm.id)

        animateStreakCounter(streak.currentStreak)
        animateLongestStreak(streak.longestStreak)
        animateSuccessRate(streak.getSuccessRate())
        updateStreakMessage(streak.currentStreak)
        animateFireEmoji(streak.currentStreak)
        displayHistoryGrid(confirmations)
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
        val scale = when {
            streak >= 30 -> 1.3f
            streak >= 14 -> 1.2f
            streak >= 7  -> 1.1f
            else         -> 1.0f
        }

        streakFireEmoji.scaleX = 0.5f
        streakFireEmoji.scaleY = 0.5f
        streakFireEmoji.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator())
            .start()

        if (streak > 0) startFirePulseAnimation()
    }

    private fun startFirePulseAnimation() {
        val pulseX = ObjectAnimator.ofFloat(streakFireEmoji, "scaleX", 1.0f, 1.1f, 1.0f)
        val pulseY = ObjectAnimator.ofFloat(streakFireEmoji, "scaleY", 1.0f, 1.1f, 1.0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(pulseX, pulseY)
        animatorSet.duration = 1500
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.startDelay = 1000
        animatorSet.start()
    }

    private fun updateStreakMessage(streak: Int) {
        // No emojis — clean text only
        val message = when {
            streak == 0  -> "Start your first streak tomorrow"
            streak < 3   -> "Great start. Keep going."
            streak < 7   -> "You're building a habit"
            streak < 14  -> "Impressive consistency"
            streak < 30  -> "You're on fire"
            streak < 60  -> "Unstoppable streak"
            streak < 100 -> "Legend in the making"
            else         -> "Absolute champion"
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
        if (streak >= 7) {
            heroStreakCard.setCardBackgroundColor(Color.TRANSPARENT)
            heroStreakCard.background = ContextCompat.getDrawable(this, R.drawable.gradient_streak)
            currentStreakNumber.setTextColor(Color.WHITE)
            streakMessage.setTextColor(Color.WHITE)
        } else {
            heroStreakCard.setCardBackgroundColor(
                ContextCompat.getColor(this, R.color.surface_dark)
            )
            currentStreakNumber.setTextColor(
                ContextCompat.getColor(this, R.color.accent_orange)
            )
            streakMessage.setTextColor(
                ContextCompat.getColor(this, R.color.text_tertiary)
            )
        }
    }

    private fun displayHistoryGrid(confirmations: List<com.reliablealarm.app.domain.models.WakeConfirmation>) {
        historyGrid.removeAllViews()

        val cellSize = resources.getDimensionPixelSize(R.dimen.streak_cell_size)
        val cellMargin = resources.getDimensionPixelSize(R.dimen.streak_cell_margin)

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

                cell.scaleX = 0f
                cell.scaleY = 0f
                cell.alpha = 0f

                val delay = (week * 7 + day) * 15L
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