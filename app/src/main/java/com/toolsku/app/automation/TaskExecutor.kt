package com.toolsku.app.automation

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class TaskExecutor(
    private val service: AccessibilityService,
    private val context: Context
) {

    companion object {
        private const val TAG = "ToolskuExecutor"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var onFinish: ((TaskResult) -> Unit)? = null

    fun execute(task: TaskInfo, onFinish: (TaskResult) -> Unit) {
        this.onFinish = onFinish
        Log.i(TAG, "Executing task: ${task.task.packageName}")

        if (task.task.steps.isEmpty()) {
            finish(TaskResult.Error("Task has no steps"))
            return
        }

        executeStep(task, 0)
    }

    private fun executeStep(task: TaskInfo, stepIndex: Int) {
        if (stepIndex >= task.task.steps.size) {
            finish(TaskResult.Success)
            return
        }

        task.currentStepIndex = stepIndex
        val step = task.task.steps[stepIndex]
        Log.d(TAG, "Step $stepIndex: $step")

        when (step) {
            is ActionStep.OpenAppInfo -> {
                if (openAppInfo(step.packageName)) {
                    delay(400) { executeStep(task, stepIndex + 1) }
                } else {
                    finish(TaskResult.Error("Failed to open App Info"))
                }
            }

            is ActionStep.ClickByText -> {
                if (clickByText(step.texts)) {
                    delay(250) { executeStep(task, stepIndex + 1) }
                } else {
                    finish(TaskResult.NodeNotFound(step, "Text not found or disabled"))
                }
            }

            is ActionStep.ClickByTextSafe -> {
                if (clickByTextSafe(step.texts, step.dangerTexts)) {
                    delay(250) { executeStep(task, stepIndex + 1) }
                } else {
                    finish(TaskResult.NodeNotFound(step, "Safe text not found"))
                }
            }

            is ActionStep.Back -> {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                delay(200) { executeStep(task, stepIndex + 1) }
            }

            is ActionStep.Wait -> {
                delay(step.millis) { executeStep(task, stepIndex + 1) }
            }

            is ActionStep.ExpectText -> {
                if (expectText(step.texts)) {
                    executeStep(task, stepIndex + 1)
                } else {
                    finish(TaskResult.NodeNotFound(step, "Expected text not found"))
                }
            }
        }
    }

    // ==== ACTIONS ====

    private fun openAppInfo(pkg: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open App Info", e)
            false
        }
    }

    /**
     * Klik node yang ada di daftar texts.
     * SKIP kalau node disabled (tombol abu-abu).
     */
    private fun clickByText(texts: List<String>): Boolean {
        val root = service.rootInActiveWindow ?: return false
        val node = NodeFinder.findByText(root, texts) ?: return false

        // ⭐ Skip kalau node disabled
        if (!node.isEnabled) {
            Log.w(TAG, "Node found but DISABLED: ${texts.firstOrNull()}")
            return false
        }

        // Cek clickable parent
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        val clickableParent = NodeFinder.findClickableParent(node)
        if (clickableParent != null && clickableParent.isEnabled) {
            return clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        Log.w(TAG, "Node not clickable: ${texts.firstOrNull()}")
        return false
    }

    private fun clickByTextSafe(texts: List<String>, dangerTexts: List<String>): Boolean {
        val root = service.rootInActiveWindow ?: return false
        val node = NodeFinder.findByTextSafe(root, texts, dangerTexts) ?: return false

        if (!node.isEnabled) {
            Log.w(TAG, "Safe node found but DISABLED")
            return false
        }

        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        val clickableParent = NodeFinder.findClickableParent(node)
        if (clickableParent != null && clickableParent.isEnabled) {
            return clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        return false
    }

    private fun expectText(texts: List<String>): Boolean {
        val root = service.rootInActiveWindow ?: return false
        return NodeFinder.findByText(root, texts) != null
    }

    // ==== UTIL ====

    private fun delay(millis: Long, action: () -> Unit) {
        handler.postDelayed(action, millis)
    }

    private fun finish(result: TaskResult) {
        Log.i(TAG, "Task finish: $result")
        onFinish?.invoke(result)
        onFinish = null
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
    }
}
