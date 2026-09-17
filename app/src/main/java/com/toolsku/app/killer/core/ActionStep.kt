package com.toolsku.app.killer.core

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Interface untuk satu step.
 * Tiru dari `rj2` Baxa.
 */
interface ActionStep {
    /**
     * Mulai step — dipanggil sekali.
     */
    fun start(): StepResult

    /**
     * Handle accessibility event.
     */
    fun handleEvent(
        packageName: String,
        className: String,
        eventType: Int,
        node: AccessibilityNodeInfo?
    ): StepResult

    /**
     * Stop step — cleanup.
     */
    fun stop()

    /**
     * Cek apakah step masih berjalan.
     */
    fun isRunning(): Boolean

    /**
     * Nama step (untuk debug).
     */
    fun getName(): String

    /**
     * Event types yang di-handle step ini.
     */
    fun getSupportedEvents(): List<Int>

    /**
     * Handle pause.
     */
    fun pause()
}
