package com.reliablealarm.app.domain.models

/**
 * Model representing a single wake confirmation event.
 *
 * WHY: Historical tracking allows visualization in grid view.
 * Each confirmation represents one alarm trigger and user response.
 *
 * Design:
 * - alarmId links to specific alarm
 * - triggerTime is when alarm fired
 * - confirmationTime is when user responded
 * - wokeOnTime is user's self-reported status
 * - date for grid visualization
 */
data class WakeConfirmation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val alarmId: String,
    val triggerTime: Long,
    val confirmationTime: Long? = null,
    val wokeOnTime: Boolean? = null,
    val date: String, // Format: "yyyy-MM-dd"
    val alarmName: String = ""
) {

    /**
     * Check if user has responded to confirmation.
     * WHY: Determines if notification should still be shown.
     */
    fun isConfirmed(): Boolean {
        return confirmationTime != null && wokeOnTime != null
    }

    /**
     * Get minutes between trigger and confirmation.
     * WHY: Shows response latency.
     */
    fun getResponseMinutes(): Int? {
        if (triggerTime == 0L || confirmationTime == null) return null
        return ((confirmationTime - triggerTime) / (60 * 1000)).toInt()
    }
}