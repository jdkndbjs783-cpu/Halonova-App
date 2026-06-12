package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "communication_events")
data class CommunicationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CALL", "NOTIFICATION", "VOICE"
    val senderOrApp: String, // Caller name or App name or Voice Command
    val title: String, // Action title or Notification title
    val details: String, // Details or message content
    val timestamp: Long = System.currentTimeMillis()
)
