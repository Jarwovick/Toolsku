package com.toolsku.app.shortcut

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.automation.ActionStep
import com.toolsku.app.automation.AutomationAccessibilityService
import com.toolsku.app.automation.AutomationTask
import com.toolsku.app.automation.OemProfile
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.Prefs
import kotlinx.coroutines.launch

/**
 * Quick Action — dijalankan dari shortcut home screen.
 * Langsung kill all + clear cache tanpa UI.
 */
class QuickActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Aktifkan Aksesibilitas dulu", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Jalankan di background
        androidx.lifecycle.lifecycleScope.launch {
            try {
                // 1. Kumpulkan app untuk kill
                val runningApps = AppRepository.getRunningApps(this@QuickActionActivity)
                val systemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)

                val allTasks = mutableListOf<AutomationTask>()

                // Task kill untuk user apps
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
                    // ⭐ PASS appLabel
                    allTasks.add(AutomationTask(app.packageName, app.label, steps))
                }

                // Task clear cache untuk system apps
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
                    // ⭐ PASS appLabel
                    allTasks.add(AutomationTask(app.packageName, app.label, steps))
                }

                if (allTasks.isEmpty()) {
                    Toast.makeText(this@QuickActionActivity, "Tidak ada app untuk diproses", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                // Jalankan queue
                service.runQueue(
                    tasks = allTasks,
                    onProgress = { current, total, appLabel ->
                        service.updateOverlay(current, total, appLabel)
                    },
                    onComplete = { stats ->
                        runOnUiThread {
                            service.hideOverlay()
                            Toast.makeText(
                                this@QuickActionActivity,
                                "Selesai: ${stats.success} sukses",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                )

                // Tampilkan overlay di awal
                service.showOverlay(0, allTasks.size, "", AutomationAccessibilityService.MODE_KILLER)

            } catch (e: Exception) {
                Toast.makeText(this@QuickActionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
