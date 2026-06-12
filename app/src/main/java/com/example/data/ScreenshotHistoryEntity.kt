package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshot_history")
data class ScreenshotHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val screenshotName: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)
