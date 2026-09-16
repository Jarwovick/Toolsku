package com.toolsku.app.killer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toolsku.app.R

class CleanerAdapter(
    private val onItemClick: (CleanerAppItem) -> Unit
) : RecyclerView.Adapter<CleanerAdapter.ViewHolder>() {

    private val items = mutableListOf<CleanerAppItem>()

    fun submitList(newItems: List<CleanerAppItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<CleanerAppItem> = items

    fun selectAll(selected: Boolean) {
        var changed = false
        for (i in items.indices) {
            if (items[i].selected != selected) {
                items[i].selected = selected
                changed = true
            }
        }
        if (changed) notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_cleaner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvCacheSize: TextView = itemView.findViewById(R.id.tvCacheSize)
        private val cbSelected: CheckBox = itemView.findViewById(R.id.cbSelected)

        fun bind(item: CleanerAppItem) {
            tvAppName.text = item.label
            tvCacheSize.text = item.formatCacheSize()
            ivAppIcon.setImageDrawable(item.icon)
            cbSelected.isChecked = item.selected

            itemView.setOnClickListener {
                item.selected = !item.selected
                cbSelected.isChecked = item.selected
                onItemClick(item)
            }

            cbSelected.setOnClickListener {
                item.selected = cbSelected.isChecked
                onItemClick(item)
            }
        }
    }
}
