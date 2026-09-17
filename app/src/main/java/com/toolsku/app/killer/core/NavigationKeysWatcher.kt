package com.toolsku.app.killer.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

class NavigationKeysWatcher(
    private val context: Context,
    private val onHomePressed: () -> Unit,
    private val onRecentPressed: () -> Unit = {}
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "NavigationKeysWatcher"
        private const val REASON_KEY = "reason"
        private const val REASON_HOME = "homekey"
        private const val REASON_RECENT = "recentapps"
    }

    private var isRegistered = false

    fun start() {
        if (isRegistered) return
        try {
            val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
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
        if (intent == null) return
        if (intent.action != Intent.ACTION_CLOSE_SYSTEM_DIALOGS) return

        val reason = intent.getStringExtra(REASON_KEY) ?: return
        when (reason) {
            REASON_HOME -> onHomePressed()
            REASON_RECENT -> onRecentPressed()
        }
    }
}
