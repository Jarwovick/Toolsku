package com.toolsku.app.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RunningAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RunningAppEntity)

    @Query("""
        SELECT * FROM running_apps 
        WHERE isClosed = 0 
          AND isUnclosable = 0 
          AND isAutoRestarted = 0
          AND isSystem = 0
        ORDER BY lastUsed DESC
    """)
    suspend fun getRunningUserApps(): List<RunningAppEntity>

    @Query("""
        SELECT * FROM running_apps 
        WHERE isClosed = 0 
          AND isUnclosable = 0 
          AND isAutoRestarted = 0
          AND isSystem = 1
        ORDER BY lastUsed DESC
    """)
    suspend fun getRunningSystemApps(): List<RunningAppEntity>

    @Query("SELECT * FROM running_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getApp(pkg: String): RunningAppEntity?

    @Query("UPDATE running_apps SET isClosed = 1 WHERE packageName = :pkg")
    suspend fun markClosed(pkg: String)

    @Query("UPDATE running_apps SET isAutoRestarted = 1 WHERE packageName = :pkg")
    suspend fun markAutoRestarted(pkg: String)

    @Query("UPDATE running_apps SET isUnclosable = 1 WHERE packageName = :pkg")
    suspend fun markUnclosable(pkg: String)

    @Query("DELETE FROM running_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}
