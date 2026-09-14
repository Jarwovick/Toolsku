package com.toolsku.app.dimmer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.toolsku.app.MainActivity
import com.toolsku.app.R

class DimmerService : Service() {

    companion object {
        const val ACTION_START = "com.toolsku.app.dimmer.START"
        const val ACTION_STOP = "com.toolsku.app.dimmer.STOP"
        const val ACTION_UPDATE = "com.toolsku.app.dimmer.UPDATE"
        const val EXTRA_LEVEL = "level"

        private const val CHANNEL_ID = "toolsku_dimmer"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var overlayManager: DimmerOverlayManager

    override fun onCreate() {
        super.onCreate()
        overlayManager = DimmerOverlayManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val level = intent.getIntExtra(EXTRA_LEVEL, 50)
                startForeground(NOTIFICATION_ID, buildNotification())
                // Level = tingkat kecerahan (0 = gelap penuh, 100 = terang)
                // Alpha overlay = kebalikannya (0 = transparan, 1 = hitam penuh)
                overlayManager.show((100 - level) / 100f)
            }
            ACTION_UPDATE -> {
                val level = intent.getIntExtra(EXTRA_LEVEL, 50)
                overlayManager.update((100 - level) / 100f)
            }
            ACTION_STOP -> {
                overlayManager.hide()
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

    override fun onDestroy() {
        overlayManager.hide()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.dimmer_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.dimmer_channel_desc)
                setShowBadge(false)
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
            .setContentTitle(getString(R.string.dimmer_notification_title))
            .setContentText(getString(R.string.dimmer_notification_text))
            .setSmallIcon(R.drawable.ic_dimmer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
