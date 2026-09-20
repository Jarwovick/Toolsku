package com.toolsku.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class ScreenDimmerService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        var isRunning = false
        var currentAlpha = 0.5f // Default 50% kegelapan

        fun updateBrightness(context: Context, percentage: Int) {
            // Skala kegelapan: 0% = bening, 100% = hitam pekat (dibatasi maks 0.85f agar layar tidak mati total)
            val calculatedAlpha = (percentage / 100f) * 0.85f
            currentAlpha = calculatedAlpha

            val intent = Intent(context, ScreenDimmerService::class.java).apply {
                action = "UPDATE_ALPHA"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Jalankan Foreground Notification wajib untuk Android modern
        val notification = NotificationCompat.Builder(this, "dimmer_channel")
            .setContentTitle("Peredupan Layar Aktif")
            .setContentText("Toolsku sedang meredupkan layar")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        if (intent?.action == "UPDATE_ALPHA") {
            overlayView?.alpha = currentAlpha
        } else if (!isRunning) {
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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "dimmer_channel",
                "Layanan Peredup Layar",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isRunning = false
    }
}
