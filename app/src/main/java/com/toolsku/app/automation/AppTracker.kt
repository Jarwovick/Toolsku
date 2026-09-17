package com.toolsku.app.automation

import android.content.Context
import android.content.pm.ApplicationInfo
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

object AppTracker {

    private const val TAG = "AppTracker"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Track app yang baru dibuka (user + system).
     */
    fun trackAppOpened(context: Context, packageName: String) {
        if (shouldSkip(context, packageName)) return

        scope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                val isSystem = isSystemApp(context, packageName)

                val existing = dao.getApp(packageName)

                if (existing == null) {
                    dao.upsert(
                        RunningAppEntity(
                            packageName = packageName,
                            lastUsed = System.currentTimeMillis(),
                            isSystem = isSystem,
                            isClosed = false
                        )
                    )
                    Log.d(TAG, "New ${if (isSystem) "system" else "user"} app: $packageName")
                } else {
                    // Reset isClosed ketika app dibuka lagi
                    dao.upsert(
                        existing.copy(
                            lastUsed = System.currentTimeMillis(),
                            isClosed = false
                        )
                    )
                    Log.d(TAG, "Updated app: $packageName (isClosed reset)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track: $packageName", e)
            }
        }
    }

    fun trackAppClosed(packageName: String) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().markClosed(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark closed: $packageName", e)
            }
        }
    }

    fun trackAutoRestart(packageName: String) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().markAutoRestarted(packageName)
                Log.d(TAG, "Auto-restarted: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark auto-restart: $packageName", e)
            }
        }
    }

    fun trackUnclosable(packageName: String) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().markUnclosable(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark unclosable: $packageName", e)
            }
        }
    }

    fun resetAfterKill(packages: List<String>) {
        scope.launch {
            try {
                DatabaseProvider.runningAppDao().resetFlags(packages)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset flags", e)
            }
        }
    }

    fun cleanup() {
        scope.launch {
            try {
                val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                DatabaseProvider.runningAppDao().deleteOlderThan(cutoff)
            } catch (e: Exception) {
                Log.e(TAG, "Cleanup failed", e)
            }
        }
    }

    private fun isSystemApp(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cek apakah app harus di-skip.
     * System apps BOLEH di-track kalau punya launcher (bisa dibuka user).
     */
    private fun shouldSkip(context: Context, packageName: String): Boolean {
        // Skip app sendiri
        if (packageName == context.packageName) return true

        // Skip system UI, launcher, dll
        if (packageName in SKIP_PACKAGES) return true

        // Skip launcher
        if (packageName.contains("launcher")) return true

        // Skip keyboard
        if (packageName.contains("inputmethod")) return true

        // Skip app yang di-exception
        if (Prefs.isException(packageName)) return true

        // Skip DANGEROUS system apps
        if (SystemApps.isDangerousSystemApp(packageName)) return true

        // Cek launch intent — kalau TIDAK punya, skip
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            launchIntent == null
        } catch (e: PackageManager.NameNotFoundException) {
            true
        }
    }

    private val SKIP_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.providers.settings",
        "com.android.shell",
        "com.android.providers.media",
        "com.android.providers.contacts",
        "com.android.providers.telephony",
        "com.android.providers.calendar",
        "com.android.providers.downloads"
    )
}
