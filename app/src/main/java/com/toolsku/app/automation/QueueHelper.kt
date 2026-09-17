package com.toolsku.app.automation

import android.util.Log

/**
 * Helper untuk menjalankan queue automation (untuk Cleaner & Shortcut).
 * Ini pakai API lama — tetap dipertahankan agar Cleaner & Shortcut jalan.
 */
object QueueHelper {

    private const val TAG = "QueueHelper"

    /**
     * Jalankan queue lama (untuk Cleaner & Shortcut).
     */
    fun runQueue(
        service: Any,
        tasks: List<AutomationTask>,
        onProgress: ((current: Int, total: Int, appLabel: String) -> Unit)? = null,
        onComplete: ((QueueStats) -> Unit)? = null
    ): Boolean {
        Log.i(TAG, "Running legacy queue: ${tasks.size} tasks")
        // TODO: Implementasi legacy queue untuk Cleaner & Shortcut
        // Sementara return false
        return false
    }
}
