package com.toolsku.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepository {

    private const val TAG = "AppRepo"

    /**
     * Ambil daftar SEMUA app terinstall.
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
     * Ambil daftar app yang BENAR-BENAR RUNNING.
     *
     * Pakai:
     * 1. getRunningAppProcesses() — utama
     * 2. getRunningServices() — tambahan
     *
     * TIDAK pakai UsageStatsManager (Baxa tidak pakai).
     */
    suspend fun getRunningApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val ourPackage = context.packageName
        val runningPackages = mutableSetOf<String>()

        // ===== Metode 1: getRunningAppProcesses =====
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = am.runningAppProcesses
            if (processes != null) {
                for (process in processes) {
                    if (process.pkgList == null) continue
                    val pkg = process.pkgList.firstOrNull() ?: process.processName ?: continue
                    runningPackages.add(pkg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRunningAppProcesses failed", e)
        }

        // ===== Metode 2: getRunningServices =====
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val services = am.getRunningServices(100)
            for (service in services) {
                runningPackages.add(service.service.packageName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRunningServices failed", e)
        }

        // ===== Filter hasil =====
        val result = mutableListOf<AppInfo>()
        for (pkg in runningPackages) {
            if (pkg == ourPackage) continue
            if (SystemApps.isSystemApp(pkg)) continue
            if (Prefs.isException(pkg)) continue

            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
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
                // App sudah tidak terinstall
            }
        }

        result.sortBy { it.label.lowercase() }
        Log.i(TAG, "Final running apps: ${result.size}")
        result
    }

    /**
     * Cek apakah package tertentu sedang running.
     * Dipakai untuk verifikasi setelah kill.
     */
    suspend fun isPackageRunning(context: Context, packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                // Cek via processes
                val processes = am.runningAppProcesses
                if (processes != null) {
                    for (process in processes) {
                        if (process.pkgList?.contains(packageName) == true) return@withContext true
                        if (process.processName == packageName) return@withContext true
                    }
                }

                // Cek via services
                @Suppress("DEPRECATION")
                val services = am.getRunningServices(100)
                for (service in services) {
                    if (service.service.packageName == packageName) return@withContext true
                }

                false
            } catch (e: Exception) {
                false
            }
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
