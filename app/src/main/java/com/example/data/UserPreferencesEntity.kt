package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "Nova Scout",
    val geminiApiKey: String = "",
    val darkThemeEnabled: Boolean = true,
    val avatarId: String = "cyborg",
    val isLoggedIn: Boolean = false,
    val engineActive: Boolean = false,
    val assistantName: String = "Hello Nova",
    val voiceLanguage: String = "en",
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val speechVolume: Float = 1.0f,
    val themeMode: String = "cyber", // cyber, dark, light
    val startupEngineEnabled: Boolean = false,
    val notificationReadbackEnabled: Boolean = false,
    val voiceFeedbackEnabled: Boolean = true,
    val sessionAutoRestoreEnabled: Boolean = false,
    val personalityMode: String = "Friendly", // Professional, Friendly, Casual
    val wakePhrase: String = "Hello Nova",
    val activeAssistant: String = "NOVA"
)
