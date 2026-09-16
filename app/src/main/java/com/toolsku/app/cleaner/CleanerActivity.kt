package com.toolsku.app.cleaner

import android.os.Bundle
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
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.CacheSizeFetcher
import kotlinx.coroutines.launch

class CleanerActivity : AppCompatActivity() {

    private lateinit var adapter: CleanerAdapter
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvTotalCache: TextView
    private lateinit var tvAppCount: TextView
    private lateinit var btnSelectAll: ImageButton
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnClear: Button
    private lateinit var tvLoading: TextView

    private var isProcessing = false

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

    private fun loadApps() {
        tvLoading.visibility = View.VISIBLE
        tvLoading.text = getString(R.string.cleaner_loading)

        lifecycleScope.launch {
            try {
                val userApps = AppRepository.getInstalledApps(this@CleanerActivity, includeSystem = false)
                val systemApps = AppRepository.getSafeSystemApps(this@CleanerActivity)
                val allApps = (userApps + systemApps).distinctBy { it.packageName }

                val cacheSizes = CacheSizeFetcher.getCacheSizes(
                    this@CleanerActivity,
                    allApps.map { it.packageName }
                )

                val items = allApps.map { app ->
                    CleanerAppItem(
                        packageName = app.packageName,
                        label = app.label,
                        icon = app.icon,
                        cacheSize = cacheSizes[app.packageName] ?: 0L,
                        isSystem = app.isSystem,
                        selected = true
                    )
                }.sortedByDescending { it.cacheSize }

                adapter.submitList(items)
                updateTotals(items)
                updateSelectAllIcon()
                updateButtonCount()

                tvLoading.visibility = View.GONE
            } catch (e: Exception) {
                tvLoading.text = "Error: ${e.message}"
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
                    loadApps()
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
