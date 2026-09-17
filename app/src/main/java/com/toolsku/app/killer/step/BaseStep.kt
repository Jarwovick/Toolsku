package com.toolsku.app.killer.step

import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.action.BaseAction
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Base class untuk step.
 * Tiru dari `ip2` Baxa.
 */
abstract class BaseStep : ActionStep {

    /**
     * Daftar action dalam step ini.
     */
    protected val actions: MutableList<BaseAction> = CopyOnWriteArrayList()

    /**
     * Event types yang di-handle.
     * ⚠️ Rename dari `supportedEvents` ke `eventTypes` untuk hindari
     * konflik dengan `ActionStep.getSupportedEvents()`.
     */
    protected val eventTypes: MutableList<Int> = CopyOnWriteArrayList()

    @Volatile
    protected var running: Boolean = false

    @Volatile
    protected var paused: Boolean = false

    protected val handler: Handler = Handler(Looper.getMainLooper())

    protected var currentWindowId: Int = -1

    protected var currentPackage: String = ""

    init {
        eventTypes.add(TYPE_WINDOW_STATE_CHANGED)
        eventTypes.add(TYPE_WINDOW_CONTENT_CHANGED)
    }

    companion object {
        const val TYPE_WINDOW_STATE_CHANGED = 32
        const val TYPE_WINDOW_CONTENT_CHANGED = 2048
        const val TYPE_WINDOWS_CHANGED = 4194304
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

    /**
     * Eksekusi step — override di subclass.
     */
    protected abstract fun execute(): StepResult

    override fun handleEvent(
        packageName: String,
        className: String,
        eventType: Int,
        node: AccessibilityNodeInfo?
    ): StepResult {
        if (!running) return StepResult.keepStage()

        // Cek event type didukung
        if (!eventTypes.contains(eventType)) {
            return StepResult.keepStage()
        }

        // Cek window ID (skip event dari window lain)
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

        // Process actions
        return processActions(eventType, node)
    }

    /**
     * Process actions dengan node.
     */
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

    /**
     * Return event types — dipanggil oleh ActionStep interface.
     */
    override fun getSupportedEvents(): List<Int> = eventTypes

    override fun pause() {
        paused = true
    }

    open fun canSkip(): Boolean = false

    fun getPackage(): String = currentPackage
}
