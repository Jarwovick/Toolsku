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
import com.toolsku.app.automation.ActionStep
import com.toolsku.app.automation.AppTracker
import com.toolsku.app.automation.AutomationAccessibilityService
import com.toolsku.app.automation.AutomationTask
import com.toolsku.app.automation.OemProfile
import com.toolsku.app.core.AppInfo
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.Prefs
import com.toolsku.app.core.db.DatabaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        btnRefresh.setOnClickListener {
            loadApps()
        }

        btnSelectAll.setOnClickListener {
            val allCurrentlySelected = adapter.getAppItems().all { it.selected }
            val newState = !allCurrentlySelected
            adapter.selectAll(newState)
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
        if (!isProcessing) {
            loadApps()
        }
    }

    /**
     * Handle Intent baru — saat forceBackToKiller dipanggil.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra("refresh_after_kill", false)) {
            Log.i(TAG, "onNewIntent: refresh after kill")
            // Reset isProcessing karena kita baru balik dari kill
            isProcessing = false
            updateButtonCount()

            // Refresh daftar
            loadApps()
            updateRamInfo()
        }
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
        val selected = adapter.getAppItems().filter { it.selected }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Tidak ada aplikasi dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, R.string.killer_test_no_accessibility, Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Hentikan ${selected.size} aplikasi?")
            .setMessage("Aplikasi akan dihentikan paksa.")
            .setPositiveButton("Hentikan") { _, _ -> executeKill(selected) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeKill(apps: List<KillerAppItem>) {
        isProcessing = true
        updateButtonCount()

        val service = AutomationAccessibilityService.instance ?: return

        AutomationAccessibilityService.onStopClick = {
            service.cancelQueue()
            service.hideOverlay()
            isProcessing = false
            updateButtonCount()
            Toast.makeText(this, "Dibatalkan", Toast.LENGTH_SHORT).show()
        }

        service.showOverlay(0, apps.size, "", AutomationAccessibilityService.MODE_KILLER)

        val tasks = apps.map { app ->
            val steps = mutableListOf<ActionStep>()
            steps.add(ActionStep.OpenAppInfo(app.packageName))
            steps.add(ActionStep.Wait(300))
            steps.add(ActionStep.ClickByText(OemProfile.forceStopButtonLabels))
            steps.add(ActionStep.Wait(200))
            steps.add(ActionStep.ClickByText(OemProfile.forceStopConfirmLabels))
            steps.add(ActionStep.Wait(200))
            // Cukup 1x Back — sisanya di-force oleh Intent
            steps.add(ActionStep.Back)
            steps.add(ActionStep.Wait(150))
            AutomationTask(app.packageName, app.label, steps)
        }

        service.runQueue(
            tasks = tasks,
            onProgress = { current, total, appLabel ->
                service.updateOverlay(current, total, appLabel)
            },
            onComplete = { stats ->
                runOnUiThread {
                    // 1. Sembunyikan overlay
                    service.hideOverlay()
                    AutomationAccessibilityService.onStopClick = null
                    isProcessing = false
                    updateButtonCount()

                    Toast.makeText(
                        this,
                        "Selesai: ${stats.success} sukses, ${stats.failed} gagal",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 2. Mark app sebagai closed di DB
                    markAsClosed(apps)

                    // 3. ⭐ FORCE buka KillerActivity
                    forceBackToKiller()

                    // 4. Setelah 4 detik, cek auto-restart
                    handler.postDelayed({
                        checkAutoRestart(apps)
                    }, 4000)
                }
            }
        )
    }

    /**
     * Force kembali ke KillerActivity — hapus semua activity di atasnya.
     */
    private fun forceBackToKiller() {
        try {
            val intent = Intent(this, KillerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP 
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP 
                        or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("refresh_after_kill", true)
            }
            startActivity(intent)
            Log.i(TAG, "Force back to Killer via Intent")
        } catch (e: Exception) {
            Log.e(TAG, "forceBackToKiller failed", e)
            finish()
        }
    }

    /**
     * Mark app yang di-kill sebagai "closed" di database.
     */
    private fun markAsClosed(apps: List<KillerAppItem>) {
        lifecycleScope.launch {
            try {
                val dao = DatabaseProvider.runningAppDao()
                val packages = apps.map { it.packageName }

                Log.i(TAG, "Marking ${packages.size} apps as closed")

                packages.forEach { pkg ->
                    dao.markClosed(pkg)
                }

                Log.i(TAG, "Marked as closed successfully")

                // Refresh daftar setelah 500ms
                delay(500)
                loadApps()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark as closed", e)
            }
        }
    }

    /**
     * Cek app yang auto-restart setelah kill.
     */
    private fun checkAutoRestart(killedApps: List<KillerAppItem>) {
        lifecycleScope.launch {
            try {
                delay(3000)

                val dao = DatabaseProvider.runningAppDao()
                var autoRestartCount = 0

                killedApps.forEach { app ->
                    val entity = dao.getApp(app.packageName)
                    if (entity != null && !entity.isClosed) {
                        dao.markAutoRestarted(app.packageName)
                        autoRestartCount++
                        Log.i(TAG, "Auto-restart: ${app.packageName}")
                    }
                }

                if (autoRestartCount > 0) {
                    Toast.makeText(
                        this@KillerActivity,
                        "$autoRestartCount app restart otomatis",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                loadApps()
            } catch (e: Exception) {
                Log.e(TAG, "checkAutoRestart failed", e)
            }
        }
    }
}
