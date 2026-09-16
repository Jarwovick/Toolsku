package com.toolsku.app.automation

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.toolsku.app.R

class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ToolskuA11y"

        @Volatile
        var instance: AutomationAccessibilityService? = null
            private set

        // Mode overlay
        const val MODE_KILLER = "killer"
        const val MODE_CLEANER = "cleaner"

        var onStopClick: (() -> Unit)? = null
    }

    private lateinit var executor: TaskExecutor
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // Overlay state
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var progressCircle: ProgressBar
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPercent: TextView
    private lateinit var tvAppName: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private var currentMode: String = MODE_KILLER

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        executor = TaskExecutor(this, this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
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
        hideOverlay()
        Log.i(TAG, "Accessibility Service destroyed")
    }

    // ==== QUEUE ====

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

        processNextTask(onComplete)
        return true
    }

    private fun processNextTask(onComplete: ((QueueStats) -> Unit)?) {
        val task = TaskQueue.next()
        if (task == null) {
            val stats = TaskQueue.getStats()
            isRunning = false
            Log.i(TAG, "All tasks complete: $stats")
            onComplete?.invoke(stats)
            return
        }

        executor.execute(task) { result ->
            TaskQueue.completeCurrent(result)
            handler.postDelayed({
                processNextTask(onComplete)
            }, 300)
        }
    }

    fun cancelQueue() {
        Log.i(TAG, "Cancel queue requested")
        TaskQueue.cancelAll()
        isRunning = false
        executor.cleanup()
    }

    // ==== OVERLAY (pakai TYPE_ACCESSIBILITY_OVERLAY) ====

    /**
     * Tampilkan overlay pakai TYPE_ACCESSIBILITY_OVERLAY.
     * Tipe ini bisa menutupi Settings/App Info — tidak seperti TYPE_APPLICATION_OVERLAY.
     */
    fun showOverlay(current: Int, total: Int, appName: String, mode: String) {
        currentMode = mode

        if (overlayView != null) {
            updateOverlay(current, total, appName)
            return
        }

        try {
            val inflater = LayoutInflater.from(this)
            overlayView = inflater.inflate(R.layout.overlay_blocker, null)

            progressCircle = overlayView!!.findViewById(R.id.progressCircle)
            progressBar = overlayView!!.findViewById(R.id.progressBar)
            tvPercent = overlayView!!.findViewById(R.id.tvPercent)
            tvAppName = overlayView!!.findViewById(R.id.tvAppName)
            tvTitle = overlayView!!.findViewById(R.id.tvTitle)
            tvSubtitle = overlayView!!.findViewById(R.id.tvSubtitle)

            if (mode == MODE_CLEANER) {
                tvTitle.text = getString(R.string.overlay_cleaning_title)
                tvSubtitle.text = getString(R.string.overlay_cleaning_subtitle)
            } else {
                tvTitle.text = getString(R.string.overlay_closing_apps)
                tvSubtitle.text = getString(R.string.overlay_shutting_down)
            }

            overlayView!!.findViewById<FrameLayout>(R.id.btnStop).setOnClickListener {
                onStopClick?.invoke()
            }

            // ⭐ KUNCI: pakai TYPE_ACCESSIBILITY_OVERLAY
            val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

            windowManager.addView(overlayView, params)

            updateOverlay(current, total, appName)

            Log.i(TAG, "Accessibility overlay shown (mode=$mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show accessibility overlay", e)
        }
    }

    fun updateOverlay(current: Int, total: Int, appName: String) {
        if (overlayView == null) {
            Log.w(TAG, "Overlay null — re-showing")
            showOverlay(current, total, appName, currentMode)
            return
        }

        val percent = if (total > 0) ((current.toFloat() / total) * 100).toInt() else 0

        handler.post {
            try {
                progressCircle.progress = percent
                progressBar.progress = percent
                tvPercent.text = percent.toString()
                if (appName.isNotEmpty()) {
                    tvAppName.text = appName
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay", e)
            }
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay", e)
            }
        }
        overlayView = null
    }
}
