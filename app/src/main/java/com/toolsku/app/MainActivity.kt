package com.toolsku.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.boot.BootActivity
import com.toolsku.app.cleaner.CleanerActivity
import com.toolsku.app.core.PermissionHelper
import com.toolsku.app.databinding.ActivityMainBinding
import com.toolsku.app.dimmer.DimmerActivity
import com.toolsku.app.killer.KillerActivity
import com.toolsku.app.onboarding.PermissionActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMenuCards()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun setupMenuCards() {
        // Card 1: Dimmer
        binding.cardDimmer.apply {
            findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_dimmer)
            findViewById<android.widget.TextView>(R.id.tvTitle).setText(R.string.menu_dimmer_title)
            findViewById<android.widget.TextView>(R.id.tvDesc).setText(R.string.menu_dimmer_desc)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, DimmerActivity::class.java))
            }
        }

        // Card 2: Cleaner
        binding.cardCleaner.apply {
            findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_cleaner)
            findViewById<android.widget.TextView>(R.id.tvTitle).setText(R.string.menu_cleaner_title)
            findViewById<android.widget.TextView>(R.id.tvDesc).setText(R.string.menu_cleaner_desc)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, CleanerActivity::class.java))
            }
        }

        // Card 3: Killer
        binding.cardKiller.apply {
            findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_killer)
            findViewById<android.widget.TextView>(R.id.tvTitle).setText(R.string.menu_killer_title)
            findViewById<android.widget.TextView>(R.id.tvDesc).setText(R.string.menu_killer_desc)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, KillerActivity::class.java))
            }
        }

        // Card 4: Boot
        binding.cardBoot.apply {
            findViewById<android.widget.ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_boot)
            findViewById<android.widget.TextView>(R.id.tvTitle).setText(R.string.menu_boot_title)
            findViewById<android.widget.TextView>(R.id.tvDesc).setText(R.string.menu_boot_desc)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, BootActivity::class.java))
            }
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this@MainActivity, PermissionActivity::class.java))
        }
    }

    /**
     * Cek izin yang dibutuhkan. Kalau ada yang belum, arahkan ke onboarding.
     */
    private fun checkPermissions() {
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(this)
        val hasOverlay = PermissionHelper.canDrawOverlays(this)
        val hasUsage = PermissionHelper.hasUsageAccess(this)

        // Kalau semua izin sudah ada, tandai first launch selesai
        if (hasAccessibility && hasOverlay && hasUsage) {
            com.toolsku.app.core.Prefs.isFirstLaunch = false
        }
    }
}
