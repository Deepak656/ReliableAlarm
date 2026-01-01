package com.reliablealarm.app.domain

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.reliablealarm.app.domain.models.Alarm
import com.reliablealarm.app.waketasks.TaskConfig

class AlarmRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "alarms",
        Context.MODE_PRIVATE
    )

    /**
     * ---- POLYMORPHIC SUPPORT FOR TaskConfig ----
     * Gson can't deserialize sealed classes automatically,
     * so we manually write a type discriminator.
     */

    private val taskConfigSerializer = JsonSerializer<TaskConfig> { src, _, context ->
        val obj = context.serialize(src).asJsonObject

        when (src) {
            is TaskConfig.MathConfig -> obj.addProperty("type", "math")
            is TaskConfig.StepConfig -> obj.addProperty("type", "steps")
            is TaskConfig.ShakeConfig -> obj.addProperty("type", "shake")
            is TaskConfig.QrConfig -> obj.addProperty("type", "qr")
            is TaskConfig.TypingConfig -> obj.addProperty("type", "typing")
            is TaskConfig.TapTaskConfig -> obj.addProperty("type", "tap")
            is TaskConfig.ColorBallsConfig -> obj.addProperty("type", "colorballs")
        }

        obj
    }

    private val taskConfigDeserializer = JsonDeserializer<TaskConfig> { json, _, context ->
        val obj = json.asJsonObject
        val type = obj["type"]?.asString

        return@JsonDeserializer when (type) {
            "math" -> context.deserialize(obj, TaskConfig.MathConfig::class.java)
            "steps" -> context.deserialize(obj, TaskConfig.StepConfig::class.java)
            "shake" -> context.deserialize(obj, TaskConfig.ShakeConfig::class.java)
            "qr" -> context.deserialize(obj, TaskConfig.QrConfig::class.java)
            "typing" -> context.deserialize(obj, TaskConfig.TypingConfig::class.java)
            "tap" -> context.deserialize(obj, TaskConfig.TapTaskConfig::class.java)
            "colorballs" -> context.deserialize(obj, TaskConfig.ColorBallsConfig::class.java)
            else -> null
        }
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(TaskConfig::class.java, taskConfigSerializer)
        .registerTypeAdapter(TaskConfig::class.java, taskConfigDeserializer)
        .create()

    /**
     * Save or update an alarm.
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
     */
    fun getAlarm(id: String): Alarm? {
        return getAllAlarms().firstOrNull { it.id == id }
    }

    /**
     * Get all alarms.
     */
    fun getAllAlarms(): List<Alarm> {
        val json = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Alarm>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse alarms", e)
            emptyList()
        }
    }

    /**
     * Get enabled alarms.
     */
    fun getEnabledAlarms(): List<Alarm> =
        getAllAlarms().filter { it.isEnabled }

    /**
     * Delete alarm.
     */
    fun deleteAlarm(id: String): Boolean {
        val alarms = getAllAlarms().toMutableList()
        val removed = alarms.removeIf { it.id == id }
        if (removed) saveAllAlarms(alarms)
        return removed
    }

    /**
     * Toggle enable.
     */
    fun toggleAlarm(id: String, enabled: Boolean): Alarm? {
        val alarm = getAlarm(id) ?: return null
        return saveAlarm(alarm.copy(isEnabled = enabled))
    }

    /**
     * Last triggered tracking.
     */
    fun updateLastTriggered(id: String, time: Long) {
        prefs.edit()
            .putLong("last_triggered_$id", time)
            .apply()
    }

    fun getLastTriggered(id: String): Long =
        prefs.getLong("last_triggered_$id", 0)

    /**
     * Clear everything.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Internal save list.
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
