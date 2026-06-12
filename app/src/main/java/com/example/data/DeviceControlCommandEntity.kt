package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_control_commands")
data class DeviceControlCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val commandText: String,
    val executionDetails: String,
    val status: String, // "QUEUED", "EXECUTING", "COMPLETED", "FAILED"
    val timestamp: Long = System.currentTimeMillis()
)
