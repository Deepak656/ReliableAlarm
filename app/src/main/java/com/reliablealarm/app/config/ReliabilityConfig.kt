package com.reliablealarm.app.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration for all reliability mechanisms.
 * Each mechanism can be individually enabled/disabled.
 *
 * WHY: Feature flags allow users to customize behavior based on their device.
 * Some OEMs may conflict with certain mechanisms, so granular control is essential.
 *
 * DEFAULT: All mechanisms ENABLED for maximum reliability.
 *
 * Storage: SharedPreferences for instant access without database overhead.
 */
class ReliabilityConfig(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "reliability_config",
        Context.MODE_PRIVATE
    )

    /**
     * Dual scheduling redundancy.
     * Uses both AlarmManager and WorkManager for backup.
     * WHY: If one mechanism fails, the other triggers.
     */
    var dualScheduling: Boolean
        get() = prefs.getBoolean(KEY_DUAL_SCHEDULING, true)
        set(value) = prefs.edit().putBoolean(KEY_DUAL_SCHEDULING, value).apply()

    /**
     * Foreground service during alarm.
     * Prevents process killing by system.
     * WHY: Foreground services have highest priority, less likely to be killed.
     */
    var foregroundService: Boolean
        get() = prefs.getBoolean(KEY_FOREGROUND_SERVICE, true)
        set(value) = prefs.edit().putBoolean(KEY_FOREGROUND_SERVICE, value).apply()

    /**
     * Bluetooth audio routing override.
     * Forces speaker output even if Bluetooth connected.
     * WHY: User may not hear alarm through Bluetooth headphones if not worn.
     */
    var bluetoothOverride: Boolean
        get() = prefs.getBoolean(KEY_BLUETOOTH_OVERRIDE, true)
        set(value) = prefs.edit().putBoolean(KEY_BLUETOOTH_OVERRIDE, value).apply()

    /**
     * Progressive volume escalation.
     * Increases volume every 10 seconds from 70% to 100%.
     * WHY: Gentler wake-up with safety net if user doesn't respond.
     */
    var volumeEscalation: Boolean
        get() = prefs.getBoolean(KEY_VOLUME_ESCALATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VOLUME_ESCALATION, value).apply()

    /**
     * Auto re-ring protection.
     * If dismissed within 10 seconds, re-trigger after 60 seconds.
     * WHY: Prevents accidental dismissal while asleep.
     */
    var autoReRing: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RE_RING, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RE_RING, value).apply()

    /**
     * Alarm re-registration watchdog.
     * Verifies and re-registers alarms on every app launch.
     * WHY: Protects against OS updates, OEM battery optimization clearing alarms.
     */
    var alarmWatchdog: Boolean
        get() = prefs.getBoolean(KEY_ALARM_WATCHDOG, true)
        set(value) = prefs.edit().putBoolean(KEY_ALARM_WATCHDOG, value).apply()

    /**
     * Boot persistence.
     * Re-registers all alarms after device reboot.
     * WHY: AlarmManager alarms are cleared on reboot.
     */
    var bootPersistence: Boolean
        get() = prefs.getBoolean(KEY_BOOT_PERSISTENCE, true)
        set(value) = prefs.edit().putBoolean(KEY_BOOT_PERSISTENCE, value).apply()

    /**
     * Low-memory survival.
     * Handles onTrimMemory/onLowMemory to restart service if needed.
     * WHY: System may kill service under memory pressure.
     */
    var lowMemorySurvival: Boolean
        get() = prefs.getBoolean(KEY_LOW_MEMORY_SURVIVAL, true)
        set(value) = prefs.edit().putBoolean(KEY_LOW_MEMORY_SURVIVAL, value).apply()

    /**
     * OEM battery killer detection.
     * Shows device-specific guidance for battery optimization.
     * WHY: OEMs like Xiaomi, Samsung aggressively kill background tasks.
     */
    var oemBatteryDetection: Boolean
        get() = prefs.getBoolean(KEY_OEM_BATTERY_DETECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_OEM_BATTERY_DETECTION, value).apply()

    /**
     * Reset all settings to default (all enabled).
     * WHY: Easy recovery if user breaks configuration.
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /**
     * Get all settings as map for display.
     * WHY: UI can iterate and show all options.
     */
    fun getAllSettings(): Map<String, Boolean> {
        return mapOf(
            "Dual Scheduling" to dualScheduling,
            "Foreground Service" to foregroundService,
            "Bluetooth Override" to bluetoothOverride,
            "Volume Escalation" to volumeEscalation,
            "Auto Re-Ring" to autoReRing,
            "Alarm Watchdog" to alarmWatchdog,
            "Boot Persistence" to bootPersistence,
            "Low Memory Survival" to lowMemorySurvival,
            "OEM Battery Detection" to oemBatteryDetection
        )
    }

    companion object {
        private const val KEY_DUAL_SCHEDULING = "dual_scheduling"
        private const val KEY_FOREGROUND_SERVICE = "foreground_service"
        private const val KEY_BLUETOOTH_OVERRIDE = "bluetooth_override"
        private const val KEY_VOLUME_ESCALATION = "volume_escalation"
        private const val KEY_AUTO_RE_RING = "auto_re_ring"
        private const val KEY_ALARM_WATCHDOG = "alarm_watchdog"
        private const val KEY_BOOT_PERSISTENCE = "boot_persistence"
        private const val KEY_LOW_MEMORY_SURVIVAL = "low_memory_survival"
        private const val KEY_OEM_BATTERY_DETECTION = "oem_battery_detection"
    }
}