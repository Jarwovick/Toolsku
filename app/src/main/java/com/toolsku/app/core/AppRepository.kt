package com.toolsku.app.core

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepository {

    private const val TAG = "AppRepo"

    // Threshold "recently used" untuk fallback
    private const val USAGE_THRESHOLD_MS = 5 * 60 * 1000L  // 5 menit

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

            for (pkg in exceptionPackages) {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    result.add(
                        AppInfo(
                            packageName = pkg,
                            label = pm.getApplicationLabel(appInfo).toString(),
                            icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                            isSystem = isSystem,
                            isException = true
                        )
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Exception app not found: $pkg")
                }
            }

            result.sortBy { it.label.lowercase() }
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
                if (SystemApps.isDangerousSystemApp(app.packageName)) continue

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
     * Ambil safe system apps.
     */
    suspend fun getSafeSystemApps(context: Context): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val result = mutableListOf<AppInfo>()
            val ourPackage = context.packageName

            for (app in packages) {
                if (app.packageName == ourPackage) continue
                if (!SystemApps.isSafeSystemApp(app.packageName)) continue
                if (Prefs.isException(app.packageName)) continue

                result.add(
                    AppInfo(
                        packageName = app.packageName,
                        label = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                        isSystem = true,
                        isException = false
                    )
                )
            }

            result.sortBy { it.label.lowercase() }
            result
        }

    /**
     * Ambil daftar app RUNNING (3 metode).
     *
     * 1. getRunningAppProcesses() — app foreground
     * 2. getRunningServices() — app dengan service aktif
     * 3. UsageStatsManager (5 menit) — app yang baru dipakai
     */
    suspend fun getRunningApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val ourPackage = context.packageName
        val runningPackages = mutableSetOf<String>()

        // === 1. getRunningAppProcesses ===
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = am.runningAppProcesses
            if (processes != null) {
                for (process in processes) {
                    if (process.pkgList == null) continue
                    for (pkg in process.pkgList) {
                        if (pkg != ourPackage) runningPackages.add(pkg)
                    }
                }
            }
            Log.d(TAG, "Method 1 (Processes): ${runningPackages.size}")
        } catch (e: Exception) {
            Log.e(TAG, "getRunningAppProcesses failed", e)
        }

        // === 2. getRunningServices ===
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val services = am.getRunningServices(200)
            for (service in services) {
                val pkg = service.service.packageName
                if (pkg != ourPackage) runningPackages.add(pkg)
            }
            Log.d(TAG, "Method 2 (Services): ${runningPackages.size}")
        } catch (e: Exception) {
            Log.e(TAG, "getRunningServices failed", e)
        }

        // === 3. UsageStatsManager (5 menit) ===
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - USAGE_THRESHOLD_MS

            val usageStats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )
            if (usageStats != null) {
                for (stat in usageStats) {
                    val pkg = stat.packageName ?: continue
                    val timeSinceLastUse = endTime - stat.lastTimeUsed
                    if (timeSinceLastUse <= USAGE_THRESHOLD_MS && pkg != ourPackage) {
                        runningPackages.add(pkg)
                    }
                }
            }
            Log.d(TAG, "Method 3 (UsageStats): ${runningPackages.size}")
        } catch (e: Exception) {
            Log.e(TAG, "UsageStats failed", e)
        }

        // === Convert ke AppInfo ===
        val result = mutableListOf<AppInfo>()
        for (pkg in runningPackages) {
            if (pkg == ourPackage) continue
            if (SystemApps.isDangerousSystemApp(pkg)) continue
            if (Prefs.isException(pkg)) continue

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                // Skip system apps dari running list — mereka di-handle oleh getSafeSystemApps
                if (isSystem) continue

                result.add(
                    AppInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                        isSystem = false,
                        isException = false
                    )
                )
            } catch (e: Exception) {
                // App sudah uninstall
            }
        }

        result.sortBy { it.label.lowercase() }
        Log.i(TAG, "Final running apps: ${result.size}")
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
