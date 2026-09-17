package com.toolsku.app.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RunningAppDao {

    /**
     * Insert atau update app.
     * Kalau sudah ada → update lastUsed.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RunningAppEntity)

    /**
     * Ambil semua running apps (yang belum ditutup + belum auto-restart).
     */
    @Query("""
        SELECT * FROM running_apps 
        WHERE isClosed = 0 
          AND isUnclosable = 0 
        ORDER BY lastUsed DESC
    """)
    suspend fun getRunningApps(): List<RunningAppEntity>

    /**
     * Ambil app dengan package name.
     */
    @Query("SELECT * FROM running_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getApp(pkg: String): RunningAppEntity?

    /**
     * Mark app sebagai closed.
     */
    @Query("UPDATE running_apps SET isClosed = 1 WHERE packageName = :pkg")
    suspend fun markClosed(pkg: String)

    /**
     * Mark app sebagai auto-restarted.
     */
    @Query("UPDATE running_apps SET isAutoRestarted = 1 WHERE packageName = :pkg")
    suspend fun markAutoRestarted(pkg: String)

    /**
     * Mark app sebagai unclosable.
     */
    @Query("UPDATE running_apps SET isUnclosable = 1 WHERE packageName = :pkg")
    suspend fun markUnclosable(pkg: String)

    /**
     * Reset flag setelah kill — biar muncul lagi kalau app dibuka kembali.
     */
    @Query("""
        UPDATE running_apps 
        SET isClosed = 0, 
            isAutoRestarted = 0 
        WHERE packageName IN (:packages)
    """)
    suspend fun resetFlags(packages: List<String>)

    /**
     * Hapus app dari database.
     */
    @Query("DELETE FROM running_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    /**
     * Hapus semua yang sudah lama tidak dipakai (> 24 jam).
     */
    @Query("DELETE FROM running_apps WHERE lastUsed < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    /**
     * Ambil semua (untuk cleanup).
     */
    @Query("SELECT * FROM running_apps")
    suspend fun getAll(): List<RunningAppEntity>
}
