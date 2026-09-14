package com.toolsku.app.core

/**
 * Daftar aplikasi sistem yang TIDAK boleh disentuh sama sekali
 * (tidak di-force-stop, tidak di-clear-cache).
 *
 * Daftar ini hardcoded dan tidak bisa diubah user.
 */
object SystemApps {

    private val SYSTEM_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.shell",
        "com.android.providers.settings",
        "com.android.providers.media",
        "com.android.providers.contacts",
        "com.android.providers.telephony",
        "com.android.providers.calendar",
        "com.android.providers.downloads",
        "com.android.server.telecom",
        "com.android.dialer",
        "com.android.phone",
        "com.android.deskclock",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.toolsku.app",
        "com.toolsku.app.debug"
    )

    private val SYSTEM_PREFIXES = listOf(
        "com.android.launcher",
        "com.coloros.launcher",
        "com.oppo.launcher",
        "com.android.inputmethod",
        "com.google.android.inputmethod",
        "com.baidu.input",
        "com.sohu.inputmethod",
        "com.coloros.alarmclock",
        "com.android.internal"
    )

    /**
     * Cek apakah package termasuk aplikasi sistem.
     */
    fun isSystemApp(pkg: String): Boolean {
        if (SYSTEM_PACKAGES.contains(pkg)) return true
        return SYSTEM_PREFIXES.any { pkg.startsWith(it) }
    }

    /**
     * Cek apakah package termasuk keyboard yang aktif.
     * (versi lebih sederhana: cek prefix inputmethod)
     */
    fun isKeyboard(pkg: String): Boolean {
        return pkg.contains("inputmethod") || pkg.contains(".input")
    }
}
