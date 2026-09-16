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

class KillerAdapter(
    private val onItemClick: (KillerAppItem) -> Unit,
    private val onMenuClick: (KillerAppItem, View) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_APP = 0
        private const val TYPE_HEADER = 1
    }

    private val items = mutableListOf<KillerAppItem>()

    fun submitList(newItems: List<KillerAppItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItems(): List<KillerAppItem> = items

    /**
     * Ambil semua app (bukan header).
     */
    fun getAppItems(): List<KillerAppItem> = items.filter { !it.isSectionHeader }

    /**
     * Select all / deselect all app (bukan header).
     */
    fun selectAll(selected: Boolean) {
        var changed = false
        for (i in items.indices) {
            if (!items[i].isSectionHeader) {
                if (items[i].selected != selected) {
                    items[i].selected = selected
                    changed = true
                }
            }
        }
        if (changed) {
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isSectionHeader) TYPE_HEADER else TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_killer, parent, false)
            AppViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind(item.headerTitle)
            is AppViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSectionTitle: TextView = itemView.findViewById(R.id.tvSectionTitle)

        fun bind(title: String) {
            tvSectionTitle.text = title
        }
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
