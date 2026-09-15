package com.toolsku.app.killer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toolsku.app.R

/**
 * Adapter untuk daftar app di halaman Killer.
 */
class KillerAdapter(
    private val onItemClick: (KillerAppItem) -> Unit,
    private val onMenuClick: (KillerAppItem, View) -> Unit
) : RecyclerView.Adapter<KillerAdapter.ViewHolder>() {

    private val items = mutableListOf<KillerAppItem>()

    fun submitList(newItems: List<KillerAppItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<KillerAppItem> = items

    fun updateSelected(pkg: String, selected: Boolean) {
        val index = items.indexOfFirst { it.packageName == pkg }
        if (index >= 0) {
            items[index].selected = selected
            notifyItemChanged(index)
        }
    }

    fun selectAll(selected: Boolean) {
        for (i in items.indices) {
            items[i].selected = selected
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_killer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val btnAppMenu: ImageButton = itemView.findViewById(R.id.btnAppMenu)
        private val cbSelected: CheckBox = itemView.findViewById(R.id.cbSelected)

        fun bind(item: KillerAppItem) {
            tvAppName.text = item.label
            ivAppIcon.setImageDrawable(item.icon)
            cbSelected.isChecked = item.selected

            // Klik item = toggle checkbox
            itemView.setOnClickListener {
                val newState = !item.selected
                item.selected = newState
                cbSelected.isChecked = newState
                onItemClick(item)
            }

            // Klik checkbox juga toggle
            cbSelected.setOnClickListener {
                val newState = cbSelected.isChecked
                item.selected = newState
                onItemClick(item)
            }

            // Klik menu 3 titik
            btnAppMenu.setOnClickListener {
                onMenuClick(item, it)
            }
        }
    }
}
