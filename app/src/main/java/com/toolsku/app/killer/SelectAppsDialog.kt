package com.toolsku.app.killer

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toolsku.app.R
import com.toolsku.app.core.AppInfo
import com.toolsku.app.core.AppRepository
import com.toolsku.app.core.Prefs
import kotlinx.coroutines.launch

class SelectAppsDialog : DialogFragment() {

    companion object {
        private const val TAG = "SelectAppsDialog"
    }

    private lateinit var adapter: SelectAppsAdapter
    private lateinit var tvFilterLabel: TextView
    private lateinit var btnSelectAll: ImageButton
    private var currentFilter = FilterType.USER_APPS

    private var allUserApps: List<AppInfo> = emptyList()
    private var allSystemApps: List<AppInfo> = emptyList()

    private var onSelectionComplete: ((Set<String>) -> Unit)? = null

    enum class FilterType { USER_APPS, SYSTEM_APPS }

    fun setOnSelectionComplete(listener: (Set<String>) -> Unit) {
        onSelectionComplete = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_select_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvFilterLabel = view.findViewById(R.id.tvFilterLabel)
        btnSelectAll = view.findViewById(R.id.btnSelectAll)

        adapter = SelectAppsAdapter(
            onItemClick = { updateSelectAllIcon() }
        )

        view.findViewById<RecyclerView>(R.id.rvApps).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SelectAppsDialog.adapter
        }

        view.findViewById<LinearLayout>(R.id.btnFilter).setOnClickListener {
            showFilterMenu()
        }

        btnSelectAll.setOnClickListener {
            val newState = !adapter.isAllSelected()
            adapter.selectAll(newState)
            updateSelectAllIcon()
        }

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }

        view.findViewById<Button>(R.id.btnAdd).setOnClickListener {
            val selected = adapter.getSelectedPackages()
            Log.i(TAG, "User selected: ${selected.size} apps")
            selected.forEach { Log.d(TAG, "  - $it") }
            onSelectionComplete?.invoke(selected)
            dismiss()
        }

        loadApps()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun loadApps() {
        lifecycleScope.launch {
            // Pakai getAllApps — tidak ada filter, semua app muncul
            allUserApps = AppRepository.getAllApps(requireContext(), onlyUser = true)
            allSystemApps = AppRepository.getAllApps(requireContext(), onlyUser = false)
                .filter { it.isSystem }

            Log.i(TAG, "User apps: ${allUserApps.size}, System apps: ${allSystemApps.size}")

            applyFilter()
        }
    }

    private fun applyFilter() {
        val list = when (currentFilter) {
            FilterType.USER_APPS -> allUserApps
            FilterType.SYSTEM_APPS -> allSystemApps
        }
        val preselected = Prefs.exceptionList
        adapter.submitList(list, preselected)
        updateSelectAllIcon()
    }

    private fun updateSelectAllIcon() {
        btnSelectAll.setImageResource(
            if (adapter.isAllSelected()) R.drawable.ic_checkbox_checked
            else R.drawable.ic_checkbox_unchecked
        )
    }

    private fun showFilterMenu() {
        val popup = PopupMenu(requireContext(), view?.findViewById(R.id.btnFilter))
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
}
