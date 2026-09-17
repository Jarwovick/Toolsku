package com.toolsku.app.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RunningAppEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ToolskuDatabase : RoomDatabase() {

    abstract fun runningAppDao(): RunningAppDao

    companion object {
        @Volatile
        private var INSTANCE: ToolskuDatabase? = null

        fun getInstance(context: Context): ToolskuDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ToolskuDatabase::class.java,
                    "toolsku_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
