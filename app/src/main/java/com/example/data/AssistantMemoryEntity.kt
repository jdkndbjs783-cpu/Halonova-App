package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_memory")
data class AssistantMemoryEntity(
    @PrimaryKey val id: Int = 1,
    val lastOpenedAppName: String = "",
    val lastOpenedPackageName: String = "",
    val lastExecutedCommand: String = "",
    val lastActionTime: Long = 0L,
    val recentActions: String = "", // Newline or comma separated list of recent activities
    val lastSearchedContact: String = "",
    val recentContactSearches: String = "", // Newline separated list of recent contact searches
    val userFacts: String = "", // Long-Term Memory Foundation (Requirement 1)
    val conversationSummaries: String = "", // Summarized insights of recent chats
    val lastMemorySyncTime: Long = 0L // Last time memory was compiled/processed
)
