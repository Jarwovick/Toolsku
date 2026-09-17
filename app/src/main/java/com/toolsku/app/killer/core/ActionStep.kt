package com.toolsku.app.killer.core

import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.engine.TaskStateMachine

/**
 * Interface untuk satu step.
 * Tiru dari `rj2` Baxa.
 */
interface ActionStep {
    fun start(): StepResult

    fun handleEvent(
        packageName: String,
        className: String,
        eventType: Int,
        node: AccessibilityNodeInfo?
    ): StepResult

    fun stop()

    fun isRunning(): Boolean

    fun getName(): String

    fun getSupportedEvents(): List<Int>

    fun pause()

    fun setParentTask(task: TaskStateMachine)

    fun setCurrentWindowId(windowId: Int)

    fun getCurrentWindowId(): Int

    fun onResult(result: StepResult)
}
