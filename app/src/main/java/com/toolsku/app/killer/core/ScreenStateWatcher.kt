package com.toolsku.app.killer.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class ScreenStateWatcher(
    private val context: Context,
    private val onScreenOff: () -> Unit
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateWatcher"
    }

    private var isRegistered = false

    fun start() {
        if (isRegistered) return
        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            context.registerReceiver(this, filter)
            isRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start", e)
        }
    }

    fun stop() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(this)
            isRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop", e)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
            onScreenOff()
        }
    }
}
