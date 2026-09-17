package com.toolsku.app.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_apps")
data class RunningAppEntity(
    @PrimaryKey
    val packageName: String,
    val lastUsed: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false,
    val isAutoRestarted: Boolean = false,
    val isUnclosable: Boolean = false,
    val isClosed: Boolean = false
)
