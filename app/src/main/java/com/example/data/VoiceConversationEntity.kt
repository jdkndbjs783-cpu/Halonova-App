package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_conversation_history")
data class VoiceConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userSpeech: String,
    val novaResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "English",
    val status: String = "SUCCESS"
)
