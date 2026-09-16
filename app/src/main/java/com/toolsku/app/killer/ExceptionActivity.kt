package com.toolsku.app.killer

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.toolsku.app.R
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.Prefs
import kotlinx.coroutines.launch

class ExceptionActivity : AppCompatActivity() {

    private lateinit var adapter: ExceptionAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exception)
        title = getString(R.string.exception_title)

        // Header
        findViewById<TextView>(R.id.tvHeaderTitle).text =
            getString(R.string.header_exception_list)

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Empty text
        tvEmpty = findViewById(R.id.tvEmpty)

        // Adapter
        adapter = ExceptionAdapter(
            onRemoveClick = { pkg -> removeException(pkg) }
        )
        findViewById<RecyclerView>(R.id.rvException).apply {
            layoutManager = LinearLayoutManager(this@ExceptionActivity)
            adapter = this@ExceptionActivity.adapter
        }

        // FAB Add
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showSelectAppsDialog()
        }

        loadExceptions()
    }

    override fun onResume() {
        super.onResume()
        loadExceptions()
    }

    private fun loadExceptions() {
        lifecycleScope.launch {
            val exceptionPackages = Prefs.exceptionList
            val installedApps = AppRepository.getInstalledApps(
                this@ExceptionActivity,
                includeSystem = true
            )
            val exceptionApps = installedApps.filter {
                exceptionPackages.contains(it.packageName)
            }

            adapter.submitList(exceptionApps)

            if (exceptionApps.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
            } else {
                tvEmpty.visibility = View.GONE
            }
        }
    }

    private fun removeException(pkg: String) {
        Prefs.removeException(pkg)
        Toast.makeText(this, R.string.exception_removed, Toast.LENGTH_SHORT).show()
        loadExceptions()
    }

    private fun showSelectAppsDialog() {
        val dialog = SelectAppsDialog()
        dialog.setOnSelectionComplete { selected ->
            // Simpan pilihan baru (gabung dengan yang lama)
            val current = Prefs.exceptionList.toMutableSet()
            current.addAll(selected)
            Prefs.exceptionList = current

            Toast.makeText(
                this,
                "${selected.size} app ditambahkan",
                Toast.LENGTH_SHORT
            ).show()
            loadExceptions()
        }
        dialog.show(supportFragmentManager, "SelectAppsDialog")
    }
}
