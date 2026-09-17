package com.toolsku.app.killer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toolsku.app.R
import com.toolsku.app.core.AppInfo

class SelectAppsAdapter(
    private val onItemClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<SelectAppsAdapter.ViewHolder>() {

    private val items = mutableListOf<AppInfo>()
    private val selectedPackages = mutableSetOf<String>()

    fun submitList(newItems: List<AppInfo>, preselected: Set<String>) {
        items.clear()
        items.addAll(newItems)
        selectedPackages.clear()
        selectedPackages.addAll(preselected)
        notifyDataSetChanged()
    }

    fun getSelectedPackages(): Set<String> = selectedPackages.toSet()

    fun selectAll(selected: Boolean) {
        selectedPackages.clear()
        if (selected) {
            items.forEach { selectedPackages.add(it.packageName) }
        }
        notifyDataSetChanged()
    }

    fun isAllSelected(): Boolean {
        return items.isNotEmpty() && selectedPackages.size == items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_select, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val cbSelected: CheckBox = itemView.findViewById(R.id.cbSelected)

        fun bind(item: AppInfo) {
            tvAppName.text = item.label
            ivAppIcon.setImageDrawable(item.icon)
            cbSelected.isChecked = selectedPackages.contains(item.packageName)

            itemView.setOnClickListener { toggleSelection(item) }
            cbSelected.setOnClickListener { toggleSelection(item) }
        }

        private fun toggleSelection(item: AppInfo) {
            if (selectedPackages.contains(item.packageName)) {
                selectedPackages.remove(item.packageName)
                cbSelected.isChecked = false
            } else {
                selectedPackages.add(item.packageName)
                cbSelected.isChecked = true
            }
            onItemClick(item)
        }
    }
}
