package com.kgurgul.cpuinfo.utils

/** App UI language: "ar", "en" or "" (follow system). Arabic is the default. */
expect object AppLanguage {
    fun current(): String

    fun set(tag: String)
}
