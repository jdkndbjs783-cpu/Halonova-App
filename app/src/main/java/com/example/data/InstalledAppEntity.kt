package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val installTime: Long,
    val iconResId: Int = 0
)
