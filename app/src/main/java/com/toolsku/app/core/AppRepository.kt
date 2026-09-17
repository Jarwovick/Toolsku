package com.toolsku.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.toolsku.app.core.db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepository {

    private const val TAG = "AppRepo"

    /**
     * Ambil daftar SEMUA app terinstall (user + system).
     */
    suspend fun getInstalledApps(context: Context, includeSystem: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val result = mutableListOf<AppInfo>()
            val ourPackage = context.packageName

            for (app in packages) {
                if (app.packageName == ourPackage) continue

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (SystemApps.isDangerousSystemApp(app.packageName)) continue
                if (!includeSystem && isSystem) continue
                if (includeSystem && !isSystem) continue

                result.add(
                    AppInfo(
                        packageName = app.packageName,
                        label = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                        isSystem = isSystem,
                        isException = Prefs.isException(app.packageName)
                    )
                )
            }

            result.sortBy { it.label.lowercase() }
            result
        }

    /**
     * Ambil daftar app di exception list.
     */
    suspend fun getExceptionApps(context: Context): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val exceptionPackages = Prefs.exceptionList
            val result = mutableListOf<AppInfo>()

            Log.i(TAG, "getExceptionApps: ${exceptionPackages.size} packages in Prefs")

            for (pkg in exceptionPackages) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    val label = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        pkg
                    }

                    result.add(
                        AppInfo(
                            packageName = pkg,
                            label = label,
                            icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                            isSystem = isSystem,
                            isException = true
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "SKIP (not installed): $pkg")
                }
            }

            result.sortBy { it.label.lowercase() }
            Log.i(TAG, "getExceptionApps result: ${result.size}")
            result
        }

    /**
     * Ambil semua app untuk dialog SELECT APPS.
     */
    suspend fun getAllApps(context: Context, onlyUser: Boolean = false): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val result = mutableListOf<AppInfo>()
            val ourPackage = context.packageName

            for (app in packages) {
                if (app.packageName == ourPackage) continue

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (onlyUser && isSystem) continue

                result.add(
                    AppInfo(
                        packageName = app.packageName,
                        label = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                        isSystem = isSystem,
                        isException = Prefs.isException(app.packageName)
                    )
                )
            }

            result.sortBy { it.label.lowercase() }
            result
        }

    /**
     * Ambil system apps yang RUNNING dari DB.
     * Logic SAMA dengan getRunningApps (user apps).
     * TIDAK ada fallback.
     */
    suspend fun getSafeSystemApps(context: Context): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val result = mutableListOf<AppInfo>()

            try {
                val dao = DatabaseProvider.runningAppDao()
                val entities = dao.getRunningSystemApps()

                Log.i(TAG, "DB system apps: ${entities.size}")

                for (entity in entities) {
                    if (Prefs.isException(entity.packageName)) continue

                    try {
                        val appInfo = pm.getApplicationInfo(entity.packageName, 0)
                        result.add(
                            AppInfo(
                                packageName = entity.packageName,
                                label = pm.getApplicationLabel(appInfo).toString(),
                                icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                                isSystem = true,
                                isException = false
                            )
                        )
                    } catch (e: Exception) {
                        // App sudah uninstall — hapus dari DB
                        dao.delete(entity.packageName)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get system apps", e)
            }

            result.sortBy { it.label.lowercase() }
            Log.i(TAG, "Final system apps: ${result.size}")
            result
        }

    /**
     * Ambil USER apps yang RUNNING dari DB.
     */
    suspend fun getRunningApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val result = mutableListOf<AppInfo>()

        try {
            val dao = DatabaseProvider.runningAppDao()
            val entities = dao.getRunningUserApps()

            Log.i(TAG, "DB user apps: ${entities.size}")

            for (entity in entities) {
                if (SystemApps.isDangerousSystemApp(entity.packageName)) continue
                if (Prefs.isException(entity.packageName)) continue

                try {
                    val appInfo = pm.getApplicationInfo(entity.packageName, 0)
                    result.add(
                        AppInfo(
                            packageName = entity.packageName,
                            label = pm.getApplicationLabel(appInfo).toString(),
                            icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                            isSystem = false,
                            isException = false
                        )
                    )
                } catch (e: Exception) {
                    // App sudah uninstall — hapus dari DB
                    dao.delete(entity.packageName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get running apps from DB", e)
        }

        result.sortBy { it.label.lowercase() }
        Log.i(TAG, "Final user running apps: ${result.size}")
        result
    }

    /**
     * Kill app pakai killBackgroundProcesses().
     */
    fun killWithBackgroundProcesses(context: Context, packageName: String): Boolean {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(packageName)
            true
        } catch (e: Exception) {
            false
        }
    }
}
