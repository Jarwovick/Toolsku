package com.toolsku.app.killer.core

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.toolsku.app.core.AppTracker
import com.toolsku.app.core.db.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tracker untuk auto-restart apps.
 * Tiru dari `xy` Baxa.
 */
object AutoRestartTracker {

    private const val TAG = "AutoRestartTracker"
    private const val WAIT_AFTER_KILL_MS = 5000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    private val pendingVerification = mutableSetOf<String>()
    private val restartedApps = mutableSetOf<String>()

    fun markKilled(packageName: String) {
        Log.d(TAG, "Marked killed: $packageName")
        pendingVerification.add(packageName)

        handler.postDelayed({
            verifyRestart(packageName)
        }, WAIT_AFTER_KILL_MS)
    }

    private fun verifyRestart(packageName: String) {
        scope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                val entity = dao.getApp(packageName)

                if (entity == null) {
                    pendingVerification.remove(packageName)
                    return@launch
                }

                if (!entity.isClosed) {
                    Log.i(TAG, "Auto-restart detected: $packageName")
                    dao.markAutoRestarted(packageName)
                    restartedApps.add(packageName)
                }

                pendingVerification.remove(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Verification failed: $packageName", e)
            }
        }
    }

    fun onAppOpened(packageName: String) {
        scope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                val entity = dao.getApp(packageName)

                if (entity != null && entity.isAutoRestarted) {
                    Log.i(TAG, "Reset auto-restart: $packageName")
                    dao.upsert(entity.copy(isAutoRestarted = false))
                    restartedApps.remove(packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset: $packageName", e)
            }
        }
    }
}
