package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(
    private val dao: UserPreferencesDao,
    private val voiceDao: VoiceConversationDao,
    private val serviceLogDao: ServiceLogDao,
    private val smartActionDao: SmartActionDao,
    private val notificationDao: NotificationDao,
    private val communicationEventDao: CommunicationEventDao,
    private val voiceSessionDao: VoiceSessionDao,
    private val wakeEventDao: WakeEventDao,
    private val installedAppDao: InstalledAppDao,
    private val appLaunchHistoryDao: AppLaunchHistoryDao,
    private val assistantMemoryDao: AssistantMemoryDao,
    private val contactDao: ContactDao,
    private val contactSearchHistoryDao: ContactSearchHistoryDao,
    private val categorizedMemoryDao: CategorizedMemoryDao,
    val deviceControlCommandDao: DeviceControlCommandDao,
    val screenshotHistoryDao: ScreenshotHistoryDao
) {

    // --- Phase 23 Device Control and Screen Mechanics ---
    val allDeviceControlCommandsFlow: Flow<List<DeviceControlCommandEntity>> = deviceControlCommandDao.getAllCommandsFlow()

    suspend fun insertDeviceControlCommand(command: DeviceControlCommandEntity) {
        deviceControlCommandDao.insertCommand(command)
    }

    suspend fun updateDeviceControlCommand(command: DeviceControlCommandEntity) {
        deviceControlCommandDao.updateCommand(command)
    }

    suspend fun clearDeviceControlCommands() {
        deviceControlCommandDao.clearHistory()
    }

    val allScreenshotHistoryFlow: Flow<List<ScreenshotHistoryEntity>> = screenshotHistoryDao.getAllScreenshotsFlow()

    suspend fun insertScreenshotHistory(screenshot: ScreenshotHistoryEntity) {
        screenshotHistoryDao.insertScreenshot(screenshot)
    }

    suspend fun clearScreenshotHistory() {
        screenshotHistoryDao.clearHistory()
    }

    // --- Phase 10: Contacts and Communications ---
    val allContactsFlow: Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()

    suspend fun insertContacts(contacts: List<ContactEntity>) {
        contactDao.insertContacts(contacts)
    }

    suspend fun clearAllContacts() {
        contactDao.clearAllContacts()
    }

    suspend fun searchContacts(query: String): List<ContactEntity> {
        return contactDao.searchContacts(query)
    }

    val allContactSearchHistoryFlow: Flow<List<ContactSearchHistoryEntity>> = contactSearchHistoryDao.getAllSearchHistoryFlow()

    suspend fun insertContactSearchEvent(event: ContactSearchHistoryEntity) {
        contactSearchHistoryDao.insertSearchEvent(event)
    }

    suspend fun clearContactSearchHistory() {
        contactSearchHistoryDao.clearHistory()
    }

    // --- Phase 9 Launcher and Memory operations ---
    val allInstalledAppsFlow: Flow<List<InstalledAppEntity>> = installedAppDao.getAllInstalledAppsFlow()
    
    suspend fun insertInstalledApps(apps: List<InstalledAppEntity>) {
        installedAppDao.insertInstalledApps(apps)
    }

    suspend fun clearAllInstalledApps() {
        installedAppDao.clearAllInstalledApps()
    }

    suspend fun searchApps(query: String): List<InstalledAppEntity> {
        return installedAppDao.searchApps(query)
    }

    val allLaunchHistoryFlow: Flow<List<AppLaunchHistoryEntity>> = appLaunchHistoryDao.getAllLaunchHistoryFlow()

    suspend fun insertLaunchEvent(event: AppLaunchHistoryEntity) {
        appLaunchHistoryDao.insertLaunchEvent(event)
    }

    suspend fun clearLaunchHistory() {
        appLaunchHistoryDao.clearHistory()
    }

    val assistantMemoryFlow: Flow<AssistantMemoryEntity?> = assistantMemoryDao.getMemoryFlow()

    suspend fun getAssistantMemory(): AssistantMemoryEntity {
        return assistantMemoryDao.getMemory() ?: AssistantMemoryEntity()
    }

    suspend fun saveAssistantMemory(memory: AssistantMemoryEntity) {
        assistantMemoryDao.saveMemory(memory)
    }

    // --- Categorized memories ---
    val allCategorizedMemoriesFlow: Flow<List<CategorizedMemoryEntity>> = categorizedMemoryDao.getAllMemoriesFlow()

    suspend fun getAllCategorizedMemories(): List<CategorizedMemoryEntity> = categorizedMemoryDao.getAllMemories()

    suspend fun getCategorizedMemoriesByCategory(category: String): List<CategorizedMemoryEntity> =
        categorizedMemoryDao.getMemoriesByCategory(category)

    suspend fun insertCategorizedMemory(memory: CategorizedMemoryEntity) {
        categorizedMemoryDao.insertMemory(memory)
    }

    suspend fun updateCategorizedMemory(memory: CategorizedMemoryEntity) {
        categorizedMemoryDao.updateMemory(memory)
    }

    suspend fun deleteCategorizedMemory(id: Long) {
        categorizedMemoryDao.deleteMemory(id)
    }

    suspend fun clearAllCategorizedMemories() {
        categorizedMemoryDao.clearAllMemories()
    }

    val userPreferencesFlow: Flow<UserPreferencesEntity> = dao.getUserPreferencesFlow().map { prefs ->
        prefs?.let { validateAndCoercePreferences(it) } ?: UserPreferencesEntity()
    }

    // Wake Events flow
    val allWakeEventsFlow: Flow<List<WakeEventEntity>> = wakeEventDao.getAllWakeEventsFlow()

    suspend fun insertWakeEvent(event: WakeEventEntity) {
        wakeEventDao.insertWakeEvent(event)
    }

    suspend fun clearWakeEvents() {
        wakeEventDao.clearAllWakeEvents()
    }

    // Voice sessions flow
    val allSessionsFlow: Flow<List<VoiceSessionEntity>> = voiceSessionDao.getAllSessionsFlow()

    suspend fun insertVoiceSession(session: VoiceSessionEntity) {
        voiceSessionDao.insertSession(session)
    }

    suspend fun clearVoiceSessions() {
        voiceSessionDao.clearAllSessions()
    }

    // Notification flows/methods
    val allNotificationsFlow: Flow<List<NotificationEntity>> = notificationDao.getAllNotificationsFlow()

    suspend fun insertNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    suspend fun clearNotifications() {
        notificationDao.clearNotifications()
    }

    // Communication event flows/methods
    val allCommunicationEventsFlow: Flow<List<CommunicationEventEntity>> = communicationEventDao.getAllEventsFlow()

    suspend fun insertCommunicationEvent(event: CommunicationEventEntity) {
        communicationEventDao.insertEvent(event)
    }

    suspend fun clearCommunicationEvents() {
        communicationEventDao.clearEvents()
    }

    // Smart Actions History list flow
    val allSmartActionsFlow: Flow<List<SmartActionEntity>> = smartActionDao.getAllActionsFlow()

    suspend fun insertSmartAction(action: SmartActionEntity) {
        smartActionDao.insertAction(action)
    }

    suspend fun clearSmartActionHistory() {
        smartActionDao.clearActionHistory()
    }

    // Service logs
    val allServiceLogsFlow: Flow<List<ServiceLogEntity>> = serviceLogDao.getAllLogs()

    suspend fun insertServiceLog(log: ServiceLogEntity) {
        serviceLogDao.insertLog(log)
    }

    suspend fun clearServiceLogs() {
        serviceLogDao.clearAllLogs()
    }

    // Voice conversation history
    val allConversationsFlow: Flow<List<VoiceConversationEntity>> = voiceDao.getAllConversationsFlow()

    suspend fun insertVoiceConversation(conversation: VoiceConversationEntity) {
        voiceDao.insertConversation(conversation)
    }

    suspend fun clearVoiceHistory() {
        voiceDao.clearAllHistory()
    }

    suspend fun pruneOldDatabaseEntries(cutoffDays: Int = 3): Int {
        val cutoffTime = System.currentTimeMillis() - (cutoffDays.toLong() * 24 * 60 * 60 * 1000)
        return try {
            serviceLogDao.pruneLogs(cutoffTime)
            notificationDao.pruneNotifications(cutoffTime)
            communicationEventDao.pruneEvents(cutoffTime)
            voiceDao.pruneConversations(cutoffTime)
            smartActionDao.pruneActions(cutoffTime)
            1
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Failed to prune database: ${e.message}")
            0
        }
    }

    private fun validateAndCoercePreferences(prefs: UserPreferencesEntity): UserPreferencesEntity {
        var needsCoercion = false
        var name = prefs.assistantName
        var lang = prefs.voiceLanguage
        var speed = prefs.speechSpeed
        var pitch = prefs.speechPitch
        var volume = prefs.speechVolume
        var personality = prefs.personalityMode
        var wake = prefs.wakePhrase

        if (name.isBlank() || name == "Nova") {
            name = "Hello Nova"
            needsCoercion = true
        }
        if (lang != "en" && lang != "bn") {
            lang = "en"
            needsCoercion = true
        }
        if (speed < 0.5f || speed > 2.0f || speed.isNaN()) {
            speed = speed.coerceIn(0.5f, 2.0f)
            if (speed.isNaN()) speed = 1.0f
            needsCoercion = true
        }
        if (pitch < 0.5f || pitch > 2.0f || pitch.isNaN()) {
            pitch = pitch.coerceIn(0.5f, 2.0f)
            if (pitch.isNaN()) pitch = 1.0f
            needsCoercion = true
        }
        if (volume < 0.0f || volume > 1.0f || volume.isNaN()) {
            volume = volume.coerceIn(0.0f, 1.0f)
            if (volume.isNaN()) volume = 1.0f
            needsCoercion = true
        }
        if (personality != "Professional" && personality != "Friendly" && personality != "Casual") {
            personality = "Friendly"
            needsCoercion = true
        }
        if (wake.isBlank()) {
            wake = "Hello Nova"
            needsCoercion = true
        }

        return if (needsCoercion) {
            prefs.copy(
                assistantName = name,
                voiceLanguage = lang,
                speechSpeed = speed,
                speechPitch = pitch,
                speechVolume = volume,
                personalityMode = personality,
                wakePhrase = wake
            )
        } else {
            prefs
        }
    }

    suspend fun getPreferences(): UserPreferencesEntity {
        val raw = dao.getUserPreferences() ?: UserPreferencesEntity()
        val coerced = validateAndCoercePreferences(raw)
        if (coerced != raw) {
            savePreferences(coerced)
        }
        return coerced
    }

    suspend fun savePreferences(preferences: UserPreferencesEntity) {
        dao.saveUserPreferences(validateAndCoercePreferences(preferences))
    }

    suspend fun updateUsername(username: String) {
        val current = getPreferences()
        dao.saveUserPreferences(current.copy(username = username))
    }

    suspend fun updateGeminiApiKey(apiKey: String) {
        val current = getPreferences()
        dao.saveUserPreferences(current.copy(geminiApiKey = apiKey))
    }

    suspend fun updateThemeSelection(darkThemeEnabled: Boolean) {
        val current = getPreferences()
        dao.saveUserPreferences(current.copy(darkThemeEnabled = darkThemeEnabled))
    }

    suspend fun updateAvatar(avatarId: String) {
        val current = getPreferences()
        dao.saveUserPreferences(current.copy(avatarId = avatarId))
    }

    suspend fun updateLoginState(isLoggedIn: Boolean) {
        val current = getPreferences()
        dao.saveUserPreferences(current.copy(isLoggedIn = isLoggedIn))
    }

    suspend fun updateEngineState(active: Boolean) {
        val current = getPreferences()
        dao.saveUserPreferences(current.copy(engineActive = active))
    }
}
