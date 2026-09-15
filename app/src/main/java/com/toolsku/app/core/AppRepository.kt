package com.toolsku.app.core

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepository {

    /**
     * Ambil daftar SEMUA user app terinstall.
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
     * Ambil daftar system apps yang AMAN di-kill.
     * (galeri, kamera, browser, dll)
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
     * Ambil daftar app RUNNING (user apps).
     * Pakai getRunningAppProcesses, fallback UsageStats.
     */
    suspend fun getRunningApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val ourPackage = context.packageName
        val result = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()

        val runningProcesses = try {
            am.runningAppProcesses
        } catch (e: Exception) {
            null
        }

        if (runningProcesses != null && runningProcesses.size > 1) {
            for (process in runningProcesses) {
                if (process.pkgList == null) continue
                val pkg = process.pkgList.firstOrNull() ?: process.processName ?: continue

                if (pkg == ourPackage) continue
                if (seen.contains(pkg)) continue
                if (SystemApps.isDangerousSystemApp(pkg)) continue
                if (Prefs.isException(pkg)) continue

                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (isSystem) continue

                    seen.add(pkg)
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
                    // Skip
                }
            }
            return@withContext result.sortedBy { it.label.lowercase() }
        }

        // Fallback UsageStats 5 menit
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - (5 * 60 * 1000L)

        val usageStats = try {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        } catch (e: Exception) {
            null
        } ?: return@withContext emptyList()

        for (stat in usageStats) {
            val pkg = stat.packageName ?: continue
            if (pkg == ourPackage) continue
            if (seen.contains(pkg)) continue
            if (SystemApps.isDangerousSystemApp(pkg)) continue
            if (Prefs.isException(pkg)) continue

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem) continue

                val timeSinceLastUse = endTime - stat.lastTimeUsed
                if (timeSinceLastUse > 5 * 60 * 1000L) continue

                seen.add(pkg)
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
                // Skip
            }
        }

        result.sortBy { it.label.lowercase() }
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
