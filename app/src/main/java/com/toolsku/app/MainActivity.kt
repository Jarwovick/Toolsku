package com.toolsku.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.boot.BootActivity
import com.toolsku.app.cleaner.CleanerActivity
import com.toolsku.app.core.PermissionHelper
import com.toolsku.app.core.Prefs
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
        val cardDimmer = binding.cardDimmer as View
        cardDimmer.findViewById<ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_dimmer)
        cardDimmer.findViewById<TextView>(R.id.tvTitle).setText(R.string.menu_dimmer_title)
        cardDimmer.findViewById<TextView>(R.id.tvDesc).setText(R.string.menu_dimmer_desc)
        cardDimmer.setOnClickListener {
            startActivity(Intent(this@MainActivity, DimmerActivity::class.java))
        }

        // Card 2: Cleaner
        val cardCleaner = binding.cardCleaner as View
        cardCleaner.findViewById<ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_cleaner)
        cardCleaner.findViewById<TextView>(R.id.tvTitle).setText(R.string.menu_cleaner_title)
        cardCleaner.findViewById<TextView>(R.id.tvDesc).setText(R.string.menu_cleaner_desc)
        cardCleaner.setOnClickListener {
            startActivity(Intent(this@MainActivity, CleanerActivity::class.java))
        }

        // Card 3: Killer
        val cardKiller = binding.cardKiller as View
        cardKiller.findViewById<ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_killer)
        cardKiller.findViewById<TextView>(R.id.tvTitle).setText(R.string.menu_killer_title)
        cardKiller.findViewById<TextView>(R.id.tvDesc).setText(R.string.menu_killer_desc)
        cardKiller.setOnClickListener {
            startActivity(Intent(this@MainActivity, KillerActivity::class.java))
        }

        // Card 4: Boot
        val cardBoot = binding.cardBoot as View
        cardBoot.findViewById<ImageView>(R.id.ivIcon).setImageResource(R.drawable.ic_boot)
        cardBoot.findViewById<TextView>(R.id.tvTitle).setText(R.string.menu_boot_title)
        cardBoot.findViewById<TextView>(R.id.tvDesc).setText(R.string.menu_boot_desc)
        cardBoot.setOnClickListener {
            startActivity(Intent(this@MainActivity, BootActivity::class.java))
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this@MainActivity, PermissionActivity::class.java))
        }
    }

    private fun checkPermissions() {
        val hasAccessibility = PermissionHelper.isAccessibilityServiceEnabled(this)
        val hasOverlay = PermissionHelper.canDrawOverlays(this)
        val hasUsage = PermissionHelper.hasUsageAccess(this)

        if (hasAccessibility && hasOverlay && hasUsage) {
            Prefs.isFirstLaunch = false
        }
    }
}
