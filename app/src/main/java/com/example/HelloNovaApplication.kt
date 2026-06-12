package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.UserRepository

class HelloNovaApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { UserRepository(
        database.userPreferencesDao(),
        database.voiceConversationDao(),
        database.serviceLogDao(),
        database.smartActionDao(),
        database.notificationDao(),
        database.communicationEventDao(),
        database.voiceSessionDao(),
        database.wakeEventDao(),
        database.installedAppDao(),
        database.appLaunchHistoryDao(),
        database.assistantMemoryDao(),
        database.contactDao(),
        database.contactSearchHistoryDao(),
        database.categorizedMemoryDao(),
        database.deviceControlCommandDao(),
        database.screenshotHistoryDao()
    ) }
}
