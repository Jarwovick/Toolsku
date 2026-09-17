package com.toolsku.app.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.toolsku.app.R

/**
 * Helper untuk membuat shortcut "Toolsku Quick" di home screen.
 */
object ShortcutHelper {

    private const val TAG = "ShortcutHelper"
    private const val SHORTCUT_ID = "toolsku_quick"

    /**
     * Minta Android untuk pin shortcut ke home screen.
     */
    fun requestPinShortcut(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(
                context,
                "Butuh Android 8.0+ untuk shortcut",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            if (shortcutManager == null) {
                Toast.makeText(context, "ShortcutManager tidak tersedia", Toast.LENGTH_LONG).show()
                return
            }

            if (!shortcutManager.isRequestPinShortcutSupported) {
                Toast.makeText(
                    context,
                    "Perangkat tidak mendukung pin shortcut",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // Intent ke QuickActionActivity
            val intent = Intent(context, QuickActionActivity::class.java).apply {
                action = Intent.ACTION_VIEW
            }

            val shortcutInfo = ShortcutInfo.Builder(context, SHORTCUT_ID)
                .setShortLabel(context.getString(R.string.shortcut_short_label))
                .setLongLabel(context.getString(R.string.shortcut_long_label))
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()

            shortcutManager.requestPinShortcut(shortcutInfo, null)

            Toast.makeText(
                context,
                "Konfirmasi dialog untuk tambah shortcut",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to pin shortcut", e)
            Toast.makeText(
                context,
                "Gagal buat shortcut: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Cek apakah shortcut sudah dipasang.
     */
    fun isShortcutPinned(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        return try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            val pinnedShortcuts = shortcutManager?.pinnedShortcuts ?: return false
            pinnedShortcuts.any { it.id == SHORTCUT_ID }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hapus shortcut (kalau ada).
     */
    fun removeShortcut(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager?.removeDynamicShortcuts(listOf(SHORTCUT_ID))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove shortcut", e)
        }
    }
}
