package com.toolsku.app.dimmer

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Mengelola overlay hitam transparan untuk peredupan layar.
 * Menggunakan WindowManager dengan TYPE_APPLICATION_OVERLAY.
 */
class DimmerOverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var currentAlpha: Float = 0.5f

    /**
     * Tampilkan overlay dengan alpha tertentu.
     * @param alpha 0.0 = transparan (tidak ada peredupan), 1.0 = hitam penuh
     */
    fun show(alpha: Float) {
        currentAlpha = alpha.coerceIn(0f, 1f)

        if (overlayView == null) {
            overlayView = createOverlayView()
        }

        val params = buildLayoutParams()
        val view = overlayView ?: return
        view.setBackgroundColor(applyAlphaToBlack(currentAlpha))

        try {
            if (view.parent == null) {
                windowManager.addView(view, params)
            } else {
                windowManager.updateViewLayout(view, params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Update level peredupan tanpa hapus overlay.
     */
    fun update(alpha: Float) {
        currentAlpha = alpha.coerceIn(0f, 1f)
        overlayView?.setBackgroundColor(applyAlphaToBlack(currentAlpha))
    }

    /**
     * Sembunyikan dan hapus overlay.
     */
    fun hide() {
        overlayView?.let { view ->
            try {
                if (view.parent != null) {
                    windowManager.removeView(view)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayView = null
    }

    private fun createOverlayView(): View {
        return FrameLayout(context).apply {
            isClickable = false
            isFocusable = false
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun applyAlphaToBlack(alpha: Float): Int {
        // Hitam dengan alpha (0 = transparan, 255 = opaque)
        val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
        return android.graphics.Color.argb(alphaInt, 0, 0, 0)
    }
}
