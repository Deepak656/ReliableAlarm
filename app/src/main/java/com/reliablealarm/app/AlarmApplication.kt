package com.reliablealarm.app

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Configuration
import com.reliablealarm.app.reliability.AlarmWatchdog

class AlarmApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext

        safeRunWatchdog()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun safeRunWatchdog() {
        try {
            Handler(Looper.getMainLooper()).post {
                AlarmWatchdog.verify(this)
            }
        } catch (e: Exception) {
            Log.e("AlarmApp", "Watchdog failed", e)
        }
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
