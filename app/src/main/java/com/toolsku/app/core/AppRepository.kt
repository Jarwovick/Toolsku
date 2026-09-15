package com.toolsku.app.core

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepository {

    /**
     * Ambil daftar SEMUA app terinstall.
     * Dipakai untuk halaman Cleaner (daftar semua app).
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
                if (SystemApps.isSystemApp(app.packageName)) continue
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
     * Ambil daftar app yang BENAR-BENAR running.
     *
     * Pakai UsageStatsManager — app yang punya aktivitas dalam X milidetik terakhir.
     * Untuk "masih running", pakai threshold kecil (mis. 1000ms = 1 detik terakhir).
     *
     * CATATAN: Butuh izin PACKAGE_USAGE_STATS.
     */
    suspend fun getRunningApps(
        context: Context,
        withinMillis: Long = 1000L
    ): List<AppInfo> = withContext(Dispatchers.IO) {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - withinMillis

        // Query usage stats
        val usageStats = try {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        } catch (e: Exception) {
            null
        } ?: return@withContext emptyList()

        val pm = context.packageManager
        val ourPackage = context.packageName
        val result = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()

        for (stat in usageStats) {
            val pkg = stat.packageName ?: continue
            if (pkg == ourPackage) continue
            if (seen.contains(pkg)) continue
            if (SystemApps.isSystemApp(pkg)) continue
            if (Prefs.isException(pkg)) continue

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                // Skip system apps — kita hanya tampilkan user apps yang running
                if (isSystem) continue

                // Cek apakah app benar-benar baru aktif
                // lastTimeUsed = terakhir dipakai
                val timeSinceLastUse = endTime - stat.lastTimeUsed
                if (timeSinceLastUse > withinMillis) continue

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
                // App sudah tidak terinstall
            }
        }

        result.sortBy { it.label.lowercase() }
        result
    }
}
