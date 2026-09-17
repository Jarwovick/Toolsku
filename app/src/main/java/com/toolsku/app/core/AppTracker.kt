package com.toolsku.app.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.toolsku.app.core.db.DatabaseProvider
import com.toolsku.app.core.db.RunningAppEntity
import com.toolsku.app.killer.core.AutoRestartTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppTracker {

    private const val TAG = "AppTracker"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Track app yang baru dibuka.
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
                    Log.d(TAG, "New ${if (isSystem) "SYSTEM" else "USER"} app: $packageName")
                } else {
                    dao.upsert(
                        existing.copy(
                            lastUsed = System.currentTimeMillis(),
                            isClosed = false
                        )
                    )
                    Log.d(TAG, "Updated app: $packageName")
                }

                // ⭐ Notify auto-restart tracker
                AutoRestartTracker.onAppOpened(packageName)
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

    private fun shouldSkip(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName in SKIP_PACKAGES) return true
        if (packageName.contains("launcher")) return true
        if (packageName.contains("inputmethod")) return true
        if (Prefs.isException(packageName)) return true
        if (SystemApps.isDangerousSystemApp(packageName)) return true

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
        "com.android.providers.downloads",
        "com.android.providers.userdictionary",
        "com.android.providers.blockednumber"
    )
}
