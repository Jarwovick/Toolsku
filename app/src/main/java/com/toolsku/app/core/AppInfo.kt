package com.toolsku.app.core

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isException: Boolean,
    val cacheSize: Long = 0L,
    var selected: Boolean = false
)
