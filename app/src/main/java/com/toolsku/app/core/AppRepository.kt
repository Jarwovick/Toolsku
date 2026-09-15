package com.toolsku.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppRepository {

    /**
     * Ambil daftar SEMUA app terinstall.
     * Dipakai untuk halaman Cleaner.
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
     * Pakai ActivityManager.getRunningAppProcesses().
     * Kalau hasil kosong (karena restriksi Android 10+), fallback ke getInstalledApps.
     *
     * @param fallbackToAllApps Kalau true, tampilkan semua user apps saat running list kosong.
     */
    suspend fun getRunningApps(
        context: Context,
        fallbackToAllApps: Boolean = true
    ): List<AppInfo> = withContext(Dispatchers.IO) {
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

        if (runningProcesses != null) {
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
                    // App sudah tidak terinstall
                }
            }
        }

        // Fallback: kalau running list kosong, tampilkan semua user apps
        if (result.isEmpty() && fallbackToAllApps) {
            return@withContext getInstalledApps(context, includeSystem = false)
                .filter { !it.isException }
        }

        result.sortBy { it.label.lowercase() }
        result
    }
}
