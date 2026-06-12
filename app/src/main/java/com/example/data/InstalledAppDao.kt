package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    fun getAllInstalledAppsFlow(): Flow<List<InstalledAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstalledApps(apps: List<InstalledAppEntity>)

    @Query("DELETE FROM installed_apps")
    suspend fun clearAllInstalledApps()

    @Query("SELECT * FROM installed_apps WHERE appName LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%' ORDER BY appName ASC")
    suspend fun searchApps(query: String): List<InstalledAppEntity>
}
