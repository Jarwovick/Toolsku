package com.toolsku.app.cleaner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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
import com.toolsku.app.core.CacheSizeFetcher
import kotlinx.coroutines.launch

class CleanerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CleanerActivity"
        private const val MIN_CACHE_SIZE = 10 * 1024 * 1024L  // 10 MB
    }

    private lateinit var adapter: CleanerAdapter
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvTotalCache: TextView
    private lateinit var tvAppCount: TextView
    private lateinit var btnSelectAll: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnClear: Button
    private lateinit var tvLoading: TextView

    private var isProcessing = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleaner)
        title = getString(R.string.cleaner_title)

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        tvTotalCache = findViewById(R.id.tvTotalCache)
        tvAppCount = findViewById(R.id.tvAppCount)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnClear = findViewById(R.id.btnClear)
        tvLoading = findViewById(R.id.tvLoading)

        tvHeaderTitle.text = getString(R.string.header_clear_cache)

        adapter = CleanerAdapter(
            onItemClick = { updateButtonCount(); updateSelectAllIcon() }
        )
        findViewById<RecyclerView>(R.id.rvApps).apply {
            layoutManager = LinearLayoutManager(this@CleanerActivity)
            adapter = this@CleanerActivity.adapter
        }

        btnRefresh.setOnClickListener { loadApps() }

        btnSelectAll.setOnClickListener {
            val allSelected = adapter.getItems().all { it.selected }
            adapter.selectAll(!allSelected)
            updateSelectAllIcon()
            updateButtonCount()
        }

        btnClear.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            startCleaning()
        }

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        if (!isProcessing) {
            loadApps()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra("refresh_after_clean", false)) {
            Log.i(TAG, "onNewIntent: refresh after clean")
            isProcessing = false
            updateButtonCount()
            loadApps()
        }
    }

    // ==== DATA ====

    private fun loadApps() {
        tvLoading.visibility = View.VISIBLE
        tvLoading.text = getString(R.string.cleaner_loading)

        lifecycleScope.launch {
            try {
                Log.i(TAG, "Loading apps...")

                // ⭐ Pakai getAllAppsForCleaner — TIDAK ada exception filter
                val allApps: List<AppInfo> = AppRepository.getAllAppsForCleaner(this@CleanerActivity)
                Log.i(TAG, "Total apps: ${allApps.size}")

                // Hitung cache size
                val cacheSizes: Map<String, Long> = CacheSizeFetcher.getCacheSizes(
                    this@CleanerActivity,
                    allApps.map { it.packageName }
                )
                Log.i(TAG, "Cache sizes: ${cacheSizes.size}")

                // Filter cache >= 10 MB
                val items: List<CleanerAppItem> = allApps
                    .map { app ->
                        CleanerAppItem(
                            packageName = app.packageName,
                            label = app.label,
                            icon = app.icon,
                            cacheSize = cacheSizes[app.packageName] ?: 0L,
                            isSystem = app.isSystem,
                            selected = true
                        )
                    }
                    .filter { it.cacheSize >= MIN_CACHE_SIZE }
                    .sortedByDescending { it.cacheSize }

                Log.i(TAG, "Apps with cache >= 10MB: ${items.size}")

                adapter.submitList(items)
                updateTotals(items)
                updateSelectAllIcon()
                updateButtonCount()

                if (items.isEmpty()) {
                    tvLoading.visibility = View.VISIBLE
                    tvLoading.text = "Tidak ada cache ≥ 10 MB"
                } else {
                    tvLoading.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e(TAG, "loadApps failed", e)
                tvLoading.visibility = View.VISIBLE
                tvLoading.text = "Error: ${e.message}"
                // ⭐ JANGAN finish() — biar user lihat error
            }
        }
    }

    private fun updateTotals(items: List<CleanerAppItem>) {
        val total = items.sumOf { it.cacheSize }
        tvTotalCache.text = formatSize(total)
        tvAppCount.text = "${items.size} apps"
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.0f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    private fun updateButtonCount() {
        val count = adapter.getItems().count { it.selected }
        btnClear.text = if (count == 0) {
            getString(R.string.cleaner_clear_all_zero)
        } else {
            getString(R.string.cleaner_clear_all, count)
        }
        btnClear.isEnabled = count > 0 && !isProcessing
        btnClear.alpha = if (btnClear.isEnabled) 1f else 0.5f
    }

    private fun updateSelectAllIcon() {
        val items = adapter.getItems()
        val allSelected = items.isNotEmpty() && items.all { it.selected }
        btnSelectAll.setImageResource(
            if (allSelected) R.drawable.ic_checkbox_checked
            else R.drawable.ic_checkbox_unchecked
        )
        btnSelectAll.imageTintList = android.content.res.ColorStateList.valueOf(
            if (allSelected) ContextCompat.getColor(this, R.color.accent)
            else ContextCompat.getColor(this, R.color.text_secondary)
        )
    }

    // ==== CLEAN ====

    private fun startCleaning() {
        val selected = adapter.getItems().filter { it.selected && it.cacheSize > 0 }
        if (selected.isEmpty()) {
            Toast.makeText(this, "Tidak ada cache untuk dibersihkan", Toast.LENGTH_SHORT).show()
            return
        }

        val service = AutomationAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, R.string.killer_test_no_accessibility, Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Bersihkan cache ${selected.size} aplikasi?")
            .setMessage("Cache akan dihapus.")
            .setPositiveButton("Bersihkan") { _, _ -> executeClean(selected) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun executeClean(apps: List<CleanerAppItem>) {
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

        service.showOverlay(0, apps.size, "", AutomationAccessibilityService.MODE_CLEANER)

        val tasks = apps.map { app ->
            val steps = mutableListOf<ActionStep>()
            steps.add(ActionStep.OpenAppInfo(app.packageName))
            steps.add(ActionStep.Wait(400))
            steps.add(ActionStep.ClickByText(OemProfile.storageMenuLabels))
            steps.add(ActionStep.Wait(400))
            steps.add(ActionStep.ClickByTextSafe(
                OemProfile.clearCacheLabels,
                OemProfile.clearDataLabels
            ))
            steps.add(ActionStep.Wait(400))
            steps.add(ActionStep.Back)
            steps.add(ActionStep.Wait(200))
            steps.add(ActionStep.Back)
            steps.add(ActionStep.Wait(200))
            AutomationTask(app.packageName, app.label, steps)
        }

        service.runQueue(
            tasks = tasks,
            onProgress = { current, total, appLabel ->
                service.updateOverlay(current, total, appLabel)
            },
            onComplete = { stats ->
                runOnUiThread {
                    service.hideOverlay()
                    AutomationAccessibilityService.onStopClick = null
                    isProcessing = false
                    updateButtonCount()

                    Toast.makeText(
                        this,
                        "Selesai: ${stats.success} sukses, ${stats.failed} gagal",
                        Toast.LENGTH_SHORT
                    ).show()

                    forceBackToCleaner()

                    handler.postDelayed({
                        loadApps()
                    }, 1000)
                }
            }
        )
    }

    private fun forceBackToCleaner() {
        try {
            val intent = Intent(this, CleanerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("refresh_after_clean", true)
            }
            startActivity(intent)
            Log.i(TAG, "Force back to Cleaner via Intent")
        } catch (e: Exception) {
            Log.e(TAG, "forceBackToCleaner failed", e)
            finish()
        }
    }
}
