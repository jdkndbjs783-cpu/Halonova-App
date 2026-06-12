package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserPreferencesEntity::class, VoiceConversationEntity::class, ServiceLogEntity::class, SmartActionEntity::class, NotificationEntity::class, CommunicationEventEntity::class, VoiceSessionEntity::class, WakeEventEntity::class, InstalledAppEntity::class, AppLaunchHistoryEntity::class, AssistantMemoryEntity::class, ContactEntity::class, ContactSearchHistoryEntity::class, CategorizedMemoryEntity::class, DeviceControlCommandEntity::class, ScreenshotHistoryEntity::class], version = 16, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun voiceConversationDao(): VoiceConversationDao
    abstract fun serviceLogDao(): ServiceLogDao
    abstract fun smartActionDao(): SmartActionDao
    abstract fun notificationDao(): NotificationDao
    abstract fun communicationEventDao(): CommunicationEventDao
    abstract fun voiceSessionDao(): VoiceSessionDao
    abstract fun wakeEventDao(): WakeEventDao
    abstract fun installedAppDao(): InstalledAppDao
    abstract fun appLaunchHistoryDao(): AppLaunchHistoryDao
    abstract fun assistantMemoryDao(): AssistantMemoryDao
    abstract fun contactDao(): ContactDao
    abstract fun contactSearchHistoryDao(): ContactSearchHistoryDao
    abstract fun categorizedMemoryDao(): CategorizedMemoryDao
    abstract fun deviceControlCommandDao(): DeviceControlCommandDao
    abstract fun screenshotHistoryDao(): ScreenshotHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hello_nova_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
