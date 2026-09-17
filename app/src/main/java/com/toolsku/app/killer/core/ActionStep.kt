package com.toolsku.app.killer.core

import android.view.accessibility.AccessibilityNodeInfo

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

    /**
     * Set parent task (untuk callback).
     */
    fun setParentTask(task: TaskStateMachine)

    /**
     * Set current window id.
     */
    fun setCurrentWindowId(windowId: Int)

    /**
     * Get current window id.
     */
    fun getCurrentWindowId(): Int

    /**
     * Handle state machine result — callback dari parent.
     */
    fun onResult(result: StepResult)
}
