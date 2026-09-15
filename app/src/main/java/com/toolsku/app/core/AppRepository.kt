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
     * HANYA pakai ActivityManager.getRunningAppProcesses().
     * Tidak pakai UsageStatsManager, tidak pakai getRunningServices.
     */
    suspend fun getRunningApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val ourPackage = context.packageName
        val result = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()

        val runningProcesses = try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.runningAppProcesses
        } catch (e: Exception) {
            Log.e(TAG, "getRunningAppProcesses failed", e)
            null
        }

        if (runningProcesses == null) {
            Log.w(TAG, "runningAppProcesses is null")
            return@withContext emptyList()
        }

        Log.d(TAG, "runningAppProcesses.size = ${runningProcesses.size}")

        for (process in runningProcesses) {
            if (process.pkgList == null) continue

            val pkg = process.pkgList.firstOrNull() ?: process.processName ?: continue

            if (pkg == ourPackage) continue
            if (seen.contains(pkg)) continue
            if (SystemApps.isSystemApp(pkg)) continue
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
                Log.w(TAG, "Cannot get info for $pkg", e)
            }
        }

        result.sortBy { it.label.lowercase() }
        Log.i(TAG, "Final running apps: ${result.size}")
        result
    }

    /**
     * Cek apakah package tertentu sedang running.
     */
    suspend fun isPackageRunning(context: Context, packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val processes = am.runningAppProcesses
                if (processes != null) {
                    for (process in processes) {
                        if (process.pkgList?.contains(packageName) == true) return@withContext true
                        if (process.processName == packageName) return@withContext true
                    }
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
