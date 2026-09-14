package com.toolsku.app.automation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ToolskuA11y"

        @Volatile
        var instance: AutomationAccessibilityService? = null
            private set
    }

    private lateinit var executor: TaskExecutor
    private var isRunning = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        executor = TaskExecutor(this, this)
        Log.i(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg in OemProfile.settingsPackages) {
                    val cls = event.className?.toString() ?: ""
                    Log.d(TAG, "Settings window: $pkg / $cls")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        executor.cleanup()
        Log.i(TAG, "Accessibility Service destroyed")
    }

    /**
     * Jalankan queue otomasi.
     * Dipanggil dari Activity (mis. KillerActivity).
     */
    fun runQueue(
        tasks: List<AutomationTask>,
        onProgress: ((current: Int, total: Int, pkg: String) -> Unit)? = null,
        onComplete: ((QueueStats) -> Unit)? = null
    ): Boolean {
        if (isRunning) {
            Log.w(TAG, "Queue already running")
            return false
        }

        isRunning = true
        TaskQueue.start(tasks)

        TaskQueue.onProgress = onProgress

        // Proses task berikutnya
        processNextTask(onComplete)

        return true
    }

    private fun processNextTask(onComplete: ((QueueStats) -> Unit)?) {
        val task = TaskQueue.next()
        if (task == null) {
            // Semua selesai
            val stats = TaskQueue.getStats()
            isRunning = false
            Log.i(TAG, "All tasks complete: $stats")
            onComplete?.invoke(stats)
            return
        }

        executor.execute(task) { result ->
            TaskQueue.completeCurrent(result)
            // Delay kecil antar task
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                processNextTask(onComplete)
            }, 500)
        }
    }

    fun cancelQueue() {
        TaskQueue.cancelAll()
        isRunning = false
        executor.cleanup()
    }
}
