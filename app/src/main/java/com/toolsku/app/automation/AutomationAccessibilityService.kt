package com.toolsku.app.automation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Service aksesibilitas untuk otomasi Toolsku.
 *
 * Fase 3A: Hanya deteksi event & log. Belum eksekusi.
 * Fase 3B: Akan ditambah TaskQueue untuk eksekusi.
 */
class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ToolskuA11y"

        /** Instance aktif (dipakai oleh TaskExecutor nanti). */
        var instance: AutomationAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Log untuk debug (hanya event penting)
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg in OemProfile.settingsPackages) {
                    val cls = event.className?.toString() ?: ""
                    Log.d(TAG, "Settings window: $pkg / $cls")
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // biarkan, jangan log karena terlalu banyak
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "Accessibility Service destroyed")
    }
}
