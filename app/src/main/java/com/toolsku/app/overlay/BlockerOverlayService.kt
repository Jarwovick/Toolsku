package com.toolsku.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import androidx.core.app.NotificationCompat
import com.toolsku.app.MainActivity
import com.toolsku.app.R

/**
 * Foreground service yang menampilkan overlay full-screen saat otomasi berjalan.
 * Foreground service dipakai supaya overlay tetap muncul di atas Settings/App Info.
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
        const val EXTRA_MODE = "mode"

        const val MODE_KILLER = "killer"
        const val MODE_CLEANER = "cleaner"

        private const val CHANNEL_ID = "toolsku_overlay"
        private const val NOTIFICATION_ID = 2001

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
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView

    private var currentMode: String = MODE_KILLER

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Mulai foreground service DULU — supaya tidak di-kill
        startForeground(NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            ACTION_SHOW -> {
                val current = intent.getIntExtra(EXTRA_CURRENT, 0)
                val total = intent.getIntExtra(EXTRA_TOTAL, 1)
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_KILLER
                currentMode = mode
                showOverlay(current, total, appName, mode)
            }
            ACTION_UPDATE -> {
                val current = intent.getIntExtra(EXTRA_CURRENT, 0)
                val total = intent.getIntExtra(EXTRA_TOTAL, 1)
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""

                // Kalau overlay sudah hilang, show ulang
                if (overlayView == null) {
                    Log.w(TAG, "Overlay null on update, re-showing")
                    showOverlay(current, total, appName, currentMode)
                } else {
                    updateOverlay(current, total, appName)
                }
            }
            ACTION_HIDE -> {
                hideOverlay()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    // ==== NOTIFICATION ====

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_dimmer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }

    // ==== OVERLAY ====

    private fun showOverlay(current: Int, total: Int, appName: String, mode: String) {
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

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

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
            isShowing = true

            updateOverlay(current, total, appName)

            Log.i(TAG, "Overlay shown (mode=$mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    private fun updateOverlay(current: Int, total: Int, appName: String) {
        if (overlayView == null) {
            Log.w(TAG, "Cannot update — overlay is null")
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
