package com.toolsku.app.killer

import android.graphics.drawable.Drawable

data class KillerAppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isException: Boolean,
    var selected: Boolean = false,
    val isSectionHeader: Boolean = false,
    val headerTitle: String = ""
) {
    companion object {
        fun section(title: String): KillerAppItem {
            return KillerAppItem(
                packageName = "__section__$title",
                label = title,
                icon = null,
                isSystem = false,
                isException = false,
                selected = false,
                isSectionHeader = true,
                headerTitle = title
            )
        }
    }
}
