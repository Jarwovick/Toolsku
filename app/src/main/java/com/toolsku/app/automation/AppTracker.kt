package com.toolsku.app.automation

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.toolsku.app.core.Prefs
import com.toolsku.app.core.SystemApps
import com.toolsku.app.core.db.DatabaseProvider
import com.toolsku.app.core.db.RunningAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tracker untuk app yang dibuka/ditutup.
 * Basis data: Room database.
 */
object AppTracker {

    private const val TAG = "AppTracker"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Track app yang baru dibuka.
     * Dipanggil dari Accessibility Service.
     */
    fun trackAppOpened(context: Context, packageName: String) {
        if (shouldSkip(context, packageName)) return

        scope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                val existing = dao.getApp(packageName)

                if (existing == null) {
                    // App baru — insert
                    dao.upsert(
                        RunningAppEntity(
                            packageName = packageName,
                            lastUsed = System.currentTimeMillis(),
                            isClosed = false
                        )
                    )
                    Log.d(TAG, "New app: $packageName")
                } else {
                    // App sudah ada — update timestamp + reset isClosed
                    dao.upsert(
                        existing.copy(
                            lastUsed = System.currentTimeMillis(),
                            isClosed = false
                        )
                    )
                    Log.d(TAG, "Updated app: $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track: $packageName", e)
            }
        }
    }

    /**
     * Track app yang ditutup (user kembali ke launcher).
     */
    fun trackAppClosed(packageName: String) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().markClosed(packageName)
                Log.d(TAG, "Closed app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark closed: $packageName", e)
            }
        }
    }

    /**
     * Track app yang auto-restart setelah di-kill.
     */
    fun trackAutoRestart(packageName: String) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().markAutoRestarted(packageName)
                Log.d(TAG, "Auto-restarted app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark auto-restart: $packageName", e)
            }
        }
    }

    /**
     * Track app yang tidak bisa di-kill.
     */
    fun trackUnclosable(packageName: String) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().markUnclosable(packageName)
                Log.d(TAG, "Unclosable app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark unclosable: $packageName", e)
            }
        }
    }

    /**
     * Reset flag setelah kill — biar app muncul lagi kalau dibuka kembali.
     */
    fun resetAfterKill(packages: List<String>) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().resetFlags(packages)
                Log.d(TAG, "Reset flags for ${packages.size} apps")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset flags", e)
            }
        }
    }

    /**
     * Cleanup — hapus app yang lama tidak dipakai.
     */
    fun cleanup() {
        scope.launch {
            try {
                val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)  // 24 jam
                DatabaseProvider.runningAppDao().deleteOlderThan(cutoff)
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup failed", e)
            }
        }
    }

    /**
     * Cek apakah app harus di-skip.
     */
    private fun shouldSkip(context: Context, packageName: String): Boolean {
        // Skip app sendiri
        if (packageName == context.packageName) return true

        // Skip system UI, launcher, keyboard
        if (packageName in SKIP_PACKAGES) return true

        // Skip launcher
        if (packageName.contains("launcher")) return true

        // Skip app yang di-exception
        if (Prefs.isException(packageName)) return true

        // Skip app system yang dangerous
        if (SystemApps.isDangerousSystemApp(packageName)) return true

        // Cek apakah app punya launch intent (bukan service)
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            launchIntent == null  // skip kalau tidak punya launch intent
        } catch (e: PackageManager.NameNotFoundException) {
            true  // skip kalau tidak terinstall
        }
    }

    private val SKIP_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.providers.settings",
        "com.android.shell"
    )
}
