package com.toolsku.app.killer

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R
import com.toolsku.app.automation.ActionStep
import com.toolsku.app.automation.AutomationAccessibilityService
import com.toolsku.app.automation.AutomationTask
import com.toolsku.app.automation.OemProfile

class KillerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ToolskuKiller"
        // Package untuk test
        private const val TEST_PACKAGE = "com.whatsapp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_killer)
        title = getString(R.string.killer_title)

        val btnTest = findViewById<Button>(R.id.btnTest)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        btnTest.setOnClickListener {
            val service = AutomationAccessibilityService.instance
            if (service == null) {
                Toast.makeText(this, "Aktifkan Aksesibilitas dulu", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            tvStatus.text = "Menjalankan otomasi..."

            // Task: kill WhatsApp
            val killSteps = listOf(
                ActionStep.OpenAppInfo(TEST_PACKAGE),
                ActionStep.Wait(800),
                ActionStep.ClickByText(OemProfile.forceStopButtonLabels),
                ActionStep.Wait(500),
                ActionStep.ClickByText(OemProfile.forceStopConfirmLabels),
                ActionStep.Wait(500),
                ActionStep.Back
            )

            val task = AutomationTask(TEST_PACKAGE, killSteps)

            val started = service.runQueue(
                tasks = listOf(task),
                onProgress = { current, total, pkg ->
                    tvStatus.text = "Progress: $current/$total — $pkg"
                },
                onComplete = { stats ->
                    tvStatus.text = "Selesai: ${stats.success} sukses, ${stats.failed} gagal"
                    Toast.makeText(
                        this,
                        "Selesai: ${stats.success} sukses, ${stats.failed} gagal",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            if (!started) {
                tvStatus.text = "Gagal: queue sedang berjalan"
            }
        }
    }
}
