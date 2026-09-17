package com.toolsku.app.killer.step

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.action.BaseAction
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult
import com.toolsku.app.killer.core.TaskStateMachine
import java.util.concurrent.CopyOnWriteArrayList

abstract class BaseStep : ActionStep {

    companion object {
        private const val TAG = "BaseStep"
        const val TYPE_WINDOW_STATE_CHANGED = 32
        const val TYPE_WINDOW_CONTENT_CHANGED = 2048
        const val TYPE_WINDOWS_CHANGED = 4194304
    }

    protected val actions: MutableList<BaseAction> = CopyOnWriteArrayList()
    protected val eventTypes: MutableList<Int> = CopyOnWriteArrayList()

    @Volatile
    protected var running: Boolean = false

    @Volatile
    protected var paused: Boolean = false

    protected val handler: Handler = Handler(Looper.getMainLooper())

    protected var currentWindowId: Int = -1
    protected var currentPackage: String = ""

    protected var parentTask: TaskStateMachine? = null

    init {
        eventTypes.add(TYPE_WINDOW_STATE_CHANGED)
        eventTypes.add(TYPE_WINDOW_CONTENT_CHANGED)
    }

    override fun start(): StepResult {
        running = true
        paused = false
        return try {
            execute()
        } catch (e: Exception) {
            e.printStackTrace()
            StepResult.error()
        }
    }

    protected abstract fun execute(): StepResult

    override fun handleEvent(
        packageName: String,
        className: String,
        eventType: Int,
        node: AccessibilityNodeInfo?
    ): StepResult {
        if (!running) return StepResult.keepStage()

        if (!eventTypes.contains(eventType)) {
            return StepResult.keepStage()
        }

        if (node != null) {
            try {
                val windowId = node.windowId
                if (windowId > 0 && currentWindowId == windowId) {
                    return StepResult.keepStage()
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        return processActions(eventType, node)
    }

    private fun processActions(
        eventType: Int,
        node: AccessibilityNodeInfo?
    ): StepResult {
        if (!running) return StepResult.waitEvent()

        for (action in actions) {
            action.currentNode = node
            val result = action.execute()
            action.currentNode = null

            if (result.isKeepStage()) {
                return StepResult.keepStage()
            }

            if (result.isSkipTask()) {
                return StepResult.skipTask()
            }

            if (result.isRepeatTask()) {
                return StepResult.repeatTask()
            }

            if (result.isComplete) {
                continue
            }

            return StepResult.waitEvent()
        }

        return StepResult.success()
    }

    override fun stop() {
        running = false
        paused = false
        handler.removeCallbacksAndMessages(null)
        actions.forEach { it.cleanup(false) }
    }

    override fun isRunning(): Boolean = running

    override fun getSupportedEvents(): List<Int> = eventTypes

    override fun pause() {
        paused = true
    }

    override fun setParentTask(task: TaskStateMachine) {
        this.parentTask = task
    }

    override fun setCurrentWindowId(windowId: Int) {
        this.currentWindowId = windowId
    }

    override fun getCurrentWindowId(): Int = currentWindowId

    override fun onResult(result: StepResult) {
        // Default: no-op, bisa di-override
    }

    fun getPackage(): String = currentPackage
}
