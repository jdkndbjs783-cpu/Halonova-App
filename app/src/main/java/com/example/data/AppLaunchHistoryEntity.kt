package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_launch_history")
data class AppLaunchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appName: String,
    val packageName: String,
    val launchTime: Long,
    val status: String, // "SUCCESS" or "FAILED"
    val failureReason: String? = null
)
