package com.toolsku.app.killer.core

/**
 * State untuk UI — tiru dari `v1` Baxa.
 */
data class TaskState(
    val isRunning: Boolean = false,
    val totalApps: Int = 0,
    val currentAppName: String = "",
    val currentPackage: String = "",
    val progress: Float = 0f,
    val isComplete: Boolean = false,
    val cancelledReason: CancelReason = CancelReason.NONE
) {
    companion object {
        fun idle(): TaskState = TaskState()
        fun running(total: Int, current: String): TaskState =
            TaskState(isRunning = true, totalApps = total, currentAppName = current)
        fun completed(): TaskState = TaskState(isComplete = true)
    }
}

enum class CancelReason {
    NONE,
    USER_CANCELLED,
    HOME_BUTTON,
    SCREEN_OFF,
    ACCESSIBILITY_ERROR
}
