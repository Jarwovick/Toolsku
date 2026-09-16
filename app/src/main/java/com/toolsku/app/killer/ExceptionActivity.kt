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

    override fun onResume() {
        super.onResume()
        loadExceptions()
    }

    private fun loadExceptions() {
        lifecycleScope.launch {
            // Pakai method khusus: cari LANGSUNG per package
            val apps = AppRepository.getExceptionApps(this@ExceptionActivity)

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
