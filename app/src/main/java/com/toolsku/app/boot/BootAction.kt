package com.toolsku.app.boot

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import com.toolsku.app.automation.AutomationAccessibilityService

/**
 * Helper untuk membuka Power Menu (dialog power) via Accessibility Service.
 *
 * Di Android 12+ (API 31), tersedia GLOBAL_ACTION_POWER_DIALOG.
 * Di ColorOS (Oppo/Realme), konstanta ini sering tersedia juga di Android 10
 * sebagai ekstensi OEM — kita coba panggil via reflection.
 */
object BootAction {

    private const val TAG = "ToolskuBoot"

    /**
     * Nilai konstanta GLOBAL_ACTION_POWER_DIALOG.
     * Di AOSP API 31+, nilainya = 6.
     */
    private const val GLOBAL_ACTION_POWER_DIALOG = 6

    /**
     * Coba buka Power Menu.
     * @return true kalau berhasil.
     */
    fun openPowerMenu(): Boolean {
        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Log.w(TAG, "Accessibility service not active")
            return false
        }

        // Cara 1: Panggil langsung (Android 12+, atau ColorOS dengan ekstensi)
        try {
            val method = AccessibilityService::class.java.getMethod(
                "performGlobalAction",
                Int::class.javaPrimitiveType
            )
            val result = method.invoke(service, GLOBAL_ACTION_POWER_DIALOG) as? Boolean ?: false
            if (result) {
                Log.i(TAG, "Power dialog opened (direct)")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct call failed: ${e.message}")
        }

        // Cara 2: Coba reflection field (kadang nilainya beda per OEM)
        try {
            val field = AccessibilityService::class.java.getField("GLOBAL_ACTION_POWER_DIALOG")
            val value = field.getInt(null)
            val method = AccessibilityService::class.java.getMethod(
                "performGlobalAction",
                Int::class.javaPrimitiveType
            )
            val result = method.invoke(service, value) as? Boolean ?: false
            if (result) {
                Log.i(TAG, "Power dialog opened (reflection field=$value)")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reflection field failed: ${e.message}")
        }

        Log.e(TAG, "All methods failed")
        return false
    }

    /**
     * Cek apakah device mendukung Power Menu via Accessibility.
     */
    fun isSupported(): Boolean {
        // Android 12+ pasti mendukung
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return true

        // Android 10-11: cek apakah ColorOS (Oppo/Realme/OnePlus)
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("oppo") ||
               manufacturer.contains("realme") ||
               manufacturer.contains("oneplus")
    }
}
