package com.toolsku.app.killer

import android.graphics.drawable.Drawable

/**
 * Data satu aplikasi di halaman Killer.
 */
data class KillerAppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isException: Boolean,
    var selected: Boolean = false
)
