package com.toolsku.app

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.boot.BootAction
import com.toolsku.app.cleaner.CleanerActivity
import com.toolsku.app.core.PermissionHelper
import com.toolsku.app.dimmer.DimmerActivity
import com.toolsku.app.killer.KillerActivity
import com.toolsku.app.onboarding.PermissionActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Card 1: Dimmer
        findViewById<LinearLayout>(R.id.cardDimmer).setOnClickListener {
            startActivity(Intent(this, DimmerActivity::class.java))
        }

        // Card 2: Cleaner
        findViewById<LinearLayout>(R.id.cardCleaner).setOnClickListener {
            startActivity(Intent(this, CleanerActivity::class.java))
        }

        // Card 3: Killer
        findViewById<LinearLayout>(R.id.cardKiller).setOnClickListener {
            startActivity(Intent(this, KillerActivity::class.java))
        }

        // Card 4: Boot
        findViewById<LinearLayout>(R.id.cardBoot).setOnClickListener {
            triggerBootMenu()
        }

        // Settings
        findViewById<LinearLayout>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
    }

    private fun triggerBootMenu() {
        if (!PermissionHelper.isAccessibilityServiceEnabled(this)) {
            Toast.makeText(this, R.string.boot_no_accessibility, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, PermissionActivity::class.java))
            return
        }

        if (!BootAction.isSupported()) {
            Toast.makeText(this, R.string.boot_not_supported, Toast.LENGTH_LONG).show()
            return
        }

        val success = BootAction.openPowerMenu()
        if (success) {
            Toast.makeText(this, R.string.boot_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.boot_failed, Toast.LENGTH_LONG).show()
        }
    }
}
