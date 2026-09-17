package com.toolsku.app.killer.core

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Interface untuk satu step (satu unit kerja).
 * Tiru dari `rj2` Baxa.
 *
 * Flow:
 * 1. `start()` dipanggil pertama kali → bisa buka activity / trigger
 * 2. Setelah event accessibility masuk → `handleEvent()` dipanggil
 * 3. `stop()` dipanggil saat step berhenti
 */
interface ActionStep {
    /**
     * Mulai step — dipanggil sekali.
     * Contoh: buka App Info, klik tombol.
     */
    fun start(): StepResult

    /**
     * Handle accessibility event.
     * Dipanggil setiap event masuk dari Accessibility Service.
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
     * Return list int: 32 (WINDOW_STATE_CHANGED), 2048 (WINDOW_CONTENT_CHANGED).
     */
    fun getSupportedEvents(): List<Int>

    /**
     * Handle pause.
     */
    fun pause()
}
