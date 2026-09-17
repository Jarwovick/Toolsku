package com.toolsku.app.killer.core

/**
 * State untuk UI — tiru dari `v1` Baxa.
 *
 * ⚠️ CATATAN: CancelReason TIDAK dideklarasikan di sini.
 * Ada di file terpisah: `CancelReason.kt`.
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
            TaskState(
                isRunning = true,
                totalApps = total,
                currentAppName = current
            )

        fun completed(): TaskState =
            TaskState(isComplete = true)
    }
}
