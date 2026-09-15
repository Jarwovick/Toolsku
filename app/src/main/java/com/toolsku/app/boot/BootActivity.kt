package com.toolsku.app.boot

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R
import com.toolsku.app.automation.AutomationAccessibilityService

/**
 * Fallback Activity — hanya dibuka kalau Power Menu tidak bisa diakses
 * langsung dari MainActivity.
 *
 * Karena kita sudah handle klik "Menu Boot" langsung dari MainActivity,
 * Activity ini jarang dibuka.
 */
class BootActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AutomationAccessibilityService.instance == null) {
            Toast.makeText(this, R.string.boot_no_accessibility, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!BootAction.isSupported()) {
            Toast.makeText(this, R.string.boot_not_supported, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val success = BootAction.openPowerMenu()
        if (success) {
            Toast.makeText(this, R.string.boot_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.boot_failed, Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
