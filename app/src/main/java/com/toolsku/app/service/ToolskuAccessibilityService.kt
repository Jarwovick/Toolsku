package com.toolsku.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ToolskuAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ToolskuAccessibilityService? = null
        var isAutoStopRunning = false
        var isAutoClearCacheRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val source = event.source ?: return

        // Logika Otomasi Force Stop
        if (isAutoStopRunning) {
            performForceStop(source)
        }

        // Logika Otomasi Clear Cache
        if (isAutoClearCacheRunning) {
            performClearCache(source)
        }
    }

    private fun performForceStop(rootNode: AccessibilityNodeInfo) {
        // Cari tombol "Force Stop" / "Paksa Berhenti"
        val forceStopNodes = rootNode.findAccessibilityNodeInfosByText("Force stop")
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Paksa berhenti") }

        for (node in forceStopNodes) {
            if (node.isEnabled) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        // Cari dialog konfirmasi "OK" / "Paksa Berhenti"
        val okNodes = rootNode.findAccessibilityNodeInfosByText("OK")
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Paksa berhenti") }

        for (node in okNodes) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun performClearCache(rootNode: AccessibilityNodeInfo) {
        // Cari menu "Storage" / "Penyimpanan" jika belum terbuka
        val storageNodes = rootNode.findAccessibilityNodeInfosByText("Storage")
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Penyimpanan") }
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Storage & cache") }

        for (node in storageNodes) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Cari tombol "Clear cache" / "Hapus memori simpanan"
        val clearCacheNodes = rootNode.findAccessibilityNodeInfosByText("Clear cache")
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Hapus cache") }
            .ifEmpty { rootNode.findAccessibilityNodeInfosByText("Hapus memori simpanan") }

        for (node in clearCacheNodes) {
            if (node.isEnabled) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    fun openPowerMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
