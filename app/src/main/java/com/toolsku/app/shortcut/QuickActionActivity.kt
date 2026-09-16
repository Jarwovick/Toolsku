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
import kotlinx.coroutines.launch

/**
 * Quick Action — dijalankan dari shortcut home screen.
 * Kill running apps + clear cache.
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
            goHome()
            return
        }

        lifecycleScope.launch {
            try {
                // === 1. Ambil running apps untuk KILL ===
                val runningApps = AppRepository.getRunningApps(this@QuickActionActivity)
                Log.i(TAG, "Running apps: ${runningApps.size}")

                // === 2. Ambil safe system apps untuk CLEAR CACHE ===
                var systemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)
                Log.i(TAG, "Safe system apps: ${systemApps.size}")

                // === 3. Fallback: pakai user apps untuk clear cache ===
                if (systemApps.isEmpty()) {
                    Log.w(TAG, "No safe system apps, fallback to user apps")
                    val allUserApps = AppRepository.getInstalledApps(
                        this@QuickActionActivity,
                        includeSystem = false
                    )
                    systemApps = allUserApps.take(20)
                }

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

                // TASK 2: Clear cache
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

                Log.i(TAG, "Total: kill=${runningApps.size}, clean=${systemApps.size}, all=${allTasks.size}")

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
                val mode = if (systemApps.isNotEmpty()) {
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

                            // Kembali ke HOME (bukan Toolsku)
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
