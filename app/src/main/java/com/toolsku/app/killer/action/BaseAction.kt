package com.toolsku.app.killer.action

import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult

/**
 * Base class untuk action.
 * Tiru dari `bp2` Baxa.
 *
 * Action = operasi spesifik dalam satu step.
 * Contoh: cari & klik tombol "Paksa berhenti".
 */
abstract class BaseAction(
    protected val parentStep: ActionStep
) {
    /**
     * Node current — di-set oleh step sebelum `execute()`.
     */
    var currentNode: AccessibilityNodeInfo? = null

    /**
     * Cari node berdasarkan viewId.
     */
    protected fun findNodeByViewId(viewId: String): AccessibilityNodeInfo? {
        val node = currentNode ?: return null
        try {
            val nodes = node.findAccessibilityNodeInfosByViewId(viewId) ?: return null
            for (n in nodes) {
                if (n != null && n.isClickable) {
                    return n
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Cari node berdasarkan text (exact).
     */
    protected fun findNodeByTextExact(text: String): AccessibilityNodeInfo? {
        val node = currentNode ?: return null
        try {
            val nodes = node.findAccessibilityNodeInfosByText(text) ?: return null
            for (n in nodes) {
                if (n != null && n.isClickable) {
                    return n
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Eksekusi action. Return hasil.
     */
    abstract fun execute(): StepResult

    /**
     * Cleanup — dipanggil saat action dihapus.
     */
    open fun cleanup(isComplete: Boolean) {
        currentNode = null
    }
}
