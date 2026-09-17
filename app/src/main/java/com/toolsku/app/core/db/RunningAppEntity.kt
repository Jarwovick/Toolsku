package com.toolsku.app.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity untuk track running apps.
 */
@Entity(tableName = "running_apps")
data class RunningAppEntity(
    @PrimaryKey
    val packageName: String,

    /** Timestamp terakhir app ini dibuka */
    val lastUsed: Long = System.currentTimeMillis(),

    /** App yang auto-restart setelah di-kill */
    val isAutoRestarted: Boolean = false,

    /** App yang tidak bisa di-kill (system apps penting) */
    val isUnclosable: Boolean = false,

    /** App yang user tutup manual (tidak muncul di running list) */
    val isClosed: Boolean = false
)
