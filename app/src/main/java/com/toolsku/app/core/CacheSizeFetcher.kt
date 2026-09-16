package com.toolsku.app.core

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper untuk mendapat ukuran cache per app.
 * Pakai StorageStatsManager (API 26+).
 */
object CacheSizeFetcher {

    /**
     * Ambil ukuran cache per package.
     * Return Map<packageName, cacheSizeInBytes>
     */
    suspend fun getCacheSizes(
        context: Context,
        packages: List<String>
    ): Map<String, Long> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Long>()

        val storageStatsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else {
            null
        }

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        if (storageStatsManager == null) return@withContext result

        for (pkg in packages) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                val uuid = storageManager.getUuidForPath(android.os.Environment.getDataDirectory())
                val stats = storageStatsManager.queryStatsForUid(
                    uuid,
                    appInfo.uid
                )
                result[pkg] = stats.cacheBytes
            } catch (e: Exception) {
                result[pkg] = 0L
            }
        }

        result
    }

    /**
     * Ambil ukuran cache satu app.
     */
    suspend fun getCacheSize(context: Context, packageName: String): Long =
        withContext(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return@withContext 0L

                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val uuid = storageManager.getUuidForPath(android.os.Environment.getDataDirectory())
                val stats = storageStatsManager.queryStatsForUid(uuid, appInfo.uid)
                stats.cacheBytes
            } catch (e: Exception) {
                0L
            }
        }
}
