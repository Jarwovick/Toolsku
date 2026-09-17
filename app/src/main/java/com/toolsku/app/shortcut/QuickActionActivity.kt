package com.toolsku.app.shortcut

import android.app.ActivityManager
import android.content.Context
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
import com.toolsku.app.core.PermissionHelper
import kotlinx.coroutines.launch

/**
 * Activity transparan yang dijalankan dari shortcut "Toolsku Quick".
 *
 * Alur:
 * 1. Kill all running apps (dengan fallback)
 * 2. Clear cache all apps (≥ 10 MB)
 * 3. Kembali ke home
 */
class QuickActionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ToolskuQuick"
        private const val MIN_CACHE_SIZE = 10 * 1024 * 1024L  // 10 MB
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek izin
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

        // Jalankan
        runQuickAction()
    }

    private fun runQuickAction() {
        lifecycleScope.launch {
            try {
                // ===== FASE 1: Ambil apps untuk KILL =====
                Log.i(TAG, "Phase 1: Getting apps to kill")
                var killApps: List<AppInfo> = AppRepository.getRunningApps(this@QuickActionActivity)
                Log.i(TAG, "Running apps (DB): ${killApps.size}")

                // Fallback: kalau DB kosong, pakai user apps
                if (killApps.isEmpty()) {
                    Log.w(TAG, "No running apps, using fallback (user apps)")
                    killApps = AppRepository.getInstalledApps(
                        this@QuickActionActivity,
                        includeSystem = false
                    ).take(15)
                    Log.i(TAG, "Fallback kill apps: ${killApps.size}")
                }

                // ===== FASE 2: Ambil apps untuk CLEAR CACHE =====
                Log.i(TAG, "Phase 2: Getting apps to clean")
                val userApps: List<AppInfo> = AppRepository.getInstalledApps(
                    this@QuickActionActivity,
                    includeSystem = false
                )
                val systemApps: List<AppInfo> = AppRepository.getSafeSystemApps(
                    this@QuickActionActivity
                )
                val candidateApps: List<AppInfo> = (userApps + systemApps)
                    .distinctBy { app: AppInfo -> app.packageName }

                Log.i(TAG, "Candidate apps: ${candidateApps.size}")

                // Query cache size
                val cacheSizes: Map<String, Long> = CacheSizeFetcher.getCacheSizes(
                    this@QuickActionActivity,
                    candidateApps.map { app: AppInfo -> app.packageName }
                )
                Log.i(TAG, "Cache sizes retrieved: ${cacheSizes.size}")

                // Filter cache >= 10 MB
                val cleanApps: List<AppInfo> = candidateApps.filter { app: AppInfo ->
                    val cacheSize: Long = cacheSizes[app.packageName] ?: 0L
                    cacheSize >= MIN_CACHE_SIZE
                }
                Log.i(TAG, "Apps to clean (≥ 10 MB): ${cleanApps.size}")

                // ===== TOTAL =====
                val totalTasks: Int = killApps.size + cleanApps.size
                Log.i(TAG, "Total: kill=${killApps.size}, clean=${cleanApps.size}, all=$totalTasks")

                if (totalTasks == 0) {
                    Toast.makeText(
                        this@QuickActionActivity,
                        "Tidak ada yang perlu dibersihkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    goHome()
                    return@launch
                }

                // ===== SETUP =====
                val service: AutomationAccessibilityService =
                    AutomationAccessibilityService.instance ?: run {
                        goHome()
                        return@launch
                    }

                AutomationAccessibilityService.onStopClick = {
                    service.cancelQueue()
                    service.hideOverlay()
                    goHome()
                }

                // Mode overlay
                val mode: String = if (cleanApps.isNotEmpty()) {
                    AutomationAccessibilityService.MODE_CLEANER
                } else {
                    AutomationAccessibilityService.MODE_KILLER
                }

                service.showOverlay(0, totalTasks, "", mode)

                // ===== BUILD TASKS =====
                val allTasks: MutableList<AutomationTask> = mutableListOf<AutomationTask>()

                // Task KILL
                killApps.forEach { app: AppInfo ->
                    val steps: MutableList<ActionStep> = mutableListOf<ActionStep>()
                    steps.add(ActionStep.OpenAppInfo(app.packageName))
                    steps.add(ActionStep.Wait(300))
                    steps.add(ActionStep.ClickByText(OemProfile.forceStopButtonLabels))
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.ClickByText(OemProfile.forceStopConfirmLabels))
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.Back)
                    steps.add(ActionStep.Wait(150))

                    allTasks.add(
                        AutomationTask(
                            app.packageName,
                            app.label,
                            steps
                        )
                    )
                }

                // Task CLEAN
                cleanApps.forEach { app: AppInfo ->
                    val steps: MutableList<ActionStep> = mutableListOf<ActionStep>()
                    steps.add(ActionStep.OpenAppInfo(app.packageName))
                    steps.add(ActionStep.Wait(400))
                    steps.add(ActionStep.ClickByText(OemProfile.storageMenuLabels))
                    steps.add(ActionStep.Wait(400))
                    steps.add(
                        ActionStep.ClickByTextSafe(
                            OemProfile.clearCacheLabels,
                            OemProfile.clearDataLabels
                        )
                    )
                    steps.add(ActionStep.Wait(400))
                    steps.add(ActionStep.Back)
                    steps.add(ActionStep.Wait(200))
                    steps.add(ActionStep.Back)
                    steps.add(ActionStep.Wait(200))

                    allTasks.add(
                        AutomationTask(
                            app.packageName,
                            app.label,
                            steps
                        )
                    )
                }

                // ===== RUN =====
                service.runQueue(
                    tasks = allTasks,
                    onProgress = { current: Int, total: Int, appLabel: String ->
                        service.updateOverlay(current, total, appLabel)
                    },
                    onComplete = { stats ->
                        runOnUiThread {
                            service.hideOverlay()
                            AutomationAccessibilityService.onStopClick = null

                            // Kill Settings task
                            killSettingsTask()

                            Toast.makeText(
                                this@QuickActionActivity,
                                "Selesai: ${stats.success} sukses, ${stats.failed} gagal",
                                Toast.LENGTH_LONG
                            ).show()

                            goHome()
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
                goHome()
            }
        }
    }

    /**
     * Kembali ke home screen device.
     */
    private fun goHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "goHome failed", e)
            finish()
        }
    }

    /**
     * Kill task Settings (App Info).
     */
    private fun killSettingsTask() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.appTasks
            for (task in tasks) {
                try {
                    val taskInfo = task.taskInfo
                    val baseIntent = taskInfo?.baseIntent
                    val pkg = baseIntent?.component?.packageName

                    if (pkg == "com.android.settings" ||
                        pkg == "com.coloros.settings" ||
                        pkg == "com.oppo.settings") {
                        task.finishAndRemoveTask()
                        Log.i(TAG, "Killed Settings task: $pkg")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to kill task", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "killSettingsTask failed", e)
        }
    }

    override fun onPause() {
        super.onPause()
        // Jangan finish saat di-pause (overlay masih jalan)
    }
}
