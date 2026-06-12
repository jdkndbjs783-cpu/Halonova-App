package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_sessions")
data class VoiceSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val commandCount: Int,
    val durationMs: Long,
    val sessionType: String = "MANUAL"
)
