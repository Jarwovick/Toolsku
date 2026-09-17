package com.toolsku.app.onboarding

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R
import com.toolsku.app.core.PermissionHelper

class PermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        title = getString(R.string.onboarding_title)

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(PermissionHelper.accessibilityIntent())
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            startActivity(PermissionHelper.overlayIntent(this))
        }

        findViewById<Button>(R.id.btnUsage).setOnClickListener {
            startActivity(PermissionHelper.usageAccessIntent())
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val tvAccessibility = findViewById<TextView>(R.id.tvAccessibilityStatus)
        val tvOverlay = findViewById<TextView>(R.id.tvOverlayStatus)
        val tvUsage = findViewById<TextView>(R.id.tvUsageStatus)

        tvAccessibility.text = if (PermissionHelper.isAccessibilityServiceEnabled(this)) {
            getString(R.string.perm_granted)
        } else {
            getString(R.string.perm_not_granted)
        }

        tvOverlay.text = if (PermissionHelper.canDrawOverlays(this)) {
            getString(R.string.perm_granted)
        } else {
            getString(R.string.perm_not_granted)
        }

        tvUsage.text = if (PermissionHelper.hasUsageAccess(this)) {
            getString(R.string.perm_granted)
        } else {
            getString(R.string.perm_not_granted)
        }
    }
}
