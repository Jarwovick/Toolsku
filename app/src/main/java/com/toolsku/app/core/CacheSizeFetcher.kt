package com.toolsku.app.core

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CacheSizeFetcher {

    private const val TAG = "CacheSizeFetcher"

    suspend fun getCacheSizes(
        context: Context,
        packages: List<String>
    ): Map<String, Long> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, Long>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "SDK < O, cannot get cache sizes")
            return@withContext result
        }

        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        if (storageStatsManager == null) {
            Log.e(TAG, "StorageStatsManager is null")
            return@withContext result
        }

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        for (pkg in packages) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
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

                val total = internalCache + externalCache
                result[pkg] = total

                // Log untuk debug
                if (total > 0) {
                    Log.d(TAG, "$pkg: ${total / (1024 * 1024)} MB")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Skip $pkg: ${e.message}")
                result[pkg] = 0L
            }
        }

        result
    }

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
