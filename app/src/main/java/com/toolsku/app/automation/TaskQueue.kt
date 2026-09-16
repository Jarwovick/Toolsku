package com.toolsku.app.automation

import android.util.Log

/**
 * Antrian task otomasi.
 * Menyimpan daftar task yang akan dieksekusi berurutan.
 */
object TaskQueue {

    private const val TAG = "ToolskuQueue"

    private val tasks = mutableListOf<TaskInfo>()
    private var currentIndex = -1

    /**
     * Progress callback.
     *
     * @param current Nomor task saat ini (1-based)
     * @param total Total task
     * @param packageName Package name app yang sedang diproses
     * @param appLabel Label/nama app yang sedang diproses (untuk overlay)
     */
    var onProgress: ((current: Int, total: Int, packageName: String, appLabel: String) -> Unit)? = null

    /** Callback saat satu task selesai. */
    var onTaskComplete: ((TaskInfo) -> Unit)? = null

    /** Callback saat semua task selesai. */
    var onAllComplete: ((List<TaskInfo>) -> Unit)? = null

    /** Apakah sedang berjalan. */
    val isRunning: Boolean
        get() = currentIndex >= 0 && currentIndex < tasks.size

    /**
     * Mulai queue baru. Hentikan yang lama (kalau ada).
     */
    fun start(newTasks: List<AutomationTask>) {
        clear()
        tasks.addAll(newTasks.map { TaskInfo(it) })
        currentIndex = 0
        Log.i(TAG, "Queue started: ${tasks.size} tasks")
    }

    /**
     * Ambil task berikutnya. Return null kalau habis.
     */
    fun next(): TaskInfo? {
        if (currentIndex < 0 || currentIndex >= tasks.size) {
            // Semua selesai
            if (tasks.isNotEmpty()) {
                onAllComplete?.invoke(tasks.toList())
                currentIndex = -1
            }
            return null
        }
        val task = tasks[currentIndex]
        task.status = TaskStatus.RUNNING

        // Panggil progress callback dengan packageName + appLabel
        onProgress?.invoke(
            currentIndex + 1,
            tasks.size,
            task.task.packageName,
            task.task.appLabel
        )

        Log.d(TAG, "Next task ${currentIndex + 1}/${tasks.size}: ${task.task.packageName}")
        return task
    }

    /**
     * Tandai task saat ini selesai dengan hasil tertentu.
     */
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

        // Update index
        currentIndex++

        // Cek apakah masih ada task
        if (currentIndex >= tasks.size) {
            onAllComplete?.invoke(tasks.toList())
            currentIndex = -1
        }
    }

    /**
     * Tandai task saat ini gagal.
     */
    fun failCurrent(reason: String) {
        completeCurrent(TaskResult.Error(reason))
    }

    /**
     * Batalkan semua task.
     */
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

    /**
     * Bersihkan queue.
     */
    fun clear() {
        tasks.clear()
        currentIndex = -1
    }

    /**
     * Statistik.
     */
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
