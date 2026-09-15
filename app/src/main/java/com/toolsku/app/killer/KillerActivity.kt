package com.toolsku.app.killer

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import com.toolsku.app.automation.ActionStep
import com.toolsku.app.automation.AutomationAccessibilityService
import com.toolsku.app.automation.AutomationTask
import com.toolsku.app.automation.OemProfile
import com.toolsku.app.core.AppInfo
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.Prefs
import kotlinx.coroutines.launch

class KillerActivity : AppCompatActivity() {

    enum class FilterType { USER_APPS, SYSTEM_APPS }

    private lateinit var adapter: KillerAdapter
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvFilterLabel: TextView
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

    private var allSelected = true
    private var isProcessing = false
    private var currentFilter = FilterType.USER_APPS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_killer)
        title = getString(R.string.killer_title)

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvFilterLabel = findViewById(R.id.tvFilterLabel)
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
        tvFilterLabel.text = getString(R.string.killer_filter_user_apps)

        adapter = KillerAdapter(
            onItemClick = { updateButtonCount() },
            onMenuClick = { item, view -> showAppMenu(item, view) }
        )
        findViewById<RecyclerView>(R.id.rvApps).apply {
            layoutManager = LinearLayoutManager(this@KillerActivity)
            adapter = this@KillerActivity.adapter
        }

        findViewById<android.widget.LinearLayout>(R.id.btnFilter).setOnClickListener {
            showFilterMenu()
        }

        btnRefresh.setOnClickListener {
            loadApps()
        }

        btnSelectAll.setOnClickListener {
            allSelected = !allSelected
            adapter.selectAll(allSelected)
            updateSelectAllIcon()
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
        if (!isProcessing) {
            loadApps()
        }
    }

    // ==== DATA ====

    private fun loadApps() {
        tvLoading.visibility = View.VISIBLE
        tvLoading.text = getString(R.string.killer_loading)

        lifecycleScope.launch {
            val apps = when (currentFilter) {
                FilterType.USER_APPS -> AppRepository.getRunningApps(this@KillerActivity)
                FilterType.SYSTEM_APPS -> AppRepository.getSafeSystemApps(this@KillerActivity)
            }.map { it.toKillerItem() }

            adapter.submitList(apps)
            allSelected = apps.isNotEmpty()
            updateSelectAllIcon()
            updateButtonCount()
            updateLaunchedAppsCount(apps.size)
            updateRamInfo()

            if (apps.isEmpty()) {
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

    private fun showFilterMenu() {
        val popup = PopupMenu(this, findViewById(R.id.btnFilter))
        popup.menu.add(0, 0, 0, getString(R.string.killer_filter_user_apps))
        popup.menu.add(0, 1, 1, getString(R.string.killer_filter_system_apps))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> {
                    currentFilter = FilterType.USER_APPS
                    tvFilterLabel.text = getString(R.string.killer_filter_user_apps)
                    loadApps()
                }
                1 -> {
                    currentFilter = FilterType.SYSTEM_APPS
                    tvFilterLabel.text = getString(R.string.killer_filter_system_apps)
                    loadApps()
                }
            }
            true
        }
        popup.show()
    }

    // ==== RAM ====

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

    // ==== UI ====

    private fun updateButtonCount() {
        val count = adapter.getItems().count { it.selected }
        btnCloseApps.text = if (count == 0) {
            getString(R.string.killer_close_apps_zero)
        } else {
            getString(R.string.killer_close_apps_count, count)
        }
        btnCloseApps.isEnabled = count > 0 && !isProcessing
        btnCloseApps.alpha = if (btnCloseApps.isEnabled) 1f else 0.5f
    }

    private fun updateSelectAllIcon() {
        btnSelectAll.setImageResource(
            if (allSelected) R.drawable.ic_checkbox_checked
            else R.drawable.ic_checkbox_unchecked
        )
        btnSelectAll.imageTintList = android.content.res.ColorStateList.valueOf(
            if (allSelected) {
                ContextCompat.getColor(this, R.color.accent)
            } else {
                ContextCompat.getColor(this, R.color.text_secondary)
            }
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

    // ==== KILL ====

    private fun startKilling() {
        val selected = adapter.getItems().filter { it.selected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Tidak ada aplikasi dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, R.string.killer_test_no_accessibility, Toast.LENGTH_LONG).show()
            return
        }

        // Kill langsung tanpa dialog (untuk system apps) atau dengan dialog (user apps)
        if (currentFilter == FilterType.SYSTEM_APPS) {
            // System apps: langsung kill tanpa dialog
            executeKill(selected)
        } else {
            // User apps: dengan dialog konfirmasi
            AlertDialog.Builder(this)
                .setTitle("Hentikan ${selected.size} aplikasi?")
                .setMessage("Aplikasi akan dihentikan paksa.")
                .setPositiveButton("Hentikan") { _, _ -> executeKill(selected) }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun executeKill(apps: List<KillerAppItem>) {
        isProcessing = true
        updateButtonCount()

        val tasks = apps.map { app ->
            val steps = mutableListOf<ActionStep>()
            steps.add(ActionStep.OpenAppInfo(app.packageName))
            steps.add(ActionStep.Wait(300))
            steps.add(ActionStep.ClickByText(OemProfile.forceStopButtonLabels))
            steps.add(ActionStep.Wait(200))
            steps.add(ActionStep.ClickByText(OemProfile.forceStopConfirmLabels))
            steps.add(ActionStep.Wait(200))
            steps.add(ActionStep.Back)
            steps.add(ActionStep.Wait(150))
            AutomationTask(app.packageName, steps)
        }

        val service = AutomationAccessibilityService.instance ?: return
        service.runQueue(
            tasks = tasks,
            onProgress = { _, _, _ -> },
            onComplete = { stats ->
                runOnUiThread {
                    isProcessing = false
                    updateButtonCount()

                    val intent = Intent(this, KillerActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)

                    Toast.makeText(
                        this,
                        "Selesai: ${stats.success} sukses, ${stats.failed} gagal",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}
