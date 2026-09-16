package com.toolsku.app.killer

import android.os.Bundle
import android.util.Log
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

    companion object {
        private const val TAG = "ToolskuException"
    }

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
            Log.i(TAG, "Prefs.exceptionList: ${exceptionPackages.size} — $exceptionPackages")

            val apps = AppRepository.getExceptionApps(this@ExceptionActivity)
            Log.i(TAG, "Loaded exception apps: ${apps.size}")

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
            Log.i(TAG, "Selected: ${selected.size}")

            val current = Prefs.exceptionList.toMutableSet()
            Log.i(TAG, "Before: ${current.size}")
            current.addAll(selected)
            Prefs.exceptionList = current

            val verify = Prefs.exceptionList
            Log.i(TAG, "After: ${verify.size} — $verify")

            Toast.makeText(
                this,
                "${selected.size} app ditambahkan (total: ${verify.size})",
                Toast.LENGTH_LONG
            ).show()

            loadExceptions()
        }
        dialog.show(supportFragmentManager, "SelectAppsDialog")
    }
}
