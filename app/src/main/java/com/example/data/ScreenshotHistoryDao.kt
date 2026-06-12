package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotHistoryDao {
    @Query("SELECT * FROM screenshot_history ORDER BY timestamp DESC")
    fun getAllScreenshotsFlow(): Flow<List<ScreenshotHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotHistoryEntity)

    @Query("DELETE FROM screenshot_history")
    suspend fun clearHistory()
}
