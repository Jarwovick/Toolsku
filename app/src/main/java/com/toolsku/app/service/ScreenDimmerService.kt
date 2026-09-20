package com.toolsku.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager

class ScreenDimmerService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        var isRunning = false
        var currentAlpha = 0.5f // Default 50%

        fun updateBrightness(context: Context, percentage: Int) {
            currentAlpha = (100 - percentage) / 100f // 0% paling redup (overlay gelap), 100% paling terang
            val intent = Intent(context, ScreenDimmerService::class.java).apply {
                action = "UPDATE_ALPHA"
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_ALPHA") {
            overlayView?.alpha = currentAlpha
            return START_STICKY
        }

        if (!isRunning) {
            showOverlay()
            isRunning = true
        }

        return START_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = currentAlpha
        }

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
        }
        isRunning = false
    }
}
