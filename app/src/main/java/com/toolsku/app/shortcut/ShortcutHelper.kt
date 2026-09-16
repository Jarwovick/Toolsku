package com.toolsku.app.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.toolsku.app.R

/**
 * Helper untuk membuat shortcut "Toolsku Quick" di home screen.
 */
object ShortcutHelper {

    private const val SHORTCUT_ID = "toolsku_quick"

    /**
     * Minta Android untuk pin shortcut ke home screen.
     * User akan lihat dialog konfirmasi dari Android.
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

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Gagal buat shortcut: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
