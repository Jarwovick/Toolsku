package com.toolsku.app.automation

/**
 * Hasil eksekusi satu Task.
 */
sealed class TaskResult {
    /** Task berhasil diselesaikan. */
    object Success : TaskResult()

    /** Task gagal karena node tidak ditemukan. */
    data class NodeNotFound(val step: ActionStep, val reason: String = "") : TaskResult()

    /** Task gagal karena timeout. */
    data class Timeout(val step: ActionStep) : TaskResult()

    /** Task dibatalkan user. */
    object Cancelled : TaskResult()

    /** Task gagal karena error lain. */
    data class Error(val message: String, val cause: Throwable? = null) : TaskResult()
}

/**
 * Status task di queue.
 */
enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Info task yang sedang/sudah dieksekusi.
 */
data class TaskInfo(
    val task: AutomationTask,
    var status: TaskStatus = TaskStatus.PENDING,
    var result: TaskResult? = null,
    var currentStepIndex: Int = 0
)
