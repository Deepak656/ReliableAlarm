package com.reliablealarm.app.domain.models

import com.reliablealarm.app.waketasks.TaskConfig
import java.util.*

/**
 * Core domain model representing a single alarm.
 *
 * WHY: Immutable data class ensures thread-safety and predictable behavior.
 * All alarm properties are contained here for single source of truth.
 *
 * Design decisions:
 * - UUID for unique identification (survives database migrations)
 * - Separate hour/minute for precise scheduling
 * - Boolean set for repeat days (efficient, clear)
 * - Enabled state for soft delete (preserve history)
 * - Custom message for user context
 */
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Alarm",
    val message: String = "Wake Up!",
    val hour: Int = 8, // 24-hour format: 0-23
    val minute: Int = 0, // 0-59
    val repeatMonday: Boolean = false,
    val repeatTuesday: Boolean = false,
    val repeatWednesday: Boolean = false,
    val repeatThursday: Boolean = false,
    val repeatFriday: Boolean = false,
    val repeatSaturday: Boolean = false,
    val repeatSunday: Boolean = false,
    val isEnabled: Boolean = true,
    // ⭐ NEW ⭐
    val wakeTasks: List<String> = emptyList(),

    // ⭐ NEW ⭐ keyed by WakeTaskType.key
    val taskSettings: Map<String, TaskConfig> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
) {

    /**
     * Check if alarm repeats on any day.
     * WHY: Determines scheduling strategy (one-time vs recurring).
     */
    fun isRepeating(): Boolean {
        return repeatMonday || repeatTuesday || repeatWednesday ||
                repeatThursday || repeatFriday || repeatSaturday || repeatSunday
    }

    /**
     * Check if alarm should trigger on a specific day of week.
     * @param dayOfWeek Calendar.SUNDAY through Calendar.SATURDAY
     * WHY: Used by scheduler to determine next trigger time.
     */
    fun isActiveOnDay(dayOfWeek: Int): Boolean {
        return when (dayOfWeek) {
            Calendar.MONDAY -> repeatMonday
            Calendar.TUESDAY -> repeatTuesday
            Calendar.WEDNESDAY -> repeatWednesday
            Calendar.THURSDAY -> repeatThursday
            Calendar.FRIDAY -> repeatFriday
            Calendar.SATURDAY -> repeatSaturday
            Calendar.SUNDAY -> repeatSunday
            else -> false
        }
    }

    /**
     * Get list of active days for display.
     * WHY: UI needs to show which days alarm is active.
     */
    fun getActiveDays(): List<Int> {
        val days = mutableListOf<Int>()
        if (repeatSunday) days.add(Calendar.SUNDAY)
        if (repeatMonday) days.add(Calendar.MONDAY)
        if (repeatTuesday) days.add(Calendar.TUESDAY)
        if (repeatWednesday) days.add(Calendar.WEDNESDAY)
        if (repeatThursday) days.add(Calendar.THURSDAY)
        if (repeatFriday) days.add(Calendar.FRIDAY)
        if (repeatSaturday) days.add(Calendar.SATURDAY)
        return days
    }

    /**
     * Get formatted time string for display.
     * WHY: Consistent time formatting across UI.
     */
    fun getFormattedTime(): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", displayHour, minute, amPm)
    }

    /**
     * Get formatted days string for display.
     * WHY: Show repeat pattern in compact form.
     */
    fun getFormattedDays(): String {
        if (!isRepeating()) return "Once"

        val allDays = repeatMonday && repeatTuesday && repeatWednesday &&
                repeatThursday && repeatFriday && repeatSaturday && repeatSunday
        if (allDays) return "Every day"

        val weekdays = repeatMonday && repeatTuesday && repeatWednesday &&
                repeatThursday && repeatFriday && !repeatSaturday && !repeatSunday
        if (weekdays) return "Weekdays"

        val weekend = !repeatMonday && !repeatTuesday && !repeatWednesday &&
                !repeatThursday && !repeatFriday && repeatSaturday && repeatSunday
        if (weekend) return "Weekends"

        // Show abbreviated days
        val days = mutableListOf<String>()
        if (repeatSunday) days.add("Sun")
        if (repeatMonday) days.add("Mon")
        if (repeatTuesday) days.add("Tue")
        if (repeatWednesday) days.add("Wed")
        if (repeatThursday) days.add("Thu")
        if (repeatFriday) days.add("Fri")
        if (repeatSaturday) days.add("Sat")

        return days.joinToString(", ")
    }

    /**
     * Calculate next trigger time in milliseconds.
     * WHY: AlarmManager needs exact time for scheduling.
     *
     * Logic:
     * - If one-time alarm: schedule for today if time hasn't passed, else tomorrow
     * - If repeating: find next active day, may be today or future
     */
    fun getNextTriggerTime(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()

        if (!isRepeating()) {
            // One-time alarm
            if (calendar.timeInMillis <= now) {
                // Time has passed today, schedule for tomorrow
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        // Repeating alarm - find next active day
        // Check today first if time hasn't passed
        if (calendar.timeInMillis > now && isActiveOnDay(calendar.get(Calendar.DAY_OF_WEEK))) {
            return calendar.timeInMillis
        }

        // Check next 7 days
        for (i in 1..7) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            if (isActiveOnDay(calendar.get(Calendar.DAY_OF_WEEK))) {
                return calendar.timeInMillis
            }
        }

        // Should never reach here if at least one day is selected
        // Fallback to tomorrow
        return System.currentTimeMillis() + 24 * 60 * 60 * 1000
    }
}