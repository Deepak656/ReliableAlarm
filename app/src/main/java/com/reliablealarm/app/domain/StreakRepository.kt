package com.reliablealarm.app.domain

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.reliablealarm.app.domain.models.WakeStreak
import com.reliablealarm.app.domain.models.WakeConfirmation
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for wake streak and confirmation tracking.
 *
 * WHY: Gamification layer for habit formation.
 * Fully offline - no backend dependency.
 *
 * Storage:
 * - SharedPreferences for streaks (small, frequently accessed)
 * - SharedPreferences for confirmations (append-only log)
 *
 * Thread-safety: All operations synchronized on prefs.
 */
class StreakRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "wake_streaks",
        Context.MODE_PRIVATE
    )

    private val confirmPrefs: SharedPreferences = context.getSharedPreferences(
        "wake_confirmations",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ==================== Streak Management ====================

    /**
     * Get or create streak for an alarm.
     * WHY: Ensure streak exists before updating.
     *
     * @param alarmId Alarm ID
     * @return WakeStreak, creates new if doesn't exist
     */
    fun getStreak(alarmId: String): WakeStreak {
        val json = prefs.getString("streak_$alarmId", null)
        return if (json != null) {
            try {
                gson.fromJson(json, WakeStreak::class.java)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to parse streak", e)
                WakeStreak(alarmId)
            }
        } else {
            WakeStreak(alarmId)
        }
    }

    /**
     * Record successful wake-up.
     * WHY: Increment streak if consecutive, update statistics.
     *
     * Logic:
     * - If last wake was yesterday: increment streak
     * - If last wake was today: no change (duplicate)
     * - If last wake was earlier: reset streak to 1
     * - Update longest streak if current exceeds it
     *
     * @param alarmId Alarm ID
     * @param date Date string "yyyy-MM-dd"
     */
    fun recordWakeSuccess(alarmId: String, date: String) {
        val streak = getStreak(alarmId)

        val newStreak = when {
            streak.lastWakeDate == null -> {
                // First wake-up
                streak.copy(
                    currentStreak = 1,
                    longestStreak = 1,
                    lastWakeDate = date,
                    totalWakeUps = streak.totalWakeUps + 1
                )
            }
            streak.lastWakeDate == date -> {
                // Already recorded today
                streak
            }
            isYesterday(streak.lastWakeDate, date) -> {
                // Consecutive day
                val newCurrent = streak.currentStreak + 1
                streak.copy(
                    currentStreak = newCurrent,
                    longestStreak = maxOf(newCurrent, streak.longestStreak),
                    lastWakeDate = date,
                    totalWakeUps = streak.totalWakeUps + 1
                )
            }
            else -> {
                // Streak broken
                streak.copy(
                    currentStreak = 1,
                    lastWakeDate = date,
                    totalWakeUps = streak.totalWakeUps + 1
                )
            }
        }

        saveStreak(newStreak)
    }

    /**
     * Record missed wake-up.
     * WHY: Break streak and update statistics.
     *
     * @param alarmId Alarm ID
     */
    fun recordWakeMiss(alarmId: String) {
        val streak = getStreak(alarmId)
        val newStreak = streak.copy(
            currentStreak = 0,
            totalMissed = streak.totalMissed + 1
        )
        saveStreak(newStreak)
    }

    /**
     * Save streak.
     * WHY: Persist updated streak data.
     */
    private fun saveStreak(streak: WakeStreak) {
        val json = gson.toJson(streak)
        prefs.edit()
            .putString("streak_${streak.alarmId}", json)
            .apply()
    }

    /**
     * Check if date2 is the day after date1.
     * WHY: Determine if streak continues.
     */
    private fun isYesterday(date1: String, date2: String): Boolean {
        return try {
            val d1 = dateFormat.parse(date1)
            val d2 = dateFormat.parse(date2)
            if (d1 == null || d2 == null) return false

            val diff = d2.time - d1.time
            val daysDiff = diff / (24 * 60 * 60 * 1000)
            daysDiff == 1L
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Confirmation Management ====================

    /**
     * Create wake confirmation when alarm triggers.
     * WHY: Track each alarm trigger for history visualization.
     *
     * @param alarmId Alarm ID
     * @param triggerTime When alarm fired
     * @param alarmName Alarm name for display
     * @return Created confirmation
     */
    fun createConfirmation(alarmId: String, triggerTime: Long, alarmName: String): WakeConfirmation {
        val date = dateFormat.format(Date(triggerTime))
        val confirmation = WakeConfirmation(
            alarmId = alarmId,
            triggerTime = triggerTime,
            date = date,
            alarmName = alarmName
        )
        saveConfirmation(confirmation)
        return confirmation
    }

    /**
     * Update confirmation with user response.
     * WHY: Record whether user actually woke up.
     *
     * @param confirmationId Confirmation ID
     * @param wokeOnTime User's response
     */
    fun updateConfirmation(confirmationId: String, wokeOnTime: Boolean) {
        val confirmation = getConfirmation(confirmationId) ?: return

        val updated = confirmation.copy(
            confirmationTime = System.currentTimeMillis(),
            wokeOnTime = wokeOnTime
        )

        saveConfirmation(updated)

        // Update streak based on response
        if (wokeOnTime) {
            recordWakeSuccess(confirmation.alarmId, confirmation.date)
        } else {
            recordWakeMiss(confirmation.alarmId)
        }
    }

    /**
     * Get confirmation by ID.
     */
    fun getConfirmation(id: String): WakeConfirmation? {
        val json = confirmPrefs.getString("confirm_$id", null) ?: return null
        return try {
            gson.fromJson(json, WakeConfirmation::class.java)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse confirmation", e)
            null
        }
    }

    /**
     * Get all confirmations for an alarm.
     * WHY: Display history grid.
     */
    fun getConfirmations(alarmId: String): List<WakeConfirmation> {
        val allKeys = confirmPrefs.all.keys
        val confirmations = mutableListOf<WakeConfirmation>()

        for (key in allKeys) {
            if (key.startsWith("confirm_")) {
                val json = confirmPrefs.getString(key, null)
                if (json != null) {
                    try {
                        val confirmation = gson.fromJson(json, WakeConfirmation::class.java)
                        if (confirmation.alarmId == alarmId) {
                            confirmations.add(confirmation)
                        }
                    } catch (e: Exception) {
                        // Skip corrupted entries
                    }
                }
            }
        }

        return confirmations.sortedByDescending { it.triggerTime }
    }

    /**
     * Get pending confirmations (not yet answered).
     * WHY: Show notification for unanswered confirmations.
     */
    fun getPendingConfirmations(): List<WakeConfirmation> {
        val allKeys = confirmPrefs.all.keys
        val pending = mutableListOf<WakeConfirmation>()

        for (key in allKeys) {
            if (key.startsWith("confirm_")) {
                val json = confirmPrefs.getString(key, null)
                if (json != null) {
                    try {
                        val confirmation = gson.fromJson(json, WakeConfirmation::class.java)
                        if (!confirmation.isConfirmed()) {
                            pending.add(confirmation)
                        }
                    } catch (e: Exception) {
                        // Skip corrupted entries
                    }
                }
            }
        }

        return pending.sortedByDescending { it.triggerTime }
    }

    /**
     * Save confirmation.
     */
    private fun saveConfirmation(confirmation: WakeConfirmation) {
        val json = gson.toJson(confirmation)
        confirmPrefs.edit()
            .putString("confirm_${confirmation.id}", json)
            .apply()
    }

    /**
     * Get today's date string.
     */
    fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * Clear all streaks and confirmations.
     * WHY: For testing or reset.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        confirmPrefs.edit().clear().apply()
    }

    companion object {
        private const val TAG = "StreakRepository"
    }
}