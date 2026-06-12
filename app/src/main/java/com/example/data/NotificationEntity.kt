package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
