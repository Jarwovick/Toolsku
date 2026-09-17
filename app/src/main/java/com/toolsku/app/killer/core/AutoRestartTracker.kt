package com.toolsku.app.killer.core

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.toolsku.app.core.AppTracker
import com.toolsku.app.core.Prefs
import com.toolsku.app.core.db.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tracker untuk auto-restart apps.
 * Tiru dari `xy` Baxa.
 *
 * Setelah app di-kill:
 * 1. Tunggu 5 detik
 * 2. Cek apakah app muncul di accessibility event
 * 3. Kalau muncul → markAutoRestarted
 * 4. Kalau tidak → biarkan (app mati)
 */
object AutoRestartTracker {

    private const val TAG = "AutoRestartTracker"
    private const val WAIT_AFTER_KILL_MS = 5000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())

    /**
     * App yang baru di-kill — menunggu verifikasi.
     */
    private val pendingVerification = mutableSetOf<String>()

    /**
     * App yang terdeteksi restart.
     */
    private val restartedApps = mutableSetOf<String>()

    /**
     * Mark app yang baru di-kill.
     * Setelah 5 detik, verify apakah restart.
     */
    fun markKilled(packageName: String) {
        Log.d(TAG, "Marked killed: $packageName")
        pendingVerification.add(packageName)

        // Schedule verification
        handler.postDelayed({
            verifyRestart(packageName)
        }, WAIT_AFTER_KILL_MS)
    }

    /**
     * Mark app yang baru di-kill (batch).
     */
    fun markKilledBatch(packageNames: List<String>) {
        packageNames.forEach { markKilled(it) }
    }

    /**
     * Cek apakah app restart setelah kill.
     */
    private fun verifyRestart(packageName: String) {
        scope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                val entity = dao.getApp(packageName)
                
                if (entity == null) {
                    Log.d(TAG, "App removed from DB: $packageName")
                    pendingVerification.remove(packageName)
                    return@launch
                }

                // Cek isClosed
                if (!entity.isClosed) {
                    // App dibuka lagi → auto-restart
                    Log.i(TAG, "Auto-restart detected: $packageName")
                    dao.markAutoRestarted(packageName)
                    restartedApps.add(packageName)
                } else {
                    Log.d(TAG, "App remains closed: $packageName")
                }

                pendingVerification.remove(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Verification failed: $packageName", e)
            }
        }
    }

    /**
     * Dipanggil saat app dibuka (dari AppTracker).
     * Reset autoRestart flag.
     */
    fun onAppOpened(packageName: String) {
        scope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                val entity = dao.getApp(packageName)
                
                if (entity != null && entity.isAutoRestarted) {
                    Log.i(TAG, "App opened manually, reset auto-restart: $packageName")
                    // Reset isAutoRestarted
                    dao.upsert(entity.copy(isAutoRestarted = false))
                    restartedApps.remove(packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset auto-restart: $packageName", e)
            }
        }
    }

    /**
     * Mark app sebagai unclosable.
     */
    fun markUnclosable(packageName: String) {
        scope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                dao.markUnclosable(packageName)
                Log.i(TAG, "Marked unclosable: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark unclosable: $packageName", e)
            }
        }
    }

    /**
     * Cek apakah app pending verification.
     */
    fun isPendingVerification(packageName: String): Boolean {
        return pendingVerification.contains(packageName)
    }

    /**
     * Cek apakah app sudah restart.
     */
    fun isRestarted(packageName: String): Boolean {
        return restartedApps.contains(packageName)
    }

    /**
     * Clear semua pending (untuk cancel).
     */
    fun clearPending() {
        pendingVerification.clear()
        restartedApps.clear()
    }
}
