package com.toolsku.app.killer.action

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult
import com.toolsku.app.killer.step.OpenAppInfoStep

class ForceStopAction(
    parentStep: ActionStep,
    private val openAppInfoStep: OpenAppInfoStep
) : BaseAction(parentStep) {

    companion object {
        private const val TAG = "ForceStopAction"
        
        private val FORCE_STOP_LABELS = listOf(
            "Paksa berhenti",
            "Force stop",
            "Force Stop",
            "强制停止",
            "强制停止应用",
            "強制停止",
        )
    }

    private var retryCount = 0
    private val maxRetry = 3

    override fun execute(): StepResult {
        val node = currentNode ?: return StepResult.keepStage()
        
        try {
            var targetNode: AccessibilityNodeInfo? = null
            var usedLabel = ""
            
            for (label in FORCE_STOP_LABELS) {
                targetNode = findNodeByTextExact(label)
                if (targetNode != null) {
                    usedLabel = label
                    Log.d(TAG, "Found: '$label'")
                    break
                }
            }
            
            if (targetNode == null) {
                targetNode = findForceStopNode(node)
            }
            
            if (targetNode == null) {
                retryCount++
                if (retryCount >= maxRetry) {
                    Log.w(TAG, "Not found after $maxRetry retries")
                    return StepResult.skipTask()
                }
                return StepResult.keepStage()
            }
            
            if (!targetNode.isEnabled) {
                Log.w(TAG, "Not enabled")
                return StepResult.keepStage()
            }
            
            val bounds = Rect()
            targetNode.getBoundsInScreen(bounds)
            openAppInfoStep.setForceStopBounds(bounds)
            
            parentStep.pause()
            val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            if (clicked) {
                Log.i(TAG, "Clicked: '$usedLabel'")
                return StepResult.success()
            } else {
                Log.w(TAG, "Failed to click")
                return StepResult.keepStage()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return StepResult.error()
        }
    }

    private fun findForceStopNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxVisited = 200
        
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
                    text.contains("force stop", ignoreCase = true)
                }
                
                if (isMatch && node.isClickable) {
                    return node
                }
                
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
