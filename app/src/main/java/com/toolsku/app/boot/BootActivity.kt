package com.toolsku.app.boot

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R
import com.toolsku.app.automation.AutomationAccessibilityService

class BootActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boot)
        title = getString(R.string.boot_title)

        findViewById<TextView>(R.id.tvHeaderTitle).text =
            getString(R.string.header_boot_menu)

        val btnRestart = findViewById<Button>(R.id.btnRestart)
        val btnShutdown = findViewById<Button>(R.id.btnShutdown)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        // Cek Android version
        val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        if (!isSupported) {
            tvStatus.text = getString(R.string.boot_not_supported)
            tvStatus.visibility = View.VISIBLE
            btnRestart.isEnabled = false
            btnShutdown.isEnabled = false
            btnRestart.alpha = 0.5f
            btnShutdown.alpha = 0.5f
        } else {
            tvStatus.visibility = View.GONE
        }

        btnRestart.setOnClickListener {
            openPowerMenu()
        }

        btnShutdown.setOnClickListener {
            openPowerMenu()
        }
    }

    private fun openPowerMenu() {
        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, R.string.boot_no_accessibility, Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(this, R.string.boot_not_supported, Toast.LENGTH_LONG).show()
            return
        }

        // Coba panggil GLOBAL_ACTION_POWER_DIALOG
        val success = try {
            val method = AccessibilityService::class.java.getMethod(
                "performGlobalAction",
                Int::class.javaPrimitiveType
            )
            val ACTION_POWER_DIALOG = 6
            val result = method.invoke(service, ACTION_POWER_DIALOG) as? Boolean ?: false
            result
        } catch (e: Exception) {
            // Fallback reflection untuk ColorOS
            try {
                val method = AccessibilityService::class.java.getMethod(
                    "performGlobalAction",
                    Int::class.javaPrimitiveType
                )
                val ACTION_POWER_DIALOG = 6
                val result = method.invoke(service, ACTION_POWER_DIALOG) as? Boolean ?: false
                result
            } catch (e2: Exception) {
                false
            }
        }

        if (success) {
            Toast.makeText(this, R.string.boot_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.boot_failed, Toast.LENGTH_LONG).show()
        }
    }
}
