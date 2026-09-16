package com.toolsku.app.automation

/**
 * Hasil eksekusi satu Task.
 */
sealed class TaskResult {
    object Success : TaskResult()
    data class NodeNotFound(val step: ActionStep, val reason: String = "") : TaskResult()
    data class Timeout(val step: ActionStep) : TaskResult()
    object Cancelled : TaskResult()
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
