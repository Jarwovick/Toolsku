package com.toolsku.app.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.core.PermissionHelper
import com.toolsku.app.databinding.ActivityPermissionBinding

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAccessibility.setOnClickListener {
            startActivity(PermissionHelper.accessibilityIntent())
        }

        binding.btnOverlay.setOnClickListener {
            startActivity(PermissionHelper.overlayIntent(this))
        }

        binding.btnUsage.setOnClickListener {
            startActivity(PermissionHelper.usageAccessIntent())
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        binding.tvAccessibilityStatus.text = if (PermissionHelper.isAccessibilityServiceEnabled(this)) {
            getString(com.toolsku.app.R.string.perm_granted)
        } else {
            getString(com.toolsku.app.R.string.perm_not_granted)
        }

        binding.tvOverlayStatus.text = if (PermissionHelper.canDrawOverlays(this)) {
            getString(com.toolsku.app.R.string.perm_granted)
        } else {
            getString(com.toolsku.app.R.string.perm_not_granted)
        }

        binding.tvUsageStatus.text = if (PermissionHelper.hasUsageAccess(this)) {
            getString(com.toolsku.app.R.string.perm_granted)
        } else {
            getString(com.toolsku.app.R.string.perm_not_granted)
        }
    }
}
