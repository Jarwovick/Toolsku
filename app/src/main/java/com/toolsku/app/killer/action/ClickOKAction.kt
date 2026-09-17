package com.toolsku.app.killer.action

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult
import com.toolsku.app.killer.step.OpenAppInfoStep

class ClickOKAction(
    parentStep: ActionStep,
    private val openAppInfoStep: OpenAppInfoStep
) : BaseAction(parentStep) {

    companion object {
        private const val TAG = "ClickOKAction"
        
        private val OK_LABELS = listOf(
            "OK", "Ok", "ok", "Oke",
            "YA", "Ya", "Yes", "YES",
            "确定", "確定",
        )
        
        private val OK_VIEW_IDS = listOf(
            "com.android.settings:id/button1",
            "com.android.settings:id/button2",
            "android:id/button1",
            "android:id/button2",
        )
    }

    private var retryCount = 0
    private val maxRetry = 5

    override fun execute(): StepResult {
        val node = currentNode ?: return StepResult.keepStage()
        
        try {
            val forceStopBounds = openAppInfoStep.getForceStopBounds()
            
            var targetNode: AccessibilityNodeInfo? = null
            var usedLabel = ""
            
            for (label in OK_LABELS) {
                val candidate = findNodeByTextExact(label)
                if (candidate != null) {
                    if (forceStopBounds != null && isSamePosition(candidate, forceStopBounds)) {
                        continue
                    }
                    targetNode = candidate
                    usedLabel = label
                    break
                }
            }
            
            if (targetNode == null) {
                for (viewId in OK_VIEW_IDS) {
                    val candidate = findNodeByViewId(viewId)
                    if (candidate != null) {
                        if (forceStopBounds != null && isSamePosition(candidate, forceStopBounds)) {
                            continue
                        }
                        targetNode = candidate
                        usedLabel = "viewId:$viewId"
                        break
                    }
                }
            }
            
            if (targetNode == null) {
                retryCount++
                if (retryCount >= maxRetry) {
                    Log.w(TAG, "OK not found after $maxRetry — assume success")
                    return StepResult.success()
                }
                return StepResult.keepStage()
            }
            
            if (!targetNode.isEnabled) {
                return StepResult.keepStage()
            }
            
            parentStep.pause()
            val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            if (clicked) {
                Log.i(TAG, "Clicked OK: '$usedLabel'")
                return StepResult.success()
            } else {
                return StepResult.keepStage()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return StepResult.error()
        }
    }

    private fun isSamePosition(node: AccessibilityNodeInfo, bounds: Rect): Boolean {
        return try {
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds)
            nodeBounds.isEmpty() || nodeBounds == bounds
        } catch (e: Exception) {
            false
        }
    }

    override fun cleanup(isComplete: Boolean) {
        super.cleanup(isComplete)
        if (!isComplete) {
            retryCount = 0
        }
    }
}
