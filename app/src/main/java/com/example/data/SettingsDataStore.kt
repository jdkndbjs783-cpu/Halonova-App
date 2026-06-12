package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hello_nova_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val ASSISTANT_NAME = stringPreferencesKey("assistant_name")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val SPEECH_VOLUME = floatPreferencesKey("speech_volume")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val STARTUP_ENGINE = booleanPreferencesKey("startup_engine")
        val NOTIFICATION_READBACK = booleanPreferencesKey("notification_readback")
        val VOICE_FEEDBACK = booleanPreferencesKey("voice_feedback")
        val SESSION_AUTO_RESTORE = booleanPreferencesKey("session_auto_restore")
        val PERSONALITY_MODE = stringPreferencesKey("personality_mode")
        val WAKE_PHRASE = stringPreferencesKey("wake_phrase")
        val ACTIVE_ASSISTANT = stringPreferencesKey("active_assistant")
    }

    suspend fun saveSettings(entity: UserPreferencesEntity) {
        context.dataStore.edit { preferences ->
            preferences[ASSISTANT_NAME] = entity.assistantName
            preferences[VOICE_LANGUAGE] = entity.voiceLanguage
            preferences[SPEECH_SPEED] = entity.speechSpeed
            preferences[SPEECH_PITCH] = entity.speechPitch
            preferences[SPEECH_VOLUME] = entity.speechVolume
            preferences[THEME_MODE] = entity.themeMode
            preferences[STARTUP_ENGINE] = entity.startupEngineEnabled
            preferences[NOTIFICATION_READBACK] = entity.notificationReadbackEnabled
            preferences[VOICE_FEEDBACK] = entity.voiceFeedbackEnabled
            preferences[SESSION_AUTO_RESTORE] = entity.sessionAutoRestoreEnabled
            preferences[PERSONALITY_MODE] = entity.personalityMode
            preferences[WAKE_PHRASE] = entity.wakePhrase
            preferences[ACTIVE_ASSISTANT] = entity.activeAssistant
        }
    }

    fun getSettingsFlow(): Flow<UserPreferencesEntity> {
        return context.dataStore.data.map { preferences ->
            UserPreferencesEntity(
                id = 1,
                assistantName = preferences[ASSISTANT_NAME] ?: "Hello Nova",
                voiceLanguage = preferences[VOICE_LANGUAGE] ?: "en",
                speechSpeed = preferences[SPEECH_SPEED] ?: 1.0f,
                speechPitch = preferences[SPEECH_PITCH] ?: 1.0f,
                speechVolume = preferences[SPEECH_VOLUME] ?: 1.0f,
                themeMode = preferences[THEME_MODE] ?: "cyber",
                startupEngineEnabled = preferences[STARTUP_ENGINE] ?: false,
                notificationReadbackEnabled = preferences[NOTIFICATION_READBACK] ?: false,
                voiceFeedbackEnabled = preferences[VOICE_FEEDBACK] ?: true,
                sessionAutoRestoreEnabled = preferences[SESSION_AUTO_RESTORE] ?: false,
                personalityMode = preferences[PERSONALITY_MODE] ?: "Friendly",
                wakePhrase = preferences[WAKE_PHRASE] ?: "Hello Nova",
                activeAssistant = preferences[ACTIVE_ASSISTANT] ?: "NOVA"
            )
        }
    }
}
