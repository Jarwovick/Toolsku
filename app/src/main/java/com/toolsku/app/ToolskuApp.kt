package com.toolsku.app

import android.app.Application
import com.toolsku.app.core.Prefs
import com.toolsku.app.core.db.DatabaseProvider

class ToolskuApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            Prefs.init(this)
            DatabaseProvider.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
