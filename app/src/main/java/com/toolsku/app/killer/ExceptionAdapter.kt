package com.toolsku.app.killer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.toolsku.app.R
import com.toolsku.app.core.AppInfo

class ExceptionAdapter(
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<ExceptionAdapter.ViewHolder>() {

    private val items = mutableListOf<AppInfo>()

    fun submitList(newItems: List<AppInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_exception, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(item: AppInfo) {
            tvAppName.text = item.label

            if (item.icon != null) {
                ivAppIcon.setImageDrawable(item.icon)
            } else {
                ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            btnRemove.setOnClickListener {
                onRemoveClick(item.packageName)
            }
        }
    }
}
