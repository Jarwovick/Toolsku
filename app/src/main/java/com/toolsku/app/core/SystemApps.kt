package com.toolsku.app.core

object SystemApps {

    private val DANGEROUS_PACKAGES = setOf(
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
        "com.android.server.telecom",
        "com.android.dialer",
        "com.android.phone",
        "com.android.mms",
        "com.android.messaging",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.toolsku.app",
        "com.toolsku.app.debug"
    )

    private val DANGEROUS_PREFIXES = listOf(
        "com.android.launcher",
        "com.coloros.launcher",
        "com.oppo.launcher",
        "com.realme.launcher",
        "com.android.inputmethod",
        "com.google.android.inputmethod",
        "com.coloros.alarmclock",
        "com.android.server",
        "com.android.providers"
    )

    private val SAFE_SYSTEM_PACKAGES = setOf(
        "com.coloros.gallery3d",
        "com.oppo.gallery3d",
        "com.realme.gallery3d",
        "com.oppo.camera",
        "com.coloros.camera",
        "com.heytap.browser",
        "com.coloros.browser",
        "com.coloros.filemanager",
        "com.coloros.calculator",
        "com.coloros.weather",
        "com.coloros.note"
    )

    private val SAFE_SYSTEM_PREFIXES = listOf(
        "com.coloros.gallery",
        "com.oppo.gallery",
        "com.coloros.camera",
        "com.oppo.camera",
        "com.coloros.video",
        "com.coloros.music",
        "com.coloros.calculator",
        "com.coloros.weather",
        "com.coloros.note",
        "com.coloros.filemanager"
    )

    fun isSystemApp(pkg: String): Boolean {
        return pkg.startsWith("com.android.") ||
               pkg.startsWith("com.coloros.") ||
               pkg.startsWith("com.oppo.") ||
               pkg.startsWith("com.realme.") ||
               pkg.startsWith("com.heytap.") ||
               pkg.startsWith("com.google.android.")
    }

    fun isDangerousSystemApp(pkg: String): Boolean {
        if (DANGEROUS_PACKAGES.contains(pkg)) return true
        return DANGEROUS_PREFIXES.any { pkg.startsWith(it) }
    }

    fun isSafeSystemApp(pkg: String): Boolean {
        if (SAFE_SYSTEM_PACKAGES.contains(pkg)) return true
        return SAFE_SYSTEM_PREFIXES.any { pkg.startsWith(it) }
    }

    fun isKeyboard(pkg: String): Boolean {
        return pkg.contains("inputmethod") || pkg.contains(".input")
    }
}
