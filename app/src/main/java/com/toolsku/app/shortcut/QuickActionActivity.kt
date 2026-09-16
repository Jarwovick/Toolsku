package com.toolsku.app.shortcut

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.toolsku.app.automation.ActionStep
import com.toolsku.app.automation.AutomationAccessibilityService
import com.toolsku.app.automation.AutomationTask
import com.toolsku.app.automation.OemProfile
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.CacheSizeFetcher
import com.toolsku.app.core.PermissionHelper
import kotlinx.coroutines.launch

/**
 * Activity transparan yang dijalankan dari shortcut "Toolsku Quick".
 *
 * Alur:
 * 1. Kill all running apps
 * 2. Clear cache all apps (≥ 10 MB)
 * 3. Selesai → tutup
 */
class QuickActionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "QuickAction"
        private const val MIN_CACHE_SIZE = 10 * 1024 * 1024L  // 10 MB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek izin
        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Aktifkan Aksesibilitas dulu", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!PermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(this, "Aktifkan izin overlay dulu", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Jalankan alur
        runQuickAction()
    }

    private fun runQuickAction() {
        lifecycleScope.launch {
            try {
                // FASE 1: Ambil daftar app untuk kill
                Log.i(TAG, "Phase 1: Getting apps to kill")
                val runningApps = AppRepository.getRunningApps(this@QuickActionActivity)
                val systemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)
                val killApps = runningApps + systemApps

                // FASE 2: Ambil daftar app untuk clear cache
                Log.i(TAG, "Phase 2: Getting apps to clean")
                val allApps = AppRepository.getInstalledApps(this@QuickActionActivity, includeSystem = false)
                val safeSystemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)
                val allAppsForClean = (allApps + safeSystemApps).distinctBy { it.packageName }

                val cacheSizes = CacheSizeFetcher.getCacheSizes(
                    this@QuickActionActivity,
                    allAppsForClean.map { it.packageName }
                )

                val cleanApps = allAppsForClean
                    .filter { (cacheSizes[it.packageName] ?: 0L) >= MIN_CACHE_SIZE }

                // Total task
                val totalTasks = killApps.size + cleanApps.size
                Log.i(TAG, "Total tasks: $totalTasks (kill=${killApps.size}, clean=${cleanApps.size})")

                if (totalTasks == 0) {
                    Toast.makeText(
                        this@QuickActionActivity,
                        "Tidak ada yang perlu dibersihkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                // Setup stop callback
                val service = AutomationAccessibilityService.instance ?: run {
                    finish()
                    return@launch
                }

                AutomationAccessibilityService.onStopClick = {
                    service.cancelQueue()
                    service.hideOverlay()
                    finish()
                }

                // Tampilkan overlay
                service.showOverlay(
                    0, totalTasks, "",
                    AutomationAccessibilityService.MODE_CLEANER
                )

                // Build tasks
                val allTasks = mutableListOf<AutomationTask>()

                // Task untuk kill
                killApps.forEach { app ->
                    val steps = mutableListOf<ActionStep>()
                    steps.add(ActionStep.OpenAppInfo(app.packageName))
                    steps.add(ActionStep.Wait(300))
                    steps.add(ActionStep.ClickByText(OemProfile.forceStopButtonLabels))
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.ClickByText(OemProfile.forceStopConfirmLabels))
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.Back)
                    steps.add(ActionStep.Wait(150))
                    allTasks.add(AutomationTask(app.packageName, steps))
                }

                // Task untuk clean cache
                cleanApps.forEach { app ->
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
                    allTasks.add(AutomationTask(app.packageName, steps))
                }

                // Jalankan
                service.runQueue(
                    tasks = allTasks,
                    onProgress = { current, total, pkg ->
                        service.updateOverlay(current, total, pkg)
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
                            finish()
                        }
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Quick action failed", e)
                Toast.makeText(
                    this@QuickActionActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Jangan finish saat di-pause (overlay masih jalan)
    }
}
