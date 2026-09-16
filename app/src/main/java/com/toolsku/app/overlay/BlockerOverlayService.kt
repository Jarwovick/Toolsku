package com.toolsku.app.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.toolsku.app.R

/**
 * Service yang menampilkan overlay "CLOSING APPS" full-screen.
 * Menampilkan progress saat kill berjalan.
 */
class BlockerOverlayService : Service() {

    companion object {
        private const val TAG = "BlockerOverlay"

        const val ACTION_SHOW = "com.toolsku.app.overlay.SHOW"
        const val ACTION_UPDATE = "com.toolsku.app.overlay.UPDATE"
        const val ACTION_HIDE = "com.toolsku.app.overlay.HIDE"

        const val EXTRA_CURRENT = "current"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_APP_NAME = "app_name"

        /**
         * Callback statis untuk tombol stop.
         * Di-set oleh KillerActivity.
         */
        var onStopClick: (() -> Unit)? = null

        @Volatile
        var isShowing = false
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var progressCircle: ProgressBar
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPercent: TextView
    private lateinit var tvAppName: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val current = intent.getIntExtra(EXTRA_CURRENT, 0)
                val total = intent.getIntExtra(EXTRA_TOTAL, 1)
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                showOverlay(current, total, appName)
            }
            ACTION_UPDATE -> {
                val current = intent.getIntExtra(EXTRA_CURRENT, 0)
                val total = intent.getIntExtra(EXTRA_TOTAL, 1)
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                updateOverlay(current, total, appName)
            }
            ACTION_HIDE -> {
                hideOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    private fun showOverlay(current: Int, total: Int, appName: String) {
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

            // Setup stop button
            overlayView!!.findViewById<FrameLayout>(R.id.btnStop).setOnClickListener {
                onStopClick?.invoke()
            }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            windowManager.addView(overlayView, params)
            isShowing = true

            updateOverlay(current, total, appName)

            Log.i(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun updateOverlay(current: Int, total: Int, appName: String) {
        if (overlayView == null) return

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

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay", e)
            }
        }
        overlayView = null
        isShowing = false
    }
}
