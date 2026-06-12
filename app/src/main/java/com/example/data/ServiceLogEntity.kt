package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_logs")
data class ServiceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventType: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
