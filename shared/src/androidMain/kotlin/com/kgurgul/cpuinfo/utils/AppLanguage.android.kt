package com.kgurgul.cpuinfo.utils

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

actual object AppLanguage {

    private const val PREFS = "app_language"
    private const val KEY_LANGUAGE_CHOSEN = "language_chosen"
    const val DEFAULT_LANGUAGE = "ar"

    private lateinit var appContext: Context

    /** Pure decision: Arabic is forced until the user explicitly picks a language. */
    fun shouldApplyDefault(languageChosen: Boolean): Boolean = !languageChosen

    /** Call from Application.onCreate. */
    fun init(context: Context) {
        appContext = context.applicationContext
        val chosen = prefs().getBoolean(KEY_LANGUAGE_CHOSEN, false)
        if (shouldApplyDefault(chosen)) apply(DEFAULT_LANGUAGE)
    }

    actual fun current(): String =
        if (!::appContext.isInitialized) ""
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }.substringBefore('-').substringBefore(',')

    actual fun set(tag: String) {
        prefs().edit { putBoolean(KEY_LANGUAGE_CHOSEN, true) }
        apply(tag)
    }

    // AppCompatDelegate.setApplicationLocales on API 33+ only reaches LocaleManager through an
    // already-created AppCompat activity, so from Application.onCreate it was a no-op. Call the
    // framework directly there; below 33 AppCompat stores it (autoStoreLocales) and applies it.
    private fun apply(tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
