package com.toolsku.app.dimmer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.toolsku.app.R
import com.toolsku.app.core.PermissionHelper
import com.toolsku.app.core.Prefs

class DimmerActivity : AppCompatActivity() {

    private var isDimmingActive = false
    private var currentLevel = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dimmer)
        title = getString(R.string.dimmer_title)

        currentLevel = Prefs.dimmerLevel
        isDimmingActive = Prefs.dimmerEnabled

        setupUI()
        requestNotificationPermissionIfNeeded()
    }

    private fun setupUI() {
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val tvLevel = findViewById<android.widget.TextView>(R.id.tvLevel)
        val tvStatus = findViewById<android.widget.TextView>(R.id.tvStatus)
        val btnToggle = findViewById<android.widget.Button>(R.id.btnToggle)

        seekBar.progress = currentLevel
        tvLevel.text = getString(R.string.dimmer_current_level, currentLevel)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                currentLevel = progress
                tvLevel.text = getString(R.string.dimmer_current_level, progress)
                Prefs.dimmerLevel = progress

                if (isDimmingActive) {
                    sendServiceAction(DimmerService.ACTION_UPDATE, progress)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        updateStatusUI(tvStatus, btnToggle)

        btnToggle.setOnClickListener {
            if (!PermissionHelper.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.perm_overlay_desc, Toast.LENGTH_LONG).show()
                startActivity(PermissionHelper.overlayIntent(this))
                return@setOnClickListener
            }

            if (isDimmingActive) {
                stopDimming()
            } else {
                startDimming()
            }

            updateStatusUI(tvStatus, btnToggle)
        }
    }

    private fun updateStatusUI(tvStatus: android.widget.TextView, btnToggle: android.widget.Button) {
        if (isDimmingActive) {
            tvStatus.text = getString(R.string.dimmer_on)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
            btnToggle.text = getString(R.string.dimmer_off)
            btnToggle.backgroundTintList =
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.danger))
        } else {
            tvStatus.text = getString(R.string.dimmer_off)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            btnToggle.text = getString(R.string.dimmer_on)
            btnToggle.backgroundTintList =
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent))
        }
    }

    private fun startDimming() {
        isDimmingActive = true
        Prefs.dimmerEnabled = true
        sendServiceAction(DimmerService.ACTION_START, currentLevel)
    }

    private fun stopDimming() {
        isDimmingActive = false
        Prefs.dimmerEnabled = false
        sendServiceAction(DimmerService.ACTION_STOP, 0)
    }

    private fun sendServiceAction(action: String, level: Int) {
        val intent = Intent(this, DimmerService::class.java).apply {
            this.action = action
            putExtra(DimmerService.EXTRA_LEVEL, level)
        }
        if (action == DimmerService.ACTION_START) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Sync status jika user ubah izin dari Settings
        val tvStatus = findViewById<android.widget.TextView>(R.id.tvStatus)
        val btnToggle = findViewById<android.widget.Button>(R.id.btnToggle)
        updateStatusUI(tvStatus, btnToggle)
    }
}
