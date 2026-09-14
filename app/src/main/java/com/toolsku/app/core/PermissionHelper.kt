package com.toolsku.app.core

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

/**
 * Helper untuk cek & minta izin khusus Toolsku.
 */
object PermissionHelper {

    /**
     * Cek apakah overlay (SYSTEM_ALERT_WINDOW) diizinkan.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Intent untuk minta izin overlay.
     */
    fun overlayIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * Cek apakah usage access diizinkan (untuk ukuran cache).
     */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedService = "${context.packageName}/${context.packageName}.automation.AutomationAccessibilityService"
        val expectedServiceShort = "${context.packageName}/.automation.AutomationAccessibilityService"

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val component = splitter.next()
            // Cek beberapa format
            if (component.equals(expectedService, ignoreCase = true) ||
                component.equals(expectedServiceShort, ignoreCase = true) ||
                component.contains("${context.packageName}/") &&
                    component.contains("AutomationAccessibilityService")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Intent untuk minta usage access.
     */
    fun usageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Cek apakah accessibility service aktif.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = "${context.packageName}/${context.packageName}.automation.AutomationAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val component = splitter.next()
            if (component.equals(service, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Intent untuk buka Settings → Accessibility.
     */
    fun accessibilityIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }
}
