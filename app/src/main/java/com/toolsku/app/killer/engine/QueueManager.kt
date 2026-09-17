package com.toolsku.app.killer.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.CancelReason
import com.toolsku.app.killer.core.NavigationKeysWatcher
import com.toolsku.app.killer.core.ScreenStateWatcher

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
    private var cancelReason = CancelReason.NONE

    // ⭐ Watchers
    private val navWatcher = NavigationKeysWatcher(
        context = context,
        onHomePressed = {
            Log.i(TAG, "Home pressed — cancel queue")
            cancelReason = CancelReason.HOME_BUTTON
            cancelInternal()
        }
    )

    private val screenWatcher = ScreenStateWatcher(
        context = context,
        onScreenOff = {
            Log.i(TAG, "Screen off — cancel queue")
            cancelReason = CancelReason.SCREEN_OFF
            cancelInternal()
        }
    )

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
            onComplete(QueueResult(0, 0, 0, CancelReason.NONE))
            return
        }

        isRunning = true
        currentIndex = 0
        cancelReason = CancelReason.NONE
        Log.i(TAG, "Queue started: ${tasks.size} tasks")

        // ⭐ Start watchers
        navWatcher.start()
        screenWatcher.start()

        processNextTask()
    }

    private fun processNextTask() {
        if (!isRunning) return
        if (currentIndex >= tasks.size) {
            finishQueue()
            return
        }

        val taskInfo = tasks[currentIndex]
        Log.i(TAG, "Task ${currentIndex + 1}/${tasks.size}: ${taskInfo.appLabel}")

        onProgress(currentIndex + 1, tasks.size, taskInfo.appLabel)

        currentTask = TaskStateMachine(
            context = context,
            packageName = taskInfo.packageName,
            appLabel = taskInfo.appLabel,
            steps = taskInfo.steps,
            onTaskComplete = { result ->
                when (result) {
                    is TaskResult.Success -> taskInfo.status = TaskStatus.SUCCESS
                    is TaskResult.Skipped -> taskInfo.status = TaskStatus.SKIPPED
                    is TaskResult.Failure -> taskInfo.status = TaskStatus.FAILED
                }

                handler.postDelayed({
                    currentIndex++
                    currentTask = null
                    processNextTask()
                }, DELAY_BETWEEN_TASKS)
            }
        )

        currentTask?.start()
    }

    fun handleEvent(
        pkg: String,
        className: String,
        eventType: Int,
        node: AccessibilityNodeInfo?
    ) {
        currentTask?.handleEvent(pkg, className, eventType, node)
    }

    private fun finishQueue() {
        isRunning = false
        stopWatchers()

        val success = tasks.count { it.status == TaskStatus.SUCCESS }
        val skipped = tasks.count { it.status == TaskStatus.SKIPPED }
        val failed = tasks.count { it.status == TaskStatus.FAILED }

        Log.i(TAG, "Queue complete: success=$success, skipped=$skipped, failed=$failed")
        onComplete(QueueResult(success, skipped, failed, cancelReason))
    }

    fun cancel() {
        Log.i(TAG, "Cancel requested by user")
        cancelReason = CancelReason.USER_CANCELLED
        cancelInternal()
    }

    private fun cancelInternal() {
        isRunning = false
        currentTask?.stop()
        currentTask = null
        handler.removeCallbacksAndMessages(null)
        stopWatchers()

        val success = tasks.count { it.status == TaskStatus.SUCCESS }
        val skipped = tasks.count { it.status == TaskStatus.SKIPPED }
        val failed = tasks.count { it.status == TaskStatus.FAILED }

        onComplete(QueueResult(success, skipped, failed, cancelReason))
    }

    private fun stopWatchers() {
        try {
            navWatcher.stop()
            screenWatcher.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop watchers", e)
        }
    }

    fun isActive(): Boolean = isRunning

    data class TaskInfo(
        val packageName: String,
        val appLabel: String,
        val steps: List<ActionStep>,
        var status: TaskStatus = TaskStatus.PENDING
    )

    enum class TaskStatus {
        PENDING, SUCCESS, SKIPPED, FAILED
    }

    data class AppToKill(
        val packageName: String,
        val appLabel: String
    )
}

data class QueueResult(
    val success: Int,
    val skipped: Int,
    val failed: Int,
    val cancelReason: CancelReason = CancelReason.NONE
)
