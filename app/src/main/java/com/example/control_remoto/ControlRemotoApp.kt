package com.example.control_remoto

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ControlRemotoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val savedMode = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedMode)
    }

    companion object {
        const val PREFS = "settings"
        const val KEY_NIGHT_MODE = "night_mode"
    }
}
