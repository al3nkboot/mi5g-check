package com.kgurgul.cpuinfo.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLanguageTest {
    @Test
    fun appliesArabicUntilUserChooses() {
        assertTrue(AppLanguage.shouldApplyDefault(languageChosen = false))
        assertFalse(AppLanguage.shouldApplyDefault(languageChosen = true))
    }
}
