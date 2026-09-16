package com.toolsku.app.shortcut

import android.os.Bundle
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
                // Kumpulkan app untuk kill
                val runningApps = AppRepository.getRunningApps(this@QuickActionActivity)
                val systemApps = AppRepository.getSafeSystemApps(this@QuickActionActivity)

                val allTasks = mutableListOf<AutomationTask>()

                // Task 1: Kill user apps
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

                // Task 2: Clear cache system apps
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

                if (allTasks.isEmpty()) {
                    Toast.makeText(
                        this@QuickActionActivity,
                        "Tidak ada app untuk diproses",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    return@launch
                }

                // Setup stop callback (tidak ada tombol, tapi jaga-jaga)
                AutomationAccessibilityService.onStopClick = {
                    service.cancelQueue()
                    service.hideOverlay()
                    finish()
                }

                // Show overlay
                service.showOverlay(
                    0,
                    allTasks.size,
                    "",
                    AutomationAccessibilityService.MODE_KILLER
                )

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
                                "Selesai: ${stats.success} sukses",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                )

            } catch (e: Exception) {
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
