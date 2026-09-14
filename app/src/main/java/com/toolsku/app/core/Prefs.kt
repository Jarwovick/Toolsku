package com.toolsku.app.core

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Penyimpanan preferensi Toolsku.
 * Semua setting disimpan di satu file SharedPreferences.
 */
object Prefs {

    private const val KEY_DIMMER_LEVEL = "dimmer_level"
    private const val KEY_DIMMER_ENABLED = "dimmer_enabled"
    private const val KEY_EXCEPTION_LIST = "exception_list"
    private const val KEY_FIRST_LAUNCH = "first_launch"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    // === Dimmer ===
    var dimmerLevel: Int
        get() = prefs.getInt(KEY_DIMMER_LEVEL, 50)
        set(value) = prefs.edit().putInt(KEY_DIMMER_LEVEL, value).apply()

    var dimmerEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIMMER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DIMMER_ENABLED, value).apply()

    // === Exception List ===
    var exceptionList: Set<String>
        get() = prefs.getStringSet(KEY_EXCEPTION_LIST, defaultExceptionList()) ?: defaultExceptionList()
        set(value) = prefs.edit().putStringSet(KEY_EXCEPTION_LIST, value).apply()

    fun addException(pkg: String) {
        val current = exceptionList.toMutableSet()
        current.add(pkg)
        exceptionList = current
    }

    fun removeException(pkg: String) {
        val current = exceptionList.toMutableSet()
        current.remove(pkg)
        exceptionList = current
    }

    fun isException(pkg: String): Boolean = exceptionList.contains(pkg)

    // === First Launch ===
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    /**
     * Daftar pengecualian default.
     * App di list ini TIDAK di-force-stop, tapi tetap di-clear-cache.
     */
    private fun defaultExceptionList(): Set<String> = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "tw.nekomimi.nekogram",
        "com.twitter.android",
        "com.yahoo.mobile.client.android.mail",
        "com.google.android.gm",
        "com.microsoft.office.outlook"
    )
}
