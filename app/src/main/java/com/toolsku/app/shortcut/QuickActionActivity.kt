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
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.CacheSizeFetcher
import com.toolsku.app.core.PermissionHelper
import kotlinx.coroutines.launch

class QuickActionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "QuickAction"
        private const val MIN_CACHE_SIZE = 10 * 1024 * 1024L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Aktifkan Aksesibilitas dulu", Toast.LENGTH_LONG).show()
            goHome()
            return
        }

        if (!PermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(this, "Aktifkan izin overlay dulu", Toast.LENGTH_LONG).show()
            goHome()
            return
        }

        runQuickAction()
    }

    private fun runQuickAction() {
        lifecycleScope.launch {
            try {
                val runningApps = AppRepository.getRunningApps(this@QuickActionActivity)
                val systemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)
                val killApps = runningApps + systemApps

                val allApps = AppRepository.getInstalledApps(this@QuickActionActivity, includeSystem = false)
                val safeSystemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)
                val allAppsForClean = (allApps + safeSystemApps).distinctBy { it.packageName }

                val cacheSizes = CacheSizeFetcher.getCacheSizes(
                    this@QuickActionActivity,
                    allAppsForClean.map { it.packageName }
                )

                val cleanApps = allAppsForClean
                    .filter { (cacheSizes[it.packageName] ?: 0L) >= MIN_CACHE_SIZE }

                val totalTasks = killApps.size + cleanApps.size
                Log.i(TAG, "Total tasks: $totalTasks (kill=${killApps.size}, clean=${cleanApps.size})")

                if (totalTasks == 0) {
                    Toast.makeText(
                        this@QuickActionActivity,
                        "Tidak ada yang perlu dibersihkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    goHome()
                    return@launch
                }

                val service = AutomationAccessibilityService.instance ?: run {
                    goHome()
                    return@launch
                }

                AutomationAccessibilityService.onStopClick = {
                    service.cancelQueue()
                    service.hideOverlay()
                    goHome()
                }

                service.showOverlay(
                    0, totalTasks, "",
                    AutomationAccessibilityService.MODE_CLEANER
                )

                val allTasks = mutableListOf<AutomationTask>()

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

                service.runQueue(
                    tasks = allTasks,
                    onProgress = { current, total, pkg ->
                        service.updateOverlay(current, total, pkg)
                    },
                    onComplete = { stats ->
                        runOnUiThread {
                            service.hideOverlay()
                            AutomationAccessibilityService.onStopClick = null
                            Log.i(TAG, "Quick action complete: ${stats.success} success")
                            goHome()
                        }
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Quick action failed", e)
                goHome()
            }
        }
    }

    /**
     * Kembali ke home screen.
     * Pakai Intent ke Home — supaya benar-benar keluar dari Toolsku.
     */
    private fun goHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go home", e)
        }
        finish()
    }
}
