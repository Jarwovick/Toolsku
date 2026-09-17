package com.toolsku.app.core

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper untuk mendapat ukuran cache per app.
 * Hitung TOTAL cache (internal + external).
 */
object CacheSizeFetcher {

    private const val TAG = "CacheSizeFetcher"

    /**
     * Ambil ukuran cache per package.
     * Return Map<packageName, cacheSizeInBytes>
     *
     * Cache size = internal cache + external cache (jika ada).
     */
    suspend fun getCacheSizes(
        context: Context,
        packages: List<String>
    ): Map<String, Long> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Long>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return@withContext result
        }

        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
            ?: return@withContext result

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        for (pkg in packages) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                val uuid = storageManager.getUuidForPath(Environment.getDataDirectory())
                val stats = storageStatsManager.queryStatsForUid(uuid, appInfo.uid)

                // Hitung cache internal
                val internalCache = stats.cacheBytes

                // Hitung cache external (API 29+)
                val externalCache = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        stats.externalCacheBytes
                    } catch (e: Exception) {
                        0L
                    }
                } else {
                    0L
                }

                val totalCache = internalCache + externalCache
                result[pkg] = totalCache

                Log.d(TAG, "$pkg: internal=$internalCache, external=$externalCache, total=$totalCache")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get cache size for $pkg", e)
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
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext 0L

                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val uuid = storageManager.getUuidForPath(Environment.getDataDirectory())
                val stats = storageStatsManager.queryStatsForUid(uuid, appInfo.uid)

                val internalCache = stats.cacheBytes
                val externalCache = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        stats.externalCacheBytes
                    } catch (e: Exception) {
                        0L
                    }
                } else {
                    0L
                }

                internalCache + externalCache
            } catch (e: Exception) {
                0L
            }
        }
}
