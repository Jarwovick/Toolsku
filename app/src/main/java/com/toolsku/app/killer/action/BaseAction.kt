package com.toolsku.app.killer.action

import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult

/**
 * Base class untuk action.
 * Tiru dari `bp2` Baxa.
 */
abstract class BaseAction(
    protected val parentStep: ActionStep
) {
    var currentNode: AccessibilityNodeInfo? = null

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

    abstract fun execute(): StepResult

    open fun cleanup(isComplete: Boolean) {
        currentNode = null
    }
}
