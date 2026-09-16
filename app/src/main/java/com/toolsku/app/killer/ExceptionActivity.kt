package com.toolsku.app.killer

import android.content.pm.PackageManager
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
import com.toolsku.app.core.AppInfo
import com.toolsku.app.core.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExceptionActivity : AppCompatActivity() {

    private lateinit var adapter: ExceptionAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exception)
        title = getString(R.string.exception_title)

        findViewById<TextView>(R.id.tvHeaderTitle).text =
            getString(R.string.header_exception_list)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = ExceptionAdapter(
            onRemoveClick = { pkg -> removeException(pkg) }
        )
        findViewById<RecyclerView>(R.id.rvException).apply {
            layoutManager = LinearLayoutManager(this@ExceptionActivity)
            adapter = this@ExceptionActivity.adapter
        }

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showSelectAppsDialog()
        }

        loadExceptions()
    }

    private fun loadExceptions() {
        lifecycleScope.launch {
            val exceptionPackages = Prefs.exceptionList

            if (exceptionPackages.isEmpty()) {
                adapter.submitList(emptyList())
                tvEmpty.visibility = View.VISIBLE
                return@launch
            }

            // AMBIL LANGSUNG per-package (cepat!)
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                exceptionPackages.mapNotNull { pkg ->
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        AppInfo(
                            packageName = pkg,
                            label = pm.getApplicationLabel(appInfo).toString(),
                            icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                            isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                            isException = true
                        )
                    } catch (e: PackageManager.NameNotFoundException) {
                        null  // App sudah di-uninstall
                    }
                }.sortedBy { it.label.lowercase() }
            }

            adapter.submitList(apps)

            if (apps.isEmpty()) {
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
