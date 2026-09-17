package com.toolsku.app.killer.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult

/**
 * State machine untuk 1 task (1 app).
 * Tiru dari `tq0` Baxa.
 *
 * Bertanggung jawab:
 * - Kelola daftar step
 * - Pindah antar step
 * - Handle event
 * - Timeout per step
 */
class TaskStateMachine(
    private val context: Context,
    val packageName: String,
    val appLabel: String,
    private val steps: List<ActionStep>,
    private val onTaskComplete: (TaskResult) -> Unit
) {
    companion object {
        private const val TAG = "TaskStateMachine"
        private const val STEP_TIMEOUT_MS = 3500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentStepIndex = 0
    private var isRunning = false
    private var timeoutRunnable: Runnable? = null
    private var windowId = -1

    /**
     * Mulai task.
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Task already running: $packageName")
            return
        }
        
        isRunning = true
        currentStepIndex = 0
        Log.i(TAG, "Starting task: $appLabel ($packageName), steps=${steps.size}")
        
        executeCurrentStep()
    }

    /**
     * Stop task.
     */
    fun stop() {
        isRunning = false
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        steps.forEach { it.stop() }
        Log.i(TAG, "Task stopped: $packageName")
    }

    /**
     * Handle accessibility event.
     */
    fun handleEvent(
        pkg: String,
        className: String,
        eventType: Int,
        node: AccessibilityNodeInfo?
    ) {
        if (!isRunning) return
        if (currentStepIndex >= steps.size) return

        // Update window id kalau ada
        if (node != null) {
            try {
                val id = node.windowId
                if (id > 0) {
                    windowId = id
                    steps[currentStepIndex].setCurrentWindowId(id)
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        // Forward ke step saat ini
        val step = steps[currentStepIndex]
        val result = step.handleEvent(pkg, className, eventType, node)
        
        // Handle result
        handleStepResult(result)
    }

    /**
     * Eksekusi step saat ini.
     */
    private fun executeCurrentStep() {
        if (!isRunning) return
        if (currentStepIndex >= steps.size) {
            finishSuccess()
            return
        }

        val step = steps[currentStepIndex]
        step.setParentTask(this)
        Log.d(TAG, "Executing step ${currentStepIndex + 1}/${steps.size}: ${step.getName()}")

        // Set timeout
        setupTimeout(step)

        // Jalankan step
        try {
            val result = step.start()
            handleStepResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Step execution failed: ${step.getName()}", e)
            handler.removeCallbacks(timeoutRunnable!!)
            moveToNextStep()
        }
    }

    /**
     * Set timeout untuk step.
     */
    private fun setupTimeout(step: ActionStep) {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        
        timeoutRunnable = Runnable {
            Log.w(TAG, "Step timeout: ${step.getName()}")
            // Timeout → anggap selesai step ini, lanjut
            moveToNextStep()
        }
        handler.postDelayed(timeoutRunnable!!, STEP_TIMEOUT_MS)
    }

    /**
     * Handle result dari step.
     */
    private fun handleStepResult(result: StepResult) {
        // Cancel timeout
        timeoutRunnable?.let { handler.removeCallbacks(it) }

        Log.d(TAG, "Step result: $result")

        when {
            result.isSkipTask() -> {
                Log.d(TAG, "Skip task: $packageName")
                finishSkipped()
            }
            result.isRepeatTask() -> {
                Log.d(TAG, "Repeat task: $packageName")
                currentStepIndex = 0
                executeCurrentStep()
            }
            result.isRepeatStage() -> {
                // Ulangi step ini
                executeCurrentStep()
            }
            result.isComplete && result.isSuccess -> {
                // Step selesai, lanjut
                moveToNextStep()
            }
            result.isComplete && result.isError -> {
                // Error — lanjut step berikut atau fail
                Log.w(TAG, "Step error: ${steps[currentStepIndex].getName()}")
                moveToNextStep()
            }
            result.isKeepStage() -> {
                // Tunggu event berikutnya — set timeout lagi
                setupTimeout(steps[currentStepIndex])
            }
            else -> {
                // Wait event
                setupTimeout(steps[currentStepIndex])
            }
        }
    }

    /**
     * Pindah ke step berikutnya.
     */
    private fun moveToNextStep() {
        currentStepIndex++
        if (currentStepIndex >= steps.size) {
            finishSuccess()
        } else {
            executeCurrentStep()
        }
    }

    /**
     * Task selesai sukses.
     */
    private fun finishSuccess() {
        isRunning = false
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        Log.i(TAG, "Task success: $packageName")
        onTaskComplete(TaskResult.Success(packageName, appLabel))
    }

    /**
     * Task di-skip.
     */
    private fun finishSkipped() {
        isRunning = false
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        Log.i(TAG, "Task skipped: $packageName")
        onTaskComplete(TaskResult.Skipped(packageName, appLabel))
    }

    /**
     * Task gagal.
     */
    private fun finishError(reason: String) {
        isRunning = false
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        Log.e(TAG, "Task failed: $packageName — $reason")
        onTaskComplete(TaskResult.Failure(packageName, appLabel, reason))
    }

    /**
     * Cek apakah task masih jalan.
     */
    fun isActive(): Boolean = isRunning
}

/**
 * Hasil akhir task.
 */
sealed class TaskResult {
    data class Success(val packageName: String, val appLabel: String) : TaskResult()
    data class Skipped(val packageName: String, val appLabel: String) : TaskResult()
    data class Failure(val packageName: String, val appLabel: String, val reason: String) : TaskResult()
}
