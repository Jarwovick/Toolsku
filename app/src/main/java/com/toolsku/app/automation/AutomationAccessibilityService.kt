package com.toolsku.app.automation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.engine.QueueManager
import com.toolsku.app.killer.overlay.KillerOverlayManager

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ToolskuA11y"

        @Volatile
        var instance: AutomationAccessibilityService? = null
            private set
    }

    /**
     * Queue Manager untuk Killer.
     */
    var queueManager: QueueManager? = null
        private set

    /**
     * Overlay Manager untuk Killer.
     */
    var overlayManager: KillerOverlayManager? = null
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlayManager = KillerOverlayManager(this)
        Log.i(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: ""
        val eventType = event.eventType
        val source: AccessibilityNodeInfo? = try {
            event.source
        } catch (e: Exception) {
            null
        }

        // Log untuk debug
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Event: $pkg / $cls")
        }

        // Forward ke QueueManager (Killer)
        queueManager?.handleEvent(pkg, cls, eventType, source)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        queueManager?.cancel()
        overlayManager?.hide()
        Log.i(TAG, "Accessibility Service destroyed")
    }

    // ============================================
    // UNTUK KILLER
    // ============================================

    /**
     * Mulai kill apps.
     */
    fun startKillQueue(
        apps: List<QueueManager.AppToKill>,
        onProgress: (current: Int, total: Int, appLabel: String) -> Unit,
        onComplete: (com.toolsku.app.killer.engine.QueueResult) -> Unit
    ): Boolean {
        if (queueManager?.isActive() == true) {
            Log.w(TAG, "Queue already active")
            return false
        }

        // Setup overlay
        overlayManager?.show()
        overlayManager?.update(0, apps.size, "")
        overlayManager?.onStopClick = {
            cancelKillQueue()
        }

        // Buat queue manager
        queueManager = QueueManager(
            context = this,
            onProgress = { current, total, appLabel ->
                overlayManager?.update(current, total, appLabel)
                onProgress(current, total, appLabel)
            },
            onComplete = { result ->
                overlayManager?.hide()
                overlayManager?.onStopClick = null
                queueManager = null
                onComplete(result)
            }
        )

        // Start
        queueManager?.start(apps)
        return true
    }

    /**
     * Cancel queue.
     */
    fun cancelKillQueue() {
        Log.i(TAG, "Cancel kill queue")
        queueManager?.cancel()
        queueManager = null
        overlayManager?.hide()
        overlayManager?.onStopClick = null
    }

    /**
     * Cek apakah ada queue aktif.
     */
    fun isKillQueueActive(): Boolean = queueManager?.isActive() == true
}
