package com.toolsku.app.killer.core

/**
 * Alasan queue dibatalkan.
 * Tiru dari `aw` Baxa.
 */
enum class CancelReason {
    NONE,
    USER_CANCELLED,
    HOME_BUTTON,
    SCREEN_OFF,
    ACCESSIBILITY_ERROR
}
