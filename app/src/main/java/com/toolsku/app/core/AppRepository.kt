    /**
     * Untuk CLEANER — SEMUA app di device (user + system),
     * TIDAK ada filter exception, TIDAK ada filter dangerous.
     *
     * Filter cache akan dilakukan di CleanerActivity.
     */
    suspend fun getAllAppsForCleaner(context: Context): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val result = mutableListOf<AppInfo>()
            val ourPackage = context.packageName

            Log.i(TAG, "getAllAppsForCleaner: scanning ${packages.size} packages")

            for (app in packages) {
                // Skip app sendiri
                if (app.packageName == ourPackage) continue

                // Skip app tanpa nama
                if (app.packageName.isNullOrEmpty()) continue

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                result.add(
                    AppInfo(
                        packageName = app.packageName,
                        label = pm.getApplicationLabel(app).toString(),
                        icon = try { pm.getApplicationIcon(app) } catch (e: Exception) { null },
                        isSystem = isSystem,
                        isException = false
                    )
                )
            }

            result.sortBy { it.label.lowercase() }
            Log.i(TAG, "getAllAppsForCleaner: ${result.size} apps")
            result
        }
