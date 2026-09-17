package com.toolsku.app.killer.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
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
 * Overlay khusus Killer.
 * Tiru dari `z90` Baxa.
 */
class KillerOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "KillerOverlay"
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val handler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var progressCircle: ProgressBar? = null
    private var progressBar: ProgressBar? = null
    private var tvPercent: TextView? = null
    private var tvAppName: TextView? = null
    private var tvTitle: TextView? = null
    private var tvSubtitle: TextView? = null

    @Volatile
    private var isShowing: Boolean = false

    var onStopClick: (() -> Unit)? = null

    fun show() {
        if (isShowing) {
            Log.d(TAG, "Already showing")
            return
        }

        try {
            val inflater = LayoutInflater.from(context)
            overlayView = inflater.inflate(R.layout.overlay_blocker, null)

            progressCircle = overlayView?.findViewById(R.id.progressCircle)
            progressBar = overlayView?.findViewById(R.id.progressBar)
            tvPercent = overlayView?.findViewById(R.id.tvPercent)
            tvAppName = overlayView?.findViewById(R.id.tvAppName)
            tvTitle = overlayView?.findViewById(R.id.tvTitle)
            tvSubtitle = overlayView?.findViewById(R.id.tvSubtitle)

            tvTitle?.text = "CLOSING APPS"
            tvSubtitle?.text = "SHUTTING DOWN"

            overlayView?.findViewById<FrameLayout>(R.id.btnStop)?.setOnClickListener {
                Log.i(TAG, "Stop clicked")
                onStopClick?.invoke()
            }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            windowManager.addView(overlayView, params)
            isShowing = true

            Log.i(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show", e)
            overlayView = null
            isShowing = false
        }
    }

    fun update(current: Int, total: Int, appLabel: String) {
        if (!isShowing) return

        val percent = if (total > 0) ((current.toFloat() / total) * 100).toInt() else 0

        handler.post {
            try {
                progressCircle?.progress = percent
                progressBar?.progress = percent
                tvPercent?.text = percent.toString()
                if (appLabel.isNotEmpty()) {
                    tvAppName?.text = appLabel
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update", e)
            }
        }
    }

    fun hide() {
        if (!isShowing) return

        try {
            overlayView?.let { view ->
                if (view.parent != null) {
                    windowManager.removeView(view)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove", e)
        }

        overlayView = null
        progressCircle = null
        progressBar = null
        tvPercent = null
        tvAppName = null
        tvTitle = null
        tvSubtitle = null
        isShowing = false

        Log.i(TAG, "Overlay hidden")
    }

    fun isOverlayShowing(): Boolean = isShowing
}
