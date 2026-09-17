package com.toolsku.app.killer.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * Watcher untuk navigation keys.
 * Tiru dari `tl1` Baxa.
 *
 * Detect Home key / Recent Apps key.
 */
class NavigationKeysWatcher(
    private val context: Context,
    private val onHomePressed: () -> Unit,
    private val onRecentPressed: () -> Unit = {}
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "NavigationKeysWatcher"
        private const val SYSTEM_DIALOG_REASON_KEY = "reason"
        private const val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"
        private const val SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps"
    }

    private var isRegistered = false

    /**
     * Start watching.
     */
    fun start() {
        if (isRegistered) return
        
        try {
            val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            context.registerReceiver(this, filter)
            isRegistered = true
            Log.d(TAG, "Started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start", e)
        }
    }

    /**
     * Stop watching.
     */
    fun stop() {
        if (!isRegistered) return
        
        try {
            context.unregisterReceiver(this)
            isRegistered = false
            Log.d(TAG, "Stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop", e)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        
        val action = intent.action ?: return
        if (action != Intent.ACTION_CLOSE_SYSTEM_DIALOGS) return
        
        val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY) ?: return
        
        when (reason) {
            SYSTEM_DIALOG_REASON_HOME_KEY -> {
                Log.d(TAG, "Home pressed")
                onHomePressed()
            }
            SYSTEM_DIALOG_REASON_RECENT_APPS -> {
                Log.d(TAG, "Recent apps pressed")
                onRecentPressed()
            }
        }
    }
}
