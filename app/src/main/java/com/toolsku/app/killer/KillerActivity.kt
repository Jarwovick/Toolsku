package com.toolsku.app.killer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toolsku.app.R
import com.toolsku.app.automation.ActionStep
import com.toolsku.app.automation.AutomationAccessibilityService
import com.toolsku.app.automation.AutomationTask
import com.toolsku.app.automation.OemProfile
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.Prefs
import kotlinx.coroutines.launch

class KillerActivity : AppCompatActivity() {

    private enum class FilterType { USER_APPS, SYSTEM_APPS }

    private lateinit var adapter: KillerAdapter
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvFilterLabel: TextView
    private lateinit var btnSelectAll: ImageButton
    private lateinit var btnCloseApps: Button
    private lateinit var progressRam: android.widget.ProgressBar
    private lateinit var barRam: android.widget.ProgressBar
    private lateinit var tvRamPercent: TextView
    private lateinit var tvRamUsedText: TextView
    private lateinit var tvRamAvailable: TextView
    private lateinit var tvLaunchedApps: TextView

    private var currentFilter = FilterType.USER_APPS
    private var allUserApps: List<KillerAppItem> = emptyList()
    private var allSystemApps: List<KillerAppItem> = emptyList()
    private var allSelected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_killer)
        title = getString(R.string.killer_title)

        // Init views
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvFilterLabel = findViewById(R.id.tvFilterLabel)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnCloseApps = findViewById(R.id.btnCloseApps)
        progressRam = findViewById(R.id.progressRam)
        barRam = findViewById(R.id.barRam)
        tvRamPercent = findViewById(R.id.tvRamPercent)
        tvRamUsedText = findViewById(R.id.tvRamUsedText)
        tvRamAvailable = findViewById(R.id.tvRamAvailable)
        tvLaunchedApps = findViewById(R.id.tvLaunchedApps)

        tvHeaderTitle.text = getString(R.string.header_kill_apps_running)

        // Setup RecyclerView
        adapter = KillerAdapter(
            onItemClick = { updateButtonCount() },
            onMenuClick = { item, view -> showAppMenu(item, view) }
        )
        findViewById<RecyclerView>(R.id.rvApps).apply {
            layoutManager = LinearLayoutManager(this@KillerActivity)
            adapter = this@KillerActivity.adapter
        }

        // Setup filter
        findViewById<LinearLayout>(R.id.btnFilter).setOnClickListener {
            showFilterMenu()
        }

        // Setup select all
        btnSelectAll.setOnClickListener {
            allSelected = !allSelected
            adapter.selectAll(allSelected)
            updateButtonCount()
        }

        // Setup tombol close apps
        btnCloseApps.setOnClickListener {
            startKilling()
        }

        // Setup menu 3 titik
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { view ->
            showMainMenu(view)
        }

        // Load data
        updateRamInfo()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        updateRamInfo()
    }

    // ==== DATA ====

    private fun loadApps() {
        lifecycleScope.launch {
            allUserApps = AppRepository.getInstalledApps(this@KillerActivity, includeSystem = false)
                .map { it.toKillerItem() }
            allSystemApps = AppRepository.getInstalledApps(this@KillerActivity, includeSystem = true)
                .filter { it.isSystem }
                .map { it.toKillerItem() }
            applyFilter()
        }
    }

    private fun com.toolsku.app.core.AppInfo.toKillerItem(): KillerAppItem {
        return KillerAppItem(
            packageName = this.packageName,
            label = this.label,
            icon = this.icon,
            isSystem = this.isSystem,
            isException = this.isException,
            selected = !this.isException
        )
    }

    private fun applyFilter() {
        val list = when (currentFilter) {
            FilterType.USER_APPS -> allUserApps
            FilterType.SYSTEM_APPS -> allSystemApps
        }
        adapter.submitList(list)
        allSelected = list.all { it.selected }
        updateSelectAllIcon()
        updateButtonCount()
    }

    // ==== RAM ====

    private fun updateRamInfo() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)

        val totalGb = mi.totalMem.toDouble() / (1024 * 1024 * 1024)
        val availGb = mi.availMem.toDouble() / (1024 * 1024 * 1024)
        val usedGb = totalGb - availGb
        val percent = ((usedGb / totalGb) * 100).toInt()

        tvRamPercent.text = "$percent%"
        progressRam.progress = percent
        barRam.progress = percent
        tvRamUsedText.text = String.format("%.2f GB / %.2f GB", usedGb, totalGb)
        tvRamAvailable.text = String.format("%.2f GB", availGb)

        // Count launched apps (approx: total installed user apps)
        tvLaunchedApps.text = allUserApps.size.toString()
    }

    // ==== UI ====

    private fun updateButtonCount() {
        val count = adapter.getItems().count { it.selected }
        btnCloseApps.text = if (count == 0) {
            getString(R.string.killer_close_apps_zero)
        } else {
            getString(R.string.killer_close_apps_count, count)
        }
    }

    private fun updateSelectAllIcon() {
        btnSelectAll.setImageResource(R.drawable.ic_checkbox_checked)
        btnSelectAll.imageTintList = android.content.res.ColorStateList.valueOf(
            if (allSelected) {
                androidx.core.content.ContextCompat.getColor(this, R.color.accent)
            } else {
                androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary)
            }
        )
    }

    private fun showFilterMenu() {
        val popup = PopupMenu(this, findViewById(R.id.btnFilter))
        popup.menu.add(0, 0, 0, getString(R.string.killer_filter_user_apps))
        popup.menu.add(0, 1, 1, getString(R.string.killer_filter_system_apps))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> {
                    currentFilter = FilterType.USER_APPS
                    tvFilterLabel.text = getString(R.string.killer_filter_user_apps)
                    applyFilter()
                }
                1 -> {
                    currentFilter = FilterType.SYSTEM_APPS
                    tvFilterLabel.text = getString(R.string.killer_filter_system_apps)
                    applyFilter()
                }
            }
            true
        }
        popup.show()
    }

    private fun showAppMenu(item: KillerAppItem, anchor: android.view.View) {
        val popup = PopupMenu(this, anchor)
        val isExc = Prefs.isException(item.packageName)
        popup.menu.add(0, 0, 0, getString(R.string.killer_menu_app_info))
        popup.menu.add(
            0, 1, 1,
            if (isExc) getString(R.string.killer_menu_remove_exception)
            else getString(R.string.killer_menu_add_exception)
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

    private fun showMainMenu(anchor: android.view.View) {
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
        // Refresh
        loadApps()
    }

    // ==== KILL ====

    private fun startKilling() {
        val selected = adapter.getItems().filter { it.selected && !it.isSystem }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Tidak ada aplikasi dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, R.string.killer_test_no_accessibility, Toast.LENGTH_LONG).show()
            return
        }

        // Konfirmasi
        AlertDialog.Builder(this)
            .setTitle("Hentikan ${selected.size} aplikasi?")
            .setMessage("Aplikasi akan dihentikan paksa.")
            .setPositiveButton("Hentikan") { _, _ ->
                executeKill(selected)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeKill(apps: List<KillerAppItem>) {
        val tasks = apps.map { app ->
            val steps = mutableListOf<ActionStep>()
            steps.add(ActionStep.OpenAppInfo(app.packageName))
            steps.add(ActionStep.Wait(800))
            // Kalau app di exception → skip force stop, langsung clear cache
            if (!Prefs.isException(app.packageName)) {
                steps.add(ActionStep.ClickByText(OemProfile.forceStopButtonLabels))
                steps.add(ActionStep.Wait(500))
                steps.add(ActionStep.ClickByText(OemProfile.forceStopConfirmLabels))
                steps.add(ActionStep.Wait(500))
            }
            steps.add(ActionStep.Back)
            AutomationTask(app.packageName, steps)
        }

        val service = AutomationAccessibilityService.instance ?: return
        service.runQueue(
            tasks = tasks,
            onProgress = { _, _, pkg -> runOnUiThread {
                Toast.makeText(this, "Processing: $pkg", Toast.LENGTH_SHORT).show()
            }},
            onComplete = { stats -> runOnUiThread {
                Toast.makeText(
                    this,
                    "Selesai: ${stats.success} sukses, ${stats.failed} gagal",
                    Toast.LENGTH_LONG
                ).show()
                loadApps()
            }}
        )
    }
}
