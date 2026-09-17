package com.toolsku.app.killer.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep

/**
 * Queue Manager untuk kelola task.
 * Tiru dari `rx2` Baxa.
 */
class QueueManager(
    private val context: Context,
    private val onProgress: (current: Int, total: Int, appLabel: String) -> Unit,
    private val onComplete: (QueueResult) -> Unit
) {
    companion object {
        private const val TAG = "QueueManager"
        private const val DELAY_BETWEEN_TASKS = 300L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tasks = mutableListOf<TaskInfo>()
    private var currentIndex = -1
    private var currentTask: TaskStateMachine? = null
    private var isRunning = false

    /**
     * Mulai queue dengan daftar app.
     */
    fun start(apps: List<AppToKill>) {
        if (isRunning) {
            Log.w(TAG, "Queue already running")
            return
        }

        tasks.clear()
        apps.forEach { app ->
            val steps = StepFactory.createKillSteps(context, app.packageName)
            tasks.add(TaskInfo(app.packageName, app.appLabel, steps))
        }

        if (tasks.isEmpty()) {
            Log.w(TAG, "No tasks to process")
            onComplete(QueueResult(0, 0, 0))
            return
        }

        isRunning = true
        currentIndex = 0
        Log.i(TAG, "Queue started: ${tasks.size} tasks")
        
        processNextTask()
    }

    /**
     * Process task berikutnya.
     */
    private fun processNextTask() {
        if (!isRunning) return
        if (currentIndex >= tasks.size) {
            finishQueue()
            return
        }

        val taskInfo = tasks[currentIndex]
        Log.i(TAG, "Task ${currentIndex + 1}/${tasks.size}: ${taskInfo.appLabel}")
        
        // Update progress
        onProgress(currentIndex + 1, tasks.size, taskInfo.appLabel)

        // Buat task
        currentTask = TaskStateMachine(
            context = context,
            packageName = taskInfo.packageName,
            appLabel = taskInfo.appLabel,
            steps = taskInfo.steps,
            onTaskComplete = { result ->
                // Catat hasil
                when (result) {
                    is TaskResult.Success -> taskInfo.status = TaskStatus.SUCCESS
                    is TaskResult.Skipped -> taskInfo.status = TaskStatus.SKIPPED
                    is TaskResult.Failure -> taskInfo.status = TaskStatus.FAILED
                }
                
                // Delay antar task
                handler.postDelayed({
                    currentIndex++
                    currentTask = null
                    processNextTask()
                }, DELAY_BETWEEN_TASKS)
            }
        )
        
        currentTask?.start()
    }

    /**
     * Handle accessibility event — forward ke task.
     */
    fun handleEvent(
        pkg: String,
        className: String,
        eventType: Int,
        node: AccessibilityNodeInfo?
    ) {
        currentTask?.handleEvent(pkg, className, eventType, node)
    }

    /**
     * Selesai semua task.
     */
    private fun finishQueue() {
        isRunning = false
        val success = tasks.count { it.status == TaskStatus.SUCCESS }
        val skipped = tasks.count { it.status == TaskStatus.SKIPPED }
        val failed = tasks.count { it.status == TaskStatus.FAILED }
        
        Log.i(TAG, "Queue complete: success=$success, skipped=$skipped, failed=$failed")
        onComplete(QueueResult(success, skipped, failed))
    }

    /**
     * Batalkan queue.
     */
    fun cancel() {
        Log.i(TAG, "Cancel queue requested")
        isRunning = false
        currentTask?.stop()
        currentTask = null
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Cek apakah queue masih jalan.
     */
    fun isActive(): Boolean = isRunning

    /**
     * Task info internal.
     */
    data class TaskInfo(
        val packageName: String,
        val appLabel: String,
        val steps: List<ActionStep>,
        var status: TaskStatus = TaskStatus.PENDING
    )

    enum class TaskStatus {
        PENDING, SUCCESS, SKIPPED, FAILED
    }

    /**
     * Data app to kill.
     */
    data class AppToKill(
        val packageName: String,
        val appLabel: String
    )
}

/**
 * Hasil akhir queue.
 */
data class QueueResult(
    val success: Int,
    val skipped: Int,
    val failed: Int
)
