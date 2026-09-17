package com.toolsku.app.core

/**
 * Kategorisasi aplikasi sistem.
 *
 * - DANGEROUS: tidak boleh di-kill/clear sama sekali
 * - SAFE: boleh di-kill (galeri, kamera, browser, dll)
 */
object SystemApps {

    /**
     * System apps yang TIDAK BOLEH di-kill / clear cache.
     * Bisa menyebabkan sistem crash.
     */
    private val DANGEROUS_PACKAGES = setOf(
        // System UI & Settings
        "com.android.systemui",
        "com.coloros.systemui",
        "com.android.settings",
        "com.coloros.settings",
        "com.android.shell",
        "com.android.providers.settings",
        "com.android.providers.media",
        "com.android.providers.contacts",
        "com.android.providers.telephony",
        "com.android.providers.calendar",
        "com.android.providers.downloads",
        "com.android.providers.userdictionary",
        "com.android.providers.blockednumber",
        "com.android.providers.media.module",
        "com.android.providers.contacts.module",

        // Telephony & Phone
        "com.android.server.telecom",
        "com.android.dialer",
        "com.android.phone",
        "com.android.mms",
        "com.android.messaging",
        "com.coloros.phonemanager",
        "com.coloros.phone",
        "com.coloros.contacts",
        "com.android.contacts",

        // System Services
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.se",
        "com.android.keychain",
        "com.android.certinstaller",
        "com.android.packageinstaller",
        "com.android.permissioncontroller",

        // Google Services
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.gsf.login",

        // App kita
        "com.toolsku.app",
        "com.toolsku.app.debug"
    )

    /**
     * Prefix package yang TIDAK BOLEH di-kill.
     */
    private val DANGEROUS_PREFIXES = listOf(
        "com.android.launcher",
        "com.coloros.launcher",
        "com.oppo.launcher",
        "com.realme.launcher",
        "com.android.inputmethod",
        "com.google.android.inputmethod",
        "com.baidu.input",
        "com.sohu.inputmethod",
        "com.coloros.alarmclock",
        "com.android.internal",
        "com.android.server",
        "com.android.providers"
    )

    /**
     * System apps yang AMAN di-kill (untuk Killer).
     */
    private val SAFE_SYSTEM_PACKAGES = setOf(
        // Galeri
        "com.coloros.gallery3d",
        "com.oppo.gallery3d",
        "com.android.gallery3d",
        "com.realme.gallery3d",

        // Kamera
        "com.oppo.camera",
        "com.coloros.camera",
        "com.android.camera",
        "com.android.camera2",

        // Browser bawaan
        "com.heytap.browser",
        "com.coloros.browser",
        "com.android.browser",

        // Music & Video
        "com.oppo.music",
        "com.heytap.music",
        "com.coloros.music",
        "com.coloros.video",
        "com.oppo.video",

        // Tools
        "com.coloros.calculator",
        "com.coloros.weather",
        "com.coloros.note",
        "com.coloros.feedback",
        "com.coloros.cloud",
        "com.coloros.oshare",
        "com.coloros.safecenter",

        // File Manager
        "com.coloros.filemanager",
        "com.oppo.filemanager",

        // Calendar
        "com.coloros.calendar",
        "com.android.calendar",

        // Email
        "com.android.email",
        "com.coloros.email",

        // Theme & Store
        "com.heytap.themestore",
        "com.coloros.themestore"
    )

    private val SAFE_SYSTEM_PREFIXES = listOf(
        "com.coloros.gallery",
        "com.oppo.gallery",
        "com.realme.gallery",
        "com.coloros.camera",
        "com.oppo.camera",
        "com.coloros.video",
        "com.coloros.music",
        "com.coloros.calculator",
        "com.coloros.weather",
        "com.coloros.note",
        "com.coloros.filemanager"
    )

    /**
     * Cek apakah package adalah system app.
     */
    fun isSystemApp(pkg: String): Boolean {
        return pkg.startsWith("com.android.") ||
               pkg.startsWith("com.coloros.") ||
               pkg.startsWith("com.oppo.") ||
               pkg.startsWith("com.realme.") ||
               pkg.startsWith("com.heytap.") ||
               pkg.startsWith("com.google.android.")
    }

    /**
     * Cek apakah package adalah system app yang TIDAK BOLEH di-kill.
     */
    fun isDangerousSystemApp(pkg: String): Boolean {
        if (DANGEROUS_PACKAGES.contains(pkg)) return true
        return DANGEROUS_PREFIXES.any { pkg.startsWith(it) }
    }

    /**
     * Cek apakah package adalah system app yang AMAN di-kill.
     */
    fun isSafeSystemApp(pkg: String): Boolean {
        if (SAFE_SYSTEM_PACKAGES.contains(pkg)) return true
        return SAFE_SYSTEM_PREFIXES.any { pkg.startsWith(it) }
    }

    /**
     * Cek apakah package adalah keyboard.
     */
    fun isKeyboard(pkg: String): Boolean {
        return pkg.contains("inputmethod") || pkg.contains(".input")
    }
}
