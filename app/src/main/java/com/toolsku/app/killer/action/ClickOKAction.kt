package com.toolsku.app.killer.action

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult
import com.toolsku.app.killer.step.OpenAppInfoStep

/**
 * Action untuk klik tombol "OK" di dialog konfirmasi.
 * Tiru dari `p40` Baxa.
 */
class ClickOKAction(
    parentStep: ActionStep,
    private val openAppInfoStep: OpenAppInfoStep
) : BaseAction(parentStep) {

    companion object {
        private const val TAG = "ClickOKAction"
        
        // Label OK — urutan prioritas
        private val OK_LABELS = listOf(
            "OK",
            "Ok",
            "ok",
            "Oke",
            "YA",
            "Ya",
            "Yes",
            "YES",
            "确定",       // ZH
            "確定",       // ZH-TW
            "Aceptar",   // ES
            "D'accord",  // FR
        )
        
        // ViewId khusus ColorOS
        private val OK_VIEW_IDS = listOf(
            "com.android.settings:id/button1",
            "com.android.settings:id/button2",
            "android:id/button1",
            "android:id/button2",
            "android:id/ok",
            "com.android.settings:id/ok",
        )
    }

    private var retryCount = 0
    private val maxRetry = 5

    override fun execute(): StepResult {
        val node = currentNode ?: return StepResult.keepStage()
        
        try {
            val forceStopBounds = openAppInfoStep.getForceStopBounds()
            
            // 1. Cari by text
            var targetNode: AccessibilityNodeInfo? = null
            var usedLabel = ""
            
            for (label in OK_LABELS) {
                val candidate = findNodeByTextExact(label)
                if (candidate != null) {
                    // Skip jika posisi sama dengan tombol "Paksa berhenti"
                    if (forceStopBounds != null && isSamePosition(candidate, forceStopBounds)) {
                        continue
                    }
                    targetNode = candidate
                    usedLabel = label
                    break
                }
            }
            
            // 2. Cari by viewId (fallback)
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
                    Log.w(TAG, "OK button not found after $maxRetry retries")
                    // Dialog mungkin tidak muncul — anggap sukses
                    return StepResult.success()
                }
                return StepResult.keepStage()
            }
            
            // 3. Cek enabled
            if (!targetNode.isEnabled) {
                Log.w(TAG, "OK button not enabled")
                return StepResult.keepStage()
            }
            
            // 4. Klik
            parentStep.pause()
            val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            if (clicked) {
                Log.i(TAG, "Clicked OK button: '$usedLabel'")
                return StepResult.success()
            } else {
                Log.w(TAG, "Failed to click OK button")
                return StepResult.keepStage()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing click OK action", e)
            return StepResult.error()
        }
    }

    /**
     * Cek apakah node berada di posisi yang sama dengan bounds.
     */
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
