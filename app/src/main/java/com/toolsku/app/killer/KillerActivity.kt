package com.toolsku.app.killer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toolsku.app.R
import com.toolsku.app.core.AppInfo
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.Prefs
import com.toolsku.app.core.db.DatabaseProvider
import kotlinx.coroutines.launch

/**
 * Halaman Killer.
 * Placeholder — otomasi akan diaktifkan di Fase K7.
 */
class KillerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KillerActivity"
    }

    private lateinit var adapter: KillerAdapter
    private lateinit var tvHeaderTitle: TextView
    private lateinit var btnSelectAll: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnCloseApps: Button
    private lateinit var progressRam: ProgressBar
    private lateinit var barRam: ProgressBar
    private lateinit var tvRamPercent: TextView
    private lateinit var tvRamUsedText: TextView
    private lateinit var tvRamAvailable: TextView
    private lateinit var tvLaunchedApps: TextView
    private lateinit var tvLoading: TextView

    private var isProcessing = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_killer)
        title = getString(R.string.killer_title)

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnCloseApps = findViewById(R.id.btnCloseApps)
        progressRam = findViewById(R.id.progressRam)
        barRam = findViewById(R.id.barRam)
        tvRamPercent = findViewById(R.id.tvRamPercent)
        tvRamUsedText = findViewById(R.id.tvRamUsedText)
        tvRamAvailable = findViewById(R.id.tvRamAvailable)
        tvLaunchedApps = findViewById(R.id.tvLaunchedApps)
        tvLoading = findViewById(R.id.tvLoading)

        tvHeaderTitle.text = getString(R.string.header_kill_apps_running)

        adapter = KillerAdapter(
            onItemClick = { updateSelectAllState() },
            onMenuClick = { item, view -> showAppMenu(item, view) }
        )
        findViewById<RecyclerView>(R.id.rvApps).apply {
            layoutManager = LinearLayoutManager(this@KillerActivity)
            adapter = this@KillerActivity.adapter
        }

        btnRefresh.setOnClickListener { loadApps() }

        btnSelectAll.setOnClickListener {
            val allCurrentlySelected = adapter.getAppItems().all { it.selected }
            adapter.selectAll(!allCurrentlySelected)
            updateSelectAllState()
            updateButtonCount()
        }

        btnCloseApps.setOnClickListener {
            if (isProcessing) {
                Toast.makeText(this, "Sedang memproses…", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startKilling()
        }

        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { view ->
            showMainMenu(view)
        }

        updateRamInfo()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        updateRamInfo()
        if (!isProcessing) loadApps()
    }

    // ==== DATA ====

    private fun loadApps() {
        tvLoading.visibility = View.VISIBLE
        tvLoading.text = getString(R.string.killer_loading)

        lifecycleScope.launch {
            val userApps = AppRepository.getRunningApps(this@KillerActivity)
            val systemApps = AppRepository.getSafeSystemApps(this@KillerActivity)

            val combined = mutableListOf<KillerAppItem>()
            if (userApps.isNotEmpty()) {
                combined.add(KillerAppItem.section("USER APPS (${userApps.size})"))
                combined.addAll(userApps.map { it.toKillerItem() })
            }
            if (systemApps.isNotEmpty()) {
                combined.add(KillerAppItem.section("SYSTEM APPS (${systemApps.size})"))
                combined.addAll(systemApps.map { it.toKillerItem() })
            }

            adapter.submitList(combined)
            updateSelectAllState()
            updateButtonCount()
            updateLaunchedAppsCount(userApps.size + systemApps.size)
            updateRamInfo()

            if (combined.isEmpty()) {
                tvLoading.visibility = View.VISIBLE
                tvLoading.text = getString(R.string.killer_no_apps)
            } else {
                tvLoading.visibility = View.GONE
            }
        }
    }

    private fun AppInfo.toKillerItem(): KillerAppItem {
        return KillerAppItem(
            packageName = this.packageName,
            label = this.label,
            icon = this.icon,
            isSystem = this.isSystem,
            isException = this.isException,
            selected = true
        )
    }

    private fun updateRamInfo() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        val totalGb = mi.totalMem.toDouble() / (1024 * 1024 * 1024)
        val availGb = mi.availMem.toDouble() / (1024 * 1024 * 1024)
        val usedGb = totalGb - availGb
        val percent = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt() else 0

        tvRamPercent.text = "$percent%"
        progressRam.progress = percent
        barRam.progress = percent
        tvRamUsedText.text = String.format("%.2f GB / %.2f GB", usedGb, totalGb)
        tvRamAvailable.text = String.format("%.2f GB", availGb)
    }

    private fun updateLaunchedAppsCount(count: Int) {
        tvLaunchedApps.text = count.toString()
    }

    private fun updateButtonCount() {
        val count = adapter.getAppItems().count { it.selected }
        btnCloseApps.text = if (count == 0) {
            getString(R.string.killer_close_apps_zero)
        } else {
            getString(R.string.killer_close_apps_count, count)
        }
        btnCloseApps.isEnabled = count > 0 && !isProcessing
        btnCloseApps.alpha = if (btnCloseApps.isEnabled) 1f else 0.5f
    }

    private fun updateSelectAllState() {
        val apps = adapter.getAppItems()
        val allSelected = apps.isNotEmpty() && apps.all { it.selected }

        btnSelectAll.setImageResource(
            if (allSelected) R.drawable.ic_checkbox_checked
            else R.drawable.ic_checkbox_unchecked
        )
        btnSelectAll.imageTintList = android.content.res.ColorStateList.valueOf(
            if (allSelected) ContextCompat.getColor(this, R.color.accent)
            else ContextCompat.getColor(this, R.color.text_secondary)
        )
    }

    private fun showAppMenu(item: KillerAppItem, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 0, 0, getString(R.string.killer_menu_app_info))
        popup.menu.add(
            0, 1, 1,
            if (Prefs.isException(item.packageName))
                getString(R.string.killer_menu_remove_exception)
            else
                getString(R.string.killer_menu_add_exception)
        )
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> openAppInfo(item.packageName)
                1 -> toggleException(item)
            }
            true
        }
        popup.show()
    }

    private fun showMainMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 0, 0, getString(R.string.killer_menu_exception))
        popup.setOnMenuItemClickListener {
            startActivity(Intent(this, ExceptionActivity::class.java))
            true
        }
        popup.show()
    }

    private fun openAppInfo(pkg: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$pkg")
        }
        startActivity(intent)
    }

    private fun toggleException(item: KillerAppItem) {
        if (Prefs.isException(item.packageName)) {
            Prefs.removeException(item.packageName)
            Toast.makeText(this, "Dihapus dari pengecualian", Toast.LENGTH_SHORT).show()
        } else {
            Prefs.addException(item.packageName)
            Toast.makeText(this, "Ditambahkan ke pengecualian", Toast.LENGTH_SHORT).show()
        }
        loadApps()
    }

    private fun startKilling() {
        val selected = adapter.getAppItems().filter { it.selected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Tidak ada aplikasi dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO Fase K7: Aktifkan otomasi
        Toast.makeText(this, "Fitur Killer akan diaktifkan di Fase K7", Toast.LENGTH_LONG).show()
    }
}
