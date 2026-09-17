package com.toolsku.app.automation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.core.AppTracker
import com.toolsku.app.killer.engine.QueueManager
import com.toolsku.app.killer.engine.QueueResult
import com.toolsku.app.killer.overlay.KillerOverlayManager

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ToolskuA11y"

        @Volatile
        var instance: AutomationAccessibilityService? = null
            private set

        @Volatile
        var onStopClick: (() -> Unit)? = null
    }

    var queueManager: QueueManager? = null
        private set

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

        // ⭐ 1. TRACK APP — isi database
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AppTracker.trackAppOpened(this, pkg)
        }

        // ⭐ 2. FORWARD KE KILLER
        queueManager?.handleEvent(pkg, cls, eventType, source)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        onStopClick = null
        queueManager?.cancel()
        queueManager = null
        overlayManager?.hide()
        overlayManager = null
        Log.i(TAG, "Accessibility Service destroyed")
    }

    // ============================================
    // KILLER
    // ============================================

    fun startKillQueue(
        apps: List<QueueManager.AppToKill>,
        onProgress: (current: Int, total: Int, appLabel: String) -> Unit,
        onComplete: (QueueResult) -> Unit
    ): Boolean {
        if (queueManager?.isActive() == true) {
            Log.w(TAG, "Queue already active")
            return false
        }

        // Tampilkan overlay
        overlayManager?.show()
        overlayManager?.update(0, apps.size, "")
        overlayManager?.onStopClick = {
            cancelQueue()
        }

        // Buat QueueManager
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

        queueManager?.start(apps)
        return true
    }

    fun cancelQueue() {
        Log.i(TAG, "Cancel queue")
        queueManager?.cancel()
        queueManager = null
        overlayManager?.hide()
        overlayManager?.onStopClick = null
    }

    fun hideOverlay() {
        Log.i(TAG, "Hide overlay")
        overlayManager?.hide()
        overlayManager?.onStopClick = null
    }

    fun showOverlay(current: Int, total: Int, appLabel: String, mode: String) {
        Log.i(TAG, "Show overlay: mode=$mode")
        overlayManager?.show()
        overlayManager?.update(current, total, appLabel)
    }

    fun updateOverlay(current: Int, total: Int, appLabel: String) {
        overlayManager?.update(current, total, appLabel)
    }

    fun isKillQueueActive(): Boolean = queueManager?.isActive() == true
}
