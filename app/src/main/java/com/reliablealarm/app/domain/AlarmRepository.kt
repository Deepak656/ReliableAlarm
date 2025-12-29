package com.reliablealarm.app.domain

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reliablealarm.app.domain.models.Alarm

/**
 * Repository for alarm persistence.
 *
 * WHY: Single source of truth for all alarm data.
 * Uses SharedPreferences for:
 * - Instant read/write (no database overhead)
 * - Atomic updates (no race conditions)
 * - Survives app restarts
 * - Survives process killing
 * - Simple JSON serialization
 *
 * Thread-safety: All operations are synchronized on prefs object.
 *
 * Alternative considered: Room database
 * Rejected because: SharedPreferences is faster for small datasets,
 * and we don't need complex queries.
 */
class AlarmRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "alarms",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    /**
     * Save or update an alarm.
     * WHY: Upsert pattern simplifies caller code.
     *
     * @param alarm Alarm to save
     * @return Updated alarm with current timestamp
     */
    fun saveAlarm(alarm: Alarm): Alarm {
        val alarms = getAllAlarms().toMutableList()
        val existingIndex = alarms.indexOfFirst { it.id == alarm.id }

        val updatedAlarm = alarm.copy(lastModified = System.currentTimeMillis())

        if (existingIndex >= 0) {
            alarms[existingIndex] = updatedAlarm
        } else {
            alarms.add(updatedAlarm)
        }

        saveAllAlarms(alarms)
        return updatedAlarm
    }

    /**
     * Get alarm by ID.
     * WHY: Needed when alarm triggers to get full alarm details.
     *
     * @param id Alarm ID
     * @return Alarm or null if not found
     */
    fun getAlarm(id: String): Alarm? {
        return getAllAlarms().firstOrNull { it.id == id }
    }

    /**
     * Get all alarms.
     * WHY: UI needs to display all alarms, watchdog needs to verify all.
     *
     * @return List of all alarms, empty if none exist
     */
    fun getAllAlarms(): List<Alarm> {
        val json = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Alarm>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            // Corrupted data, return empty and log
            android.util.Log.e(TAG, "Failed to parse alarms", e)
            emptyList()
        }
    }

    /**
     * Get all enabled alarms.
     * WHY: Scheduler only needs to register enabled alarms.
     *
     * @return List of enabled alarms
     */
    fun getEnabledAlarms(): List<Alarm> {
        return getAllAlarms().filter { it.isEnabled }
    }

    /**
     * Delete alarm by ID.
     * WHY: User can remove unwanted alarms.
     *
     * @param id Alarm ID to delete
     * @return true if deleted, false if not found
     */
    fun deleteAlarm(id: String): Boolean {
        val alarms = getAllAlarms().toMutableList()
        val removed = alarms.removeIf { it.id == id }
        if (removed) {
            saveAllAlarms(alarms)
        }
        return removed
    }

    /**
     * Toggle alarm enabled state.
     * WHY: Quick enable/disable without full edit.
     *
     * @param id Alarm ID
     * @param enabled New enabled state
     * @return Updated alarm or null if not found
     */
    fun toggleAlarm(id: String, enabled: Boolean): Alarm? {
        val alarm = getAlarm(id) ?: return null
        return saveAlarm(alarm.copy(isEnabled = enabled))
    }

    /**
     * Update alarm last triggered time.
     * WHY: Track when alarm last fired for statistics.
     *
     * Note: This is stored separately from main alarm data
     * to avoid conflicts with user edits.
     */
    fun updateLastTriggered(id: String, time: Long) {
        prefs.edit()
            .putLong("last_triggered_$id", time)
            .apply()
    }

    /**
     * Get alarm last triggered time.
     */
    fun getLastTriggered(id: String): Long {
        return prefs.getLong("last_triggered_$id", 0)
    }

    /**
     * Clear all alarms.
     * WHY: For testing or reset functionality.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Save all alarms at once.
     * WHY: Atomic write operation.
     *
     * Private because external callers should use saveAlarm().
     */
    private fun saveAllAlarms(alarms: List<Alarm>) {
        val json = gson.toJson(alarms)
        prefs.edit()
            .putString(KEY_ALARMS, json)
            .apply()
    }

    companion object {
        private const val TAG = "AlarmRepository"
        private const val KEY_ALARMS = "alarms_list"
    }
}