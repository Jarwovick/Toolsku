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
import kotlinx.coroutines.launch

/**
 * Quick Action — dijalankan dari shortcut home screen.
 * Langsung kill all + clear cache tanpa UI.
 */
class QuickActionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ToolskuQuick"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Aktifkan Aksesibilitas dulu", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // 1. Ambil app untuk KILL (user apps running)
                val runningApps = AppRepository.getRunningApps(this@QuickActionActivity)
                Log.i(TAG, "Running apps: ${runningApps.size}")

                // 2. Ambil app untuk CLEAR CACHE (safe system apps)
                var systemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)
                Log.i(TAG, "Safe system apps: ${systemApps.size}")

                // Fallback: kalau system apps kosong, ambil user apps untuk clear cache
                if (systemApps.isEmpty()) {
                    Log.w(TAG, "No safe system apps found, using user apps for cache clear")
                    val allUserApps = AppRepository.getInstalledApps(
                        this@QuickActionActivity,
                        includeSystem = false
                    )
                    // Ambil 20 app pertama (biar tidak terlalu lama)
                    systemApps = allUserApps.take(20)
                    Log.i(TAG, "Fallback user apps for cache: ${systemApps.size}")
                }

                val allTasks = mutableListOf<AutomationTask>()

                // TASK 1: Kill user apps yang running
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

                // TASK 2: Clear cache (system apps ATAU fallback user apps)
                for (app in systemApps) {
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

                Log.i(TAG, "Total tasks: ${allTasks.size} (kill=${runningApps.size}, clean=${systemApps.size})")

                if (allTasks.isEmpty()) {
                    Toast.makeText(
                        this@QuickActionActivity,
                        "Tidak ada app untuk diproses",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                // Setup stop callback
                AutomationAccessibilityService.onStopClick = {
                    service.cancelQueue()
                    service.hideOverlay()
                    finish()
                }

                // Show overlay — pakai CLEANER mode kalau ada task clean
                val mode = if (systemApps.isNotEmpty()) {
                    AutomationAccessibilityService.MODE_CLEANER
                } else {
                    AutomationAccessibilityService.MODE_KILLER
                }

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
                            finish()
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
                finish()
            }
        }
    }
}
