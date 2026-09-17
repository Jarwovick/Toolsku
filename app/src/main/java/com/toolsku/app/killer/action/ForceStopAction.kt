package com.toolsku.app.killer.action

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult
import com.toolsku.app.killer.step.OpenAppInfoStep

/**
 * Action untuk klik tombol "Paksa berhenti".
 * Tiru dari `vj2` Baxa.
 */
class ForceStopAction(
    parentStep: ActionStep,
    private val openAppInfoStep: OpenAppInfoStep
) : BaseAction(parentStep) {

    companion object {
        private const val TAG = "ForceStopAction"
        
        // Label yang dicoba (urutan prioritas)
        private val FORCE_STOP_LABELS = listOf(
            "Paksa berhenti",       // ID
            "Force stop",           // EN
            "Force Stop",           // EN
            "强制停止",              // ZH
            "强制停止应用",          // ZH
            "強制停止",              // ZH-TW
            "Detener",              // ES
            "Forcer l'arrêt",       // FR
        )
    }

    private var retryCount = 0
    private val maxRetry = 3

    override fun execute(): StepResult {
        val node = currentNode ?: return StepResult.keepStage()
        
        try {
            // 1. Coba cari dengan label teks
            var targetNode: AccessibilityNodeInfo? = null
            var usedLabel = ""
            
            for (label in FORCE_STOP_LABELS) {
                targetNode = findNodeByTextExact(label)
                if (targetNode != null) {
                    usedLabel = label
                    Log.d(TAG, "Found force stop button: '$label'")
                    break
                }
            }
            
            // 2. Cari juga di semua node (fallback)
            if (targetNode == null) {
                targetNode = findForceStopNode(node)
            }
            
            if (targetNode == null) {
                retryCount++
                if (retryCount >= maxRetry) {
                    Log.w(TAG, "Force stop button not found after $maxRetry retries")
                    return StepResult.skipTask()
                }
                return StepResult.keepStage()
            }
            
            // 3. Cek apakah node enabled
            if (!targetNode.isEnabled) {
                Log.w(TAG, "Force stop button not enabled")
                return StepResult.keepStage()
            }
            
            // 4. Simpan posisi untuk cache
            val bounds = Rect()
            targetNode.getBoundsInScreen(bounds)
            openAppInfoStep.setForceStopBounds(bounds)
            
            // 5. Klik
            parentStep.pause()
            val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            if (clicked) {
                Log.i(TAG, "Clicked force stop button: '$usedLabel'")
                return StepResult.success()
            } else {
                Log.w(TAG, "Failed to click force stop button")
                return StepResult.keepStage()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing force stop action", e)
            return StepResult.error()
        }
    }

    /**
     * Fallback: cari node dengan "force stop" di berbagai form.
     */
    private fun findForceStopNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxVisited = 200  // prevent infinite
        
        while (queue.isNotEmpty() && visited < maxVisited) {
            val node = queue.removeFirst()
            visited++
            
            try {
                val text = node.text?.toString() ?: ""
                val desc = node.contentDescription?.toString() ?: ""
                
                val isMatch = FORCE_STOP_LABELS.any { label ->
                    text.equals(label, ignoreCase = true) ||
                    desc.equals(label, ignoreCase = true) ||
                    text.contains("paksa berhenti", ignoreCase = true) ||
                    text.contains("force stop", ignoreCase = true) ||
                    desc.contains("force stop", ignoreCase = true)
                }
                
                if (isMatch && node.isClickable) {
                    return node
                }
                
                // Enqueue children
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) queue.add(child)
                }
            } catch (e: Exception) {
                // skip
            }
        }
        
        return null
    }

    override fun cleanup(isComplete: Boolean) {
        super.cleanup(isComplete)
        if (!isComplete) {
            retryCount = 0
        }
    }
}
