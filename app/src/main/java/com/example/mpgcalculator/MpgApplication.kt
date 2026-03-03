package com.eddiec.mpgcalculator

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MpgApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        applyTheme(prefs.getString(SettingsActivity.KEY_THEME, "SYSTEM") ?: "SYSTEM")
    }

    companion object {
        fun applyTheme(theme: String) {
            AppCompatDelegate.setDefaultNightMode(
                when (theme) {
                    "LIGHT" -> AppCompatDelegate.MODE_NIGHT_NO
                    "DARK"  -> AppCompatDelegate.MODE_NIGHT_YES
                    else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }
}
