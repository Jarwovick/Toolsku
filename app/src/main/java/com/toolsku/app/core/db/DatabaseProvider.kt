package com.toolsku.app.core.db

import android.content.Context

object DatabaseProvider {

    private lateinit var db: ToolskuDatabase

    fun init(context: Context) {
        db = ToolskuDatabase.getInstance(context)
    }

    fun get(): ToolskuDatabase = db

    fun runningAppDao(): RunningAppDao = db.runningAppDao()
}
