package com.kgurgul.cpuinfo.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

actual object AppLanguage {

    private const val PREFS = "app_language"
    private const val KEY_INITIALIZED = "initialized"

    /** Call once from Application.onCreate: applies Arabic on first launch. */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            prefs.edit { putBoolean(KEY_INITIALIZED, true) }
            if (AppCompatDelegate.getApplicationLocales().isEmpty) set("ar")
        }
    }

    actual fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()

    actual fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
