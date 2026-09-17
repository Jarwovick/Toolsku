package com.toolsku.app.killer.core

import android.view.accessibility.AccessibilityNodeInfo

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
}
