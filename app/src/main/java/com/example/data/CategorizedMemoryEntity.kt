package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorized_memories")
data class CategorizedMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String, // "Personal Preferences", "Daily Habits", "Frequently Used Commands", "Favorite Contacts", "Assistant Learning Notes"
    val content: String,
    val patternName: String = "",
    val frequencyCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val confidenceScore: Int = 85 // Represents AI confidence level from 0 to 100
)
