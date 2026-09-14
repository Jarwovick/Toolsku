package com.toolsku.app

import android.app.Application
import com.toolsku.app.core.Prefs

/**
 * Application class Toolsku.
 * Inisialisasi Prefs saat app pertama dibuka.
 */
class ToolskuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
