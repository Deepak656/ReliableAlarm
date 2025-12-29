package com.reliablealarm.app.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Detects and handles OEM battery optimization that kills alarms.
 *
 * WHY: Many OEMs (Xiaomi, Samsung, Oppo, Vivo, OnePlus) aggressively
 * kill background apps to save battery, even if user wants alarms.
 *
 * This class:
 * - Detects specific OEM manufacturers
 * - Checks if app is battery optimized
 * - Provides device-specific guidance to user
 * - Attempts to request battery optimization exemption
 *
 * NO external web links - all guidance is hardcoded and offline.
 */
class DevicePowerManager(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    /**
     * Check if app is being battery optimized (restricted).
     *
     * WHY: Battery optimization prevents alarms from firing reliably.
     * Android 6+ added Doze mode and App Standby.
     */
    fun isAppBatteryOptimized(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = context.packageName
            !powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            false // Pre-Android 6 doesn't have battery optimization
        }
    }

    /**
     * Request battery optimization exemption.
     *
     * WHY: Apps can request to be excluded from Doze/App Standby.
     * This is essential for alarm apps.
     *
     * Opens system settings for user to manually allow.
     */
    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Log.d(TAG, "Opened battery optimization exemption request")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open battery optimization settings", e)
                // Fallback: open general battery settings
                openBatterySettings()
            }
        }
    }

    /**
     * Open general battery optimization settings.
     * Fallback if direct request fails.
     */
    fun openBatterySettings() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened battery optimization settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery settings", e)
        }
    }

    /**
     * Detect device manufacturer.
     * WHY: Different OEMs have different battery optimization behaviors.
     */
    fun getManufacturer(): String {
        return Build.MANUFACTURER.lowercase()
    }

    /**
     * Check if device is from aggressive battery-killing OEM.
     *
     * WHY: These OEMs are known to kill apps despite battery exemption.
     * Need to show additional guidance.
     */
    fun isAggressiveOEM(): Boolean {
        val manufacturer = getManufacturer()
        return manufacturer in listOf(
            "xiaomi", "redmi", "poco",
            "oppo", "realme", "oneplus",
            "vivo", "huawei", "honor",
            "asus", "samsung"
        )
    }

    /**
     * Get device-specific battery optimization guidance.
     *
     * WHY: Each OEM has different settings UI.
     * Provide step-by-step instructions for user's specific device.
     *
     * @return Human-readable guidance text
     */
    fun getBatteryOptimizationGuidance(): String {
        val manufacturer = getManufacturer()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                """
                Xiaomi/Redmi/Poco Battery Optimization:
                
                1. Go to Settings > Battery & Performance
                2. Tap "App battery saver"
                3. Find "Reliable Alarm" and tap it
                4. Select "No restrictions"
                
                Additional steps:
                5. Go to Settings > Apps > Manage apps
                6. Find "Reliable Alarm"
                7. Enable "Autostart"
                8. Under "Battery saver", select "No restrictions"
                9. Enable "Display pop-up windows while running in background"
                
                These steps prevent MIUI from killing the alarm app.
                """.trimIndent()
            }

            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                """
                Oppo/Realme Battery Optimization:
                
                1. Go to Settings > Battery > App Battery Management
                2. Disable "App Quick Freeze"
                3. Find "Reliable Alarm" in the list
                4. Set to "Don't optimize"
                
                Additional steps:
                5. Go to Settings > Privacy > Permission Manager
                6. Select "Autostart"
                7. Enable autostart for "Reliable Alarm"
                
                ColorOS aggressively kills background apps.
                These settings ensure alarms work reliably.
                """.trimIndent()
            }

            manufacturer.contains("vivo") -> {
                """
                Vivo Battery Optimization:
                
                1. Go to Settings > Battery > Background Energy Consumption Management
                2. Find "Reliable Alarm"
                3. Allow "High background power consumption"
                
                Additional steps:
                4. Go to Settings > More Settings > Permission Manager
                5. Select "Autostart"
                6. Enable for "Reliable Alarm"
                
                Funtouch OS kills apps to save battery.
                These settings prevent alarm interruption.
                """.trimIndent()
            }

            manufacturer.contains("oneplus") -> {
                """
                OnePlus Battery Optimization:
                
                1. Go to Settings > Battery > Battery Optimization
                2. Tap "All apps"
                3. Find "Reliable Alarm"
                4. Select "Don't optimize"
                
                Additional steps (OxygenOS 11+):
                5. Go to Settings > Apps > Reliable Alarm
                6. Under "Battery", select "Don't optimize"
                7. Enable "Allow background activity"
                
                OnePlus has improved but may still kill alarms.
                """.trimIndent()
            }

            manufacturer.contains("samsung") -> {
                """
                Samsung Battery Optimization:
                
                1. Go to Settings > Apps > Reliable Alarm
                2. Tap "Battery"
                3. Set "Battery usage" to "Unrestricted"
                4. Disable "Put app to sleep"
                
                Additional steps:
                5. Go to Settings > Device care > Battery
                6. Tap "Background usage limits"
                7. Remove "Reliable Alarm" from sleeping/deep sleeping apps
                
                Samsung's One UI can be aggressive with battery saving.
                """.trimIndent()
            }

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                """
                Huawei/Honor Battery Optimization:
                
                1. Go to Settings > Battery > App Launch
                2. Find "Reliable Alarm"
                3. Disable "Manage automatically"
                4. Enable all three options:
                   - Auto-launch
                   - Secondary launch
                   - Run in background
                
                EMUI is extremely aggressive with battery optimization.
                These settings are critical for alarm reliability.
                """.trimIndent()
            }

            manufacturer.contains("asus") -> {
                """
                Asus Battery Optimization:
                
                1. Go to Mobile Manager > PowerMaster
                2. Tap "Auto-start Manager"
                3. Enable "Reliable Alarm"
                
                Additional steps:
                4. Go to Settings > Apps > Reliable Alarm
                5. Under "Battery", select "No restrictions"
                
                Asus ZenUI can prevent apps from running.
                """.trimIndent()
            }

            else -> {
                """
                General Battery Optimization:
                
                1. Go to Settings > Battery > Battery Optimization
                2. Select "All apps"
                3. Find "Reliable Alarm"
                4. Select "Don't optimize" or "Not optimized"
                
                This ensures Android doesn't restrict the alarm app
                when the device is in Doze mode or App Standby.
                
                Note: Your device manufacturer is: ${Build.MANUFACTURER}
                If alarms still don't fire reliably, search online for:
                "${Build.MANUFACTURER} battery optimization disable"
                """.trimIndent()
            }
        }
    }

    /**
     * Get short warning message for UI.
     */
    fun getBatteryWarningMessage(): String {
        return if (isAppBatteryOptimized()) {
            if (isAggressiveOEM()) {
                "⚠️ Battery optimization detected on ${Build.MANUFACTURER} device. " +
                        "This WILL prevent alarms from firing. Tap to fix now."
            } else {
                "⚠️ Battery optimization enabled. This may prevent alarms from firing. Tap to disable."
            }
        } else {
            "✓ Battery optimization disabled (optimal)"
        }
    }

    companion object {
        private const val TAG = "DevicePowerManager"
    }
}