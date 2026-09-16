package com.toolsku.app.cleaner

import android.graphics.drawable.Drawable

/**
 * Data app untuk Cleaner.
 */
data class CleanerAppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val cacheSize: Long,
    val isSystem: Boolean,
    var selected: Boolean = true
) {
    fun formatCacheSize(): String {
        return when {
            cacheSize < 1024 -> "$cacheSize B"
            cacheSize < 1024 * 1024 -> String.format("%.0f KB", cacheSize / 1024.0)
            cacheSize < 1024 * 1024 * 1024 -> String.format("%.1f MB", cacheSize / (1024.0 * 1024))
            else -> String.format("%.2f GB", cacheSize / (1024.0 * 1024 * 1024))
        }
    }
}
