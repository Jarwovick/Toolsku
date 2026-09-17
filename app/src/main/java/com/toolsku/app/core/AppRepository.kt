    /**
     * Untuk CLEANER — semua app (kecuali dangerous), TIDAK ada filter exception.
     */
    suspend fun getAllAppsForCleaner(context: Context): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val result = mutableListOf<AppInfo>()
            val ourPackage = context.packageName

            for (app in packages) {
                if (app.packageName == ourPackage) continue
                if (SystemApps.isDangerousSystemApp(app.packageName)) continue
                // ⭐ TIDAK ada filter exception

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                result.add(
                    AppInfo(
                        packageName = app.packageName,
                        label = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                        isSystem = isSystem,
                        isException = false  // ⭐ selalu false
                    )
                )
            }

            result.sortBy { it.label.lowercase() }
            Log.i(TAG, "getAllAppsForCleaner: ${result.size} apps")
            result
        }
