package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLaunchHistoryDao {
    @Query("SELECT * FROM app_launch_history ORDER BY launchTime DESC")
    fun getAllLaunchHistoryFlow(): Flow<List<AppLaunchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaunchEvent(event: AppLaunchHistoryEntity)

    @Query("DELETE FROM app_launch_history")
    suspend fun clearHistory()
}
