package com.reliablealarm.app.domain.models

/**
 * Model representing wake-up streak tracking for a specific alarm.
 *
 * WHY: Gamification encourages consistent wake-up behavior.
 * Offline-first design means no backend dependency.
 *
 * Design:
 * - alarmId links streak to specific alarm
 * - currentStreak counts consecutive successful wake-ups
 * - longestStreak preserves best achievement
 * - lastWakeDate prevents duplicate counting on same day
 */
data class WakeStreak(
    val alarmId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastWakeDate: String? = null, // Format: "yyyy-MM-dd"
    val totalWakeUps: Int = 0,
    val totalMissed: Int = 0
) {

    /**
     * Calculate success rate percentage.
     * WHY: Provides user feedback on consistency.
     */
    fun getSuccessRate(): Int {
        val total = totalWakeUps + totalMissed
        if (total == 0) return 0
        return ((totalWakeUps.toFloat() / total) * 100).toInt()
    }
}