package com.toolsku.app.core.db

import android.content.Context

/**
 * Provider global untuk akses database dari mana saja.
 */
object DatabaseProvider {

    private lateinit var db: ToolskuDatabase

    fun init(context: Context) {
        db = ToolskuDatabase.getInstance(context)
    }

    fun get(): ToolskuDatabase {
        return db
    }

    fun runningAppDao(): RunningAppDao {
        return db.runningAppDao()
    }
}
