package com.toolsku.app.shortcut

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.toolsku.app.automation.ActionStep
import com.toolsku.app.automation.AutomationAccessibilityService
import com.toolsku.app.automation.AutomationTask
import com.toolsku.app.automation.OemProfile
import com.toolsku.app.core.AppInfo
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.CacheSizeFetcher
import kotlinx.coroutines.launch

/**
 * Quick Action — dijalankan dari shortcut home screen.
 * Kill running apps + clear cache (filter 10 MB).
 */
class QuickActionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ToolskuQuick"
        private const val MIN_CACHE_SIZE = 10 * 1024 * 1024L  // 10 MB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Aktifkan Aksesibilitas dulu", Toast.LENGTH_LONG).show()
            goHome()
            return
        }

        lifecycleScope.launch {
            try {
                // === 1. Ambil running apps untuk KILL ===
                val runningApps = AppRepository.getRunningApps(this@QuickActionActivity)
                Log.i(TAG, "Running apps: ${runningApps.size}")

                // === 2. Ambil kandidat app untuk CLEAR CACHE ===
                val candidateApps = mutableListOf<AppInfo>()

                // Safe system apps
                val systemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)
                candidateApps.addAll(systemApps)

                // User apps (biar bisa clear cache app user juga)
                val userApps = AppRepository.getInstalledApps(
                    this@QuickActionActivity,
                    includeSystem = false
                )
                candidateApps.addAll(userApps)

                // Hapus duplikat
                val uniqueCandidates = candidateApps.distinctBy { it.packageName }
                Log.i(TAG, "Candidates for cache: ${uniqueCandidates.size}")

                // === 3. Query cache size untuk SEMUA kandidat ===
                val cacheSizes = CacheSizeFetcher.getCacheSizes(
                    this@QuickActionActivity,
                    uniqueCandidates.map { it.packageName }
                )

                // === 4. Filter: HANYA yang cache >= 10 MB ===
                val appsToClean = uniqueCandidates.filter { app ->
                    val cacheSize = cacheSizes[app.packageName] ?: 0L
                    cacheSize >= MIN_CACHE_SIZE
                }
                Log.i(TAG, "Apps to clean (≥ 10 MB): ${appsToClean.size}")

                val allTasks = mutableListOf<AutomationTask>()

                // TASK 1: Kill user apps running
                for (app in runningApps) {
                    val steps = mutableListOf<ActionStep>()
                    steps.add(ActionStep.OpenAppInfo(app.packageName))
                    steps.add(ActionStep.Wait(300))
                    steps.add(ActionStep.ClickByText(OemProfile.forceStopButtonLabels))
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.ClickByText(OemProfile.forceStopConfirmLabels))
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.Back)
                    steps.add(ActionStep.Wait(150))
                    allTasks.add(AutomationTask(app.packageName, app.label, steps))
                }

                // TASK 2: Clear cache (HANYA yang ≥ 10 MB)
                for (app in appsToClean) {
                    val steps = mutableListOf<ActionStep>()
                    steps.add(ActionStep.OpenAppInfo(app.packageName))
                    steps.add(ActionStep.Wait(400))
                    steps.add(ActionStep.ClickByText(OemProfile.storageMenuLabels))
                    steps.add(ActionStep.Wait(400))
                    steps.add(ActionStep.ClickByTextSafe(
                        OemProfile.clearCacheLabels,
                        OemProfile.clearDataLabels
                    ))
                    steps.add(ActionStep.Wait(400))
                    steps.add(ActionStep.Back)
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.Back)
                    steps.add(ActionStep.Wait(200))
                    allTasks.add(AutomationTask(app.packageName, app.label, steps))
                }

                Log.i(TAG, "Total tasks: kill=${runningApps.size}, clean=${appsToClean.size}, all=${allTasks.size}")

                if (allTasks.isEmpty()) {
                    Toast.makeText(
                        this@QuickActionActivity,
                        "Tidak ada app untuk diproses",
                        Toast.LENGTH_SHORT
                    ).show()
                    goHome()
                    return@launch
                }

                // Setup stop callback
                AutomationAccessibilityService.onStopClick = {
                    service.cancelQueue()
                    service.hideOverlay()
                    goHome()
                }

                // Pilih mode overlay
                val mode = if (appsToClean.isNotEmpty()) {
                    AutomationAccessibilityService.MODE_CLEANER
                } else {
                    AutomationAccessibilityService.MODE_KILLER
                }

                // Show overlay
                service.showOverlay(0, allTasks.size, "", mode)

                // Jalankan queue
                service.runQueue(
                    tasks = allTasks,
                    onProgress = { current, total, appLabel ->
                        service.updateOverlay(current, total, appLabel)
                    },
                    onComplete = { stats ->
                        runOnUiThread {
                            service.hideOverlay()
                            AutomationAccessibilityService.onStopClick = null

                            Toast.makeText(
                                this@QuickActionActivity,
                                "Selesai: ${stats.success} sukses, ${stats.failed} gagal",
                                Toast.LENGTH_LONG
                            ).show()

                            // Kembali ke HOME
                            goHome()
                        }
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error", e)
                Toast.makeText(
                    this@QuickActionActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                goHome()
            }
        }
    }

    /**
     * Kembali ke home screen device.
     */
    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
