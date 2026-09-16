package com.toolsku.app.automation

import android.util.Log

/**
 * Antrian task otomasi.
 */
object TaskQueue {

    private const val TAG = "ToolskuQueue"

    private val tasks = mutableListOf<TaskInfo>()
    private var currentIndex = -1

    /**
     * Progress callback.
     * @param current Nomor task saat ini (1-based)
     * @param total Total task
     * @param packageName Package name app
     * @param appLabel Label/nama app (untuk overlay)
     */
    var onProgress: ((current: Int, total: Int, packageName: String, appLabel: String) -> Unit)? = null

    var onTaskComplete: ((TaskInfo) -> Unit)? = null
    var onAllComplete: ((List<TaskInfo>) -> Unit)? = null

    val isRunning: Boolean
        get() = currentIndex >= 0 && currentIndex < tasks.size

    fun start(newTasks: List<AutomationTask>) {
        clear()
        tasks.addAll(newTasks.map { TaskInfo(it) })
        currentIndex = 0
        Log.i(TAG, "Queue started: ${tasks.size} tasks")
    }

    fun next(): TaskInfo? {
        if (currentIndex < 0 || currentIndex >= tasks.size) {
            if (tasks.isNotEmpty()) {
                onAllComplete?.invoke(tasks.toList())
                currentIndex = -1
            }
            return null
        }
        val task = tasks[currentIndex]
        task.status = TaskStatus.RUNNING

        onProgress?.invoke(
            currentIndex + 1,
            tasks.size,
            task.task.packageName,
            task.task.appLabel
        )

        Log.d(TAG, "Next task ${currentIndex + 1}/${tasks.size}: ${task.task.packageName}")
        return task
    }

    fun completeCurrent(result: TaskResult) {
        if (currentIndex < 0 || currentIndex >= tasks.size) return
        val task = tasks[currentIndex]
        task.result = result
        task.status = when (result) {
            is TaskResult.Success -> TaskStatus.COMPLETED
            is TaskResult.Cancelled -> TaskStatus.CANCELLED
            else -> TaskStatus.FAILED
        }
        onTaskComplete?.invoke(task)
        Log.i(TAG, "Task complete: ${task.task.packageName} -> ${task.status}")

        currentIndex++

        if (currentIndex >= tasks.size) {
            onAllComplete?.invoke(tasks.toList())
            currentIndex = -1
        }
    }

    fun failCurrent(reason: String) {
        completeCurrent(TaskResult.Error(reason))
    }

    fun cancelAll() {
        for (task in tasks) {
            if (task.status == TaskStatus.PENDING || task.status == TaskStatus.RUNNING) {
                task.status = TaskStatus.CANCELLED
                task.result = TaskResult.Cancelled
            }
        }
        currentIndex = -1
        Log.i(TAG, "All tasks cancelled")
    }

    fun clear() {
        tasks.clear()
        currentIndex = -1
    }

    fun getStats(): QueueStats {
        val total = tasks.size
        val success = tasks.count { it.status == TaskStatus.COMPLETED }
        val failed = tasks.count { it.status == TaskStatus.FAILED }
        val cancelled = tasks.count { it.status == TaskStatus.CANCELLED }
        return QueueStats(total, success, failed, cancelled)
    }
}

data class QueueStats(
    val total: Int,
    val success: Int,
    val failed: Int,
    val cancelled: Int
)
