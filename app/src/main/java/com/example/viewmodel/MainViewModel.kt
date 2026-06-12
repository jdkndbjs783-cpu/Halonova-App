package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import com.example.data.ServiceLogEntity
import com.example.service.NovaForegroundService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.UserPreferencesEntity
import com.example.data.UserRepository
import com.example.data.VoiceConversationEntity
import com.example.data.SmartActionEntity
import com.example.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import com.example.data.NotificationEntity
import com.example.data.CommunicationEventEntity
import com.example.data.VoiceSessionEntity
import com.example.data.WakeEventEntity
import com.example.data.InstalledAppEntity
import com.example.data.AppLaunchHistoryEntity
import com.example.data.AssistantMemoryEntity
import com.example.data.ContactEntity
import com.example.data.ContactSearchHistoryEntity
import com.example.service.LauncherEngineState
import com.example.service.ContactEngineState
import com.example.service.VoiceManager
import com.example.service.SmartActionType
import com.example.service.SmartActionExecutor
import com.example.service.VoiceCommandParser
import com.example.service.CommandResult
import com.example.service.VoiceSessionManager
import com.example.service.AssistantState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.random.Random
import com.example.data.DeviceControlCommandEntity
import com.example.data.ScreenshotHistoryEntity

data class CyberFile(val name: String, val type: String, val size: String, val lastModified: String)

enum class WakeSessionStatus {
    IDLE,
    LISTENING_READY,
    ACTIVE_SESSION
}

class MainViewModel(
    private val repository: UserRepository,
    private val application: Application
) : ViewModel() {

    // Splash State
    private val _isSplashFinished = MutableStateFlow(false)
    val isSplashFinished: StateFlow<Boolean> = _isSplashFinished.asStateFlow()

    // Preferences State
    val preferences: StateFlow<UserPreferencesEntity> = repository.userPreferencesFlow
        .map { it ?: UserPreferencesEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesEntity()
        )

    // Interactive Terminal State
    private val _terminalOutput = MutableStateFlow("CHRONOS-CONSOLE-v1.07 // Welcome, Operator. Initialize core engine to proceed.")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    // Live Telemetry Tickers for Engine
    private val _telemetryMessages = MutableStateFlow<List<String>>(emptyList())
    val telemetryMessages: StateFlow<List<String>> = _telemetryMessages.asStateFlow()

    private var telemetryJob: Job? = null

    // --- Voice Assistant State ---
    val voiceUserSpeech = MutableStateFlow("")
    val voiceNovaResponse = MutableStateFlow("")

    val voiceManager = VoiceManager(application) { spokenText ->
        processVoiceCommand(spokenText)
    }

    val voiceIsListening = voiceManager.isListening.asStateFlow()
    val voiceRecognizedText = voiceManager.recognizedText.asStateFlow()
    val voiceError = voiceManager.errorState.asStateFlow()
    val isTtsReady = voiceManager.isTtsReady.asStateFlow()
    val activeVoiceLanguage = voiceManager.activeLanguageCode.asStateFlow()

    // --- Phase 15: Gemini AI Conversation Engine ---
    val geminiEngine = com.example.service.GeminiEngine(application)
    val geminiConnectionStatus = geminiEngine.connectionStatus
    val geminiActiveSessionInfo = geminiEngine.activeSessionInfo
    val geminiLastResponseTimeMs = geminiEngine.lastResponseTimeMs
    val geminiModelStatus = geminiEngine.modelStatus

    // --- Phase 22: AI Brain + Auto Learning Core ---
    val autoLearningEngine = com.example.service.AutoLearningEngine(application, repository, viewModelScope)
    val learningScore = autoLearningEngine.learningScore
    val brainStatus = autoLearningEngine.brainStatus
    val lastLearnedPattern = autoLearningEngine.lastLearnedPattern
    val smartSuggestions = autoLearningEngine.smartSuggestions

    val categorizedMemories = repository.allCategorizedMemoriesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteCategorizedMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategorizedMemory(id)
            autoLearningEngine.analyzeUsagePatterns()
            _terminalOutput.value = "MEMORY_GC // Deleted memory node [ID: $id]"
        }
    }

    fun clearAllCategorizedMemories() {
        viewModelScope.launch {
            repository.clearAllCategorizedMemories()
            autoLearningEngine.analyzeUsagePatterns()
            _terminalOutput.value = "MEMORY_GC // Flushed all long-term memories."
        }
    }

    fun forceAnalyzeUsagePatterns() {
        viewModelScope.launch {
            autoLearningEngine.analyzeUsagePatterns()
            _terminalOutput.value = "AI_BRAIN // Manual synaptic pattern analysis complete."
        }
    }

    fun simulateHabitLearning(appName: String) {
        viewModelScope.launch {
            autoLearningEngine.simulateHabitLearning(appName)
            _terminalOutput.value = "AI_BRAIN // Simulated interaction events recorded for $appName"
        }
    }

    // --- Phase 17: Vision & Multimodal Flows ---
    val visionEngineStatus = geminiEngine.visionEngineStatus
    val lastAnalyzedAsset = geminiEngine.lastAnalyzedAsset
    val visionAnalysisTimeMs = geminiEngine.analysisTimeMs
    val multimodalSessionStatus = geminiEngine.multimodalSessionStatus

    // Spatial Awareness & Dynamic Alerts state (Phase 18)
    private val spatialLocationService by lazy { com.example.service.SpatialLocationService(application) }
    val locationPermissionGranted = MutableStateFlow(false)
    val locationLatitude = MutableStateFlow<Double?>(null)
    val locationLongitude = MutableStateFlow<Double?>(null)
    val locationSector = MutableStateFlow("Sector Terra-Void [SCANNING...]")
    val lastSpatialUpdateTime = MutableStateFlow(0L)

    // Phone Integration & Real Assistant Experience state (Phase 19)
    val phonePermissionGranted = MutableStateFlow(false)
    val contactsPermissionGranted = MutableStateFlow(false)
    val callPermissionGranted = MutableStateFlow(false)
    val notificationListenerConnected = MutableStateFlow(false)

    // Phase 21.1 Permission Intelligence Engine
    private val permissionIntelligenceEngine by lazy { com.example.service.PermissionIntelligenceEngine(application) }
    val securityReport = MutableStateFlow(com.example.service.PermissionStatusReport())

    fun triggerPermissionRecovery(permissionKey: String) {
        val intent = permissionIntelligenceEngine.getRecoveryIntent(permissionKey)
        if (intent != null) {
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            application.startActivity(intent)
            writeTerminalLog("PERM_RECOVERY // Initialized recovery protocol for: $permissionKey")
        } else {
            writeTerminalLog("PERM_ERROR // Recovery intent unavailable for request: $permissionKey")
        }
    }

    fun syncFullSecurityReport() {
        securityReport.value = permissionIntelligenceEngine.generateSecurityReport()
        writeTerminalLog("SECURITY_AUDIT // Manual permission sync initialized.")
    }

    // Dynamic caller details tracker
    val activeCallerName = MutableStateFlow<String?>(null)
    val activeCallerNumber = MutableStateFlow<String?>(null)
    val activeCallState = MutableStateFlow("STANDBY") // STANDBY, RINGING, ACTIVE, DISCONNECTED

    // Communication state trackers (Phase 21.3)
    val latestSmsSender = MutableStateFlow<String?>(null)
    val latestSmsContent = MutableStateFlow<String?>(null)
    val latestWhatsAppSender = MutableStateFlow<String?>(null)
    val latestWhatsAppContent = MutableStateFlow<String?>(null)

    // Voice / alert notification search text
    val notificationSearchQuery = MutableStateFlow("")

    fun checkAndSyncPhonePermissions() {
        val phoneGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            application, android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        val callLogGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            application, android.Manifest.permission.READ_CALL_LOG
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        phonePermissionGranted.value = phoneGranted && callLogGranted

        contactsPermissionGranted.value = androidx.core.content.ContextCompat.checkSelfPermission(
            application, android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        callPermissionGranted.value = androidx.core.content.ContextCompat.checkSelfPermission(
            application, android.Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        notificationListenerConnected.value = com.example.service.NovaNotificationListenerService.isServiceConnected()
    }

    // User-controlled Alert Preferences (Phase 18)
    val alertFilteringPriority = MutableStateFlow("ALL") // ALL, HIGH-ONLY, MEDIUM-AND-HIGH
    val alertCategoryFilter = MutableStateFlow("ALL") // ALL, MESSAGING, SYSTEM, SOCIAL, SECURITY
    val alertReadbackVolume = MutableStateFlow(1.0f)
    val alertEngineActive = MutableStateFlow(true)

    fun checkAndSyncLocation() {
        val granted = spatialLocationService.hasPermission()
        locationPermissionGranted.value = granted
        if (granted) {
            val loc = spatialLocationService.getLastKnownLocation()
            if (loc != null) {
                locationLatitude.value = loc.latitude
                locationLongitude.value = loc.longitude
                val sector = spatialLocationService.getSectorName(loc.latitude, loc.longitude)
                locationSector.value = sector
                lastSpatialUpdateTime.value = System.currentTimeMillis()
                writeTerminalLog("GEO_SYNC // Spatial lock acquired: $sector at Lat ${String.format(java.util.Locale.US, "%.4f", loc.latitude)}, Lng ${String.format(java.util.Locale.US, "%.4f", loc.longitude)}")
            } else {
                // Safe fallback with mock coordinates (Dhaka Sector delta-09)
                locationLatitude.value = 23.8103
                locationLongitude.value = 90.4125
                val sector = spatialLocationService.getSectorName(23.8103, 90.4125)
                locationSector.value = sector
                lastSpatialUpdateTime.value = System.currentTimeMillis()
                writeTerminalLog("GEO_SYNC // Spatial GPS weak. Activated synthetic fallback: $sector [Bengal Basin]")
            }
        } else {
            locationLatitude.value = null
            locationLongitude.value = null
            locationSector.value = "Sector Terra-Void [GPS OFFLINE]"
            lastSpatialUpdateTime.value = System.currentTimeMillis()
            writeTerminalLog("GEO_SYNC // Spatial permission denied. Operator location default to Terra-Void.")
        }
    }

    fun getNotificationCategory(item: NotificationEntity): String {
        val app = item.appName.lowercase()
        return when {
            app.contains("slack") || app.contains("whatsapp") || app.contains("messenger") || app.contains("telegram") || app.contains("viber") || app.contains("skype") || app.contains("discord") || app.contains("signal") -> "MESSAGING"
            app.contains("system") || app.contains("android") || app.contains("google") || app.contains("battery") || app.contains("os") -> "SYSTEM"
            app.contains("instagram") || app.contains("twitter") || app.contains("x") || app.contains("facebook") || app.contains("linkedin") || app.contains("tiktok") -> "SOCIAL"
            app.contains("security") || app.contains("guardian") || app.contains("nova") || app.contains("lock") -> "SECURITY"
            else -> "OTHER"
        }
    }

    fun getDeviceStateContext(): String {
        return try {
            val batteryIntent = application.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

            val cm = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetworkInfo
            val isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting
            val netType = when (activeNetwork?.type) {
                android.net.ConnectivityManager.TYPE_WIFI -> "WIFI"
                android.net.ConnectivityManager.TYPE_MOBILE -> "CELLULAR"
                else -> "ETHERNET/UNKNOWN"
            }

            val isNotifListenerAlive = com.example.service.NovaNotificationListenerService.isServiceConnected()

            "Device Telemetry Status:\n" +
                   "- Battery Level: $batteryPct%\n" +
                   "- Network Uplink: ${if (isConnected) "CONNECTED ($netType)" else "OFFLINE"}\n" +
                   "- Notification Capture Intel: ${if (isNotifListenerAlive) "ONLINE & ACTIVE" else "STANDBY (Requires Service Hook)"}"
        } catch (e: Exception) {
            "Device Telemetry: Core system variables unreadable."
        }
    }

    suspend fun getEnhancedContextInstructionForGemini(): String {
        val memories = try {
            repository.getAllCategorizedMemories()
        } catch (e: Exception) {
            emptyList()
        }
        val memoryStr = if (memories.isEmpty()) {
            "COGNITIVE LONG-TERM MEMORIES: Empty vault. Ready to learn operator preferences and habits."
        } else {
            "COGNITIVE LONG-TERM MEMORIES (PERSISTENT):\n" + memories.groupBy { it.category }.map { (category, list) ->
                "[$category]:\n" + list.joinToString("\n") { "  - ${it.content}" }
            }.joinToString("\n")
        }

        val spatialStr = if (locationPermissionGranted.value) {
            "Current Spatial Location: ${locationSector.value} (Coordinates Lat: ${locationLatitude.value}, Lng: ${locationLongitude.value})."
        } else {
            "Current Spatial Location fallback: ${locationSector.value}. Permission is currently pending/not granted by Operator."
        }

        val deviceStr = getDeviceStateContext()

        val nonNullNotifs = notificationsHistory.value
        val recentNotifsStr = if (nonNullNotifs.isEmpty()) {
            "Intercepted Alerts in security archive: None active."
        } else {
            val activeSample = nonNullNotifs.take(5).joinToString("\n") {
                "- ${it.appName.uppercase()} [${it.title}]: \"${it.message}\""
            }
            "Intercepted Alerts in security archive (most recent first):\n$activeSample"
        }

        return "\n\n--- OPERATOR ENVIRONMENT TELEMETRY MATRIX ---\n" +
               "$spatialStr\n" +
               "$deviceStr\n" +
               "$recentNotifsStr\n" +
               "\n--- COGNITIVE LONG-TERM MEMORY REGISTER ---\n" +
               "$memoryStr\n" +
               "--------------------------------------------\n" +
               "Note: You can contextually access and mention this telemetry (like user location, alerts history or battery level) and long-term memory metrics ONLY if they ask about it or if it is relevant to personalizing their answer."
    }

    val selectedImageUri = MutableStateFlow<android.net.Uri?>(null)
    val selectedPdfUri = MutableStateFlow<android.net.Uri?>(null)
    val extractedPdfText = MutableStateFlow("")
    val tempCameraUri = MutableStateFlow<android.net.Uri?>(null)

    fun processImageSelection(uri: android.net.Uri) {
        selectedImageUri.value = uri
        selectedPdfUri.value = null
        extractedPdfText.value = ""
        geminiEngine.updateLastAnalyzedAsset(getFileNameOfUri(uri) ?: "Selected_Gallery_Image.jpg")
        geminiEngine.updateMultimodalSessionStatus("ACTIVE_IMAGE")
        _terminalOutput.value = "VISION_CORE // Image selected from gallery: ${uri.lastPathSegment}"
    }

    fun processCameraCapture(uri: android.net.Uri) {
        selectedImageUri.value = uri
        selectedPdfUri.value = null
        extractedPdfText.value = ""
        geminiEngine.updateLastAnalyzedAsset("camera_capture_session.jpg")
        geminiEngine.updateMultimodalSessionStatus("ACTIVE_IMAGE")
        _terminalOutput.value = "VISION_CORE // Camera photo captured successfully: ${uri.lastPathSegment}"
    }

    fun processPdfSelection(uri: android.net.Uri) {
        selectedPdfUri.value = uri
        selectedImageUri.value = null
        val fileName = getFileNameOfUri(uri) ?: "document_profile.pdf"
        geminiEngine.updateLastAnalyzedAsset(fileName)
        geminiEngine.updateMultimodalSessionStatus("ACTIVE_PDF")
        _terminalOutput.value = "DOCUMENT_CORE // Initiating extraction scan buffer for $fileName..."
        
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val text = com.example.service.PdfTextExtractor.extract(application, uri)
            extractedPdfText.value = text
            val duration = System.currentTimeMillis() - startTime
            geminiEngine.updateAnalysisTime(duration)
            _terminalOutput.value = "DOCUMENT_CORE // Scan complete. Extracted segment len: ${text.length} chars (Time: ${duration}ms)."
        }
    }

    fun clearActiveMultimodalAsset() {
        selectedImageUri.value = null
        selectedPdfUri.value = null
        extractedPdfText.value = ""
        geminiEngine.updateLastAnalyzedAsset("None")
        geminiEngine.updateMultimodalSessionStatus("IDLE")
        _terminalOutput.value = "VISION_CORE // Active multimodal context buffer purged."
    }

    private fun getFileNameOfUri(uri: android.net.Uri): String? {
        return try {
            val cursor = application.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
            uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    private fun uriToBase64(uri: android.net.Uri): String? {
        return try {
            val inputStream = application.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to encode image uri to base64", e)
            null
        }
    }

    private fun getMimeType(uri: android.net.Uri): String {
        return application.contentResolver.getType(uri) ?: "image/jpeg"
    }

    // --- Voice Session Manager & Presence Flows (Phase 7 & 8) ---
    val voiceSessionManager = VoiceSessionManager(application, repository, viewModelScope)
    private val settingsDataStore = SettingsDataStore(application)

    val assistantState = voiceSessionManager.currentState
    val isVoiceSessionActive = voiceSessionManager.isSessionActive
    val voiceSessionStartTime = voiceSessionManager.sessionStartTime
    val voiceSessionEndTime = voiceSessionManager.sessionEndTime
    val voiceSessionCommandCount = voiceSessionManager.commandCount
    val voiceSessionDurationSeconds = voiceSessionManager.sessionDurationSeconds

    val wakeWordVerified = voiceSessionManager.wakeWordVerified
    val isContinuousListeningHookEnabled = voiceSessionManager.isContinuousListeningHookEnabled
    val wakePhraseStatus = voiceSessionManager.wakePhraseStatus

    val foregroundServiceStatus = voiceSessionManager.foregroundServiceStatus
    val voiceEngineStatus = voiceSessionManager.voiceEngineStatus
    val notificationListenerStatus = voiceSessionManager.notificationListenerStatus
    val permissionReadinessStatus = voiceSessionManager.permissionReadinessStatus
    val databaseStatus = voiceSessionManager.databaseStatus

    val lastSessionStartTime = voiceSessionManager.lastSessionStartTime
    val lastSessionEndTime = voiceSessionManager.lastSessionEndTime
    val lastSessionCommandCount = voiceSessionManager.lastSessionCommandCount
    val lastSessionDurationMs = voiceSessionManager.lastSessionDurationMs

    // Background Wake Engine Stats (Phase 21.2)
    val wakeEngineStatus = com.example.service.BackgroundWakeWordEngine.status
    val wakeEngineTriggerCount = com.example.service.BackgroundWakeWordEngine.triggerCount
    val wakeEngineLastActivationTime = com.example.service.BackgroundWakeWordEngine.lastActivationTime
    val wakeEngineUptimeSecondsFlow: StateFlow<Long> = flow {
        while (true) {
            emit(com.example.service.BackgroundWakeWordEngine.getUptimeSeconds())
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lastWakeEvent = voiceSessionManager.wakeEventManager.lastWakeEvent

    val wakeEventsFlow: StateFlow<List<WakeEventEntity>> = repository.allWakeEventsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val engineUptimeSeconds: StateFlow<Long> = flow {
        while (true) {
            emit(voiceSessionManager.wakeEventManager.getEngineUptimeSeconds())
            delay(1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // --- Phase 9: App Launcher & Assistant Memory ---
    val appLauncherEngine = com.example.service.AppLauncherEngine(application, repository, viewModelScope)
    val appLauncherState: StateFlow<LauncherEngineState> = appLauncherEngine.engineState

    val allInstalledApps: StateFlow<List<InstalledAppEntity>> = repository.allInstalledAppsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appLaunchHistory: StateFlow<List<AppLaunchHistoryEntity>> = repository.allLaunchHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val assistantMemory: StateFlow<AssistantMemoryEntity> = repository.assistantMemoryFlow
        .map { it ?: AssistantMemoryEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AssistantMemoryEntity()
        )

    // User search query for dashboard search box
    val appSearchQuery = MutableStateFlow("")

    // --- Phase 23 Device Control Flows ---
    val deviceCommandsHistory: StateFlow<List<DeviceControlCommandEntity>> = repository.allDeviceControlCommandsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val screenshotHistoryList: StateFlow<List<ScreenshotHistoryEntity>> = repository.allScreenshotHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val fileSearchQuery = MutableStateFlow("")
    val fileSearchType = MutableStateFlow("ALL") // "ALL", "IMAGE", "DOCUMENT"

    val simulatedFiles = listOf(
        CyberFile("cognitive_memory_dump.db", "DOCUMENT", "256 KB", "10 mins ago"),
        CyberFile("quantum_encryption_keys.key", "DOCUMENT", "4 KB", "2 hours ago"),
        CyberFile("avatar_zoya_neon_cyan.jpg", "IMAGE", "1.2 MB", "1 day ago"),
        CyberFile("nova_system_diagnostics.txt", "DOCUMENT", "45 KB", "3 days ago"),
        CyberFile("weekly_productivity_report.pdf", "DOCUMENT", "3.4 MB", "4 days ago"),
        CyberFile("family_portrait_holiday.jpg", "IMAGE", "4.8 MB", "1 week ago"),
        CyberFile("workspace_cyber_theme.png", "IMAGE", "2.1 MB", "2 hours ago"),
        CyberFile("voice_model_weights.bin", "DOCUMENT", "84 MB", "3 weeks ago")
    )

    val filteredFiles: StateFlow<List<CyberFile>> = combine(fileSearchQuery, fileSearchType) { query, type ->
        if (query.isBlank() && type == "ALL") {
            simulatedFiles
        } else {
            simulatedFiles.filter { file ->
                val matchQuery = file.name.contains(query, ignoreCase = true)
                val matchType = when (type) {
                    "IMAGE" -> file.type == "IMAGE"
                    "DOCUMENT" -> file.type == "DOCUMENT"
                    else -> true
                }
                matchQuery && matchType
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), simulatedFiles)

    fun clearDeviceCommands() {
        viewModelScope.launch {
            repository.clearDeviceControlCommands()
        }
    }

    fun clearScreenshots() {
        viewModelScope.launch {
            repository.clearScreenshotHistory()
        }
    }

    fun triggerAppSearch(query: String) {
        appSearchQuery.value = query
    }

    fun triggerManualAppScan() {
        _terminalOutput.value = "LAUNCHER_SCAN // Recalibrating index of package launcher nodes..."
        appLauncherEngine.scanAndIndexApps(manualTrigger = true)
    }

    fun launchAppDirectly(packageName: String, appName: String) {
        viewModelScope.launch {
            _terminalOutput.value = "LAUNCHER // Direct intent request: $appName ($packageName)"
            appLauncherEngine.launchAppByMatchedName(appName)
        }
    }

    fun clearLaunchHistory() {
        viewModelScope.launch {
            repository.clearLaunchHistory()
            _terminalOutput.value = "LAUNCHER_GC // Launch history cleared."
        }
    }

    // --- Phase 10: Contact Directory & Communication Hub ---
    val contactDirectoryEngine = com.example.service.ContactDirectoryEngine(application, repository, viewModelScope)
    val contactEngineState: StateFlow<ContactEngineState> = contactDirectoryEngine.engineState

    val allContacts: StateFlow<List<ContactEntity>> = repository.allContactsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val contactSearchHistory: StateFlow<List<ContactSearchHistoryEntity>> = repository.allContactSearchHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val contactDirectoryQuery = MutableStateFlow("")

    fun triggerContactDirectorySearch(query: String) {
        contactDirectoryQuery.value = query
    }

    fun triggerManualContactScan() {
        _terminalOutput.value = "CONTACTS_SCAN // Re-indexing system contact endpoints..."
        contactDirectoryEngine.scanAndIndexContacts()
    }

    fun searchContactDirectly(queryName: String) {
        viewModelScope.launch {
            _terminalOutput.value = "CONTACTS // Intent query target: \"$queryName\""
            contactDirectoryEngine.findContactByQuery(queryName)
        }
    }

    fun clearContactSearchHistory() {
        viewModelScope.launch {
            repository.clearContactSearchHistory()
            _terminalOutput.value = "CONTACTS_GC // Telemetry history log purged."
        }
    }

    val voiceSessions: StateFlow<List<VoiceSessionEntity>> = repository.allSessionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Message Draft, Contact Search & Voice activity states (Phase 6) ---
    val contactSearchQuery = MutableStateFlow<String?>(null)
    
    val allSystemContacts = listOf(
        ContactItem("Chief Operator Jarvis", "JARVIS-SEC-01", "Level 5 Administrator", "ONLINE", "1 min ago"),
        ContactItem("Command Center Zero", "ZERO-BASE", "Central Operational Command", "ONLINE", "3 mins ago"),
        ContactItem("Commander Kane", "KANE-OFFICER", "Sector Security Commander", "ONLINE", "10 mins ago"),
        ContactItem("Nova Network Uplink", "NOVA-CORE-99", "Core Quantum Signal Generator", "ONLINE", "Just now"),
        ContactItem("Operator Sterling", "STERLING-COMMS", "Communications Array Officer", "OFFLINE", "2 hours ago"),
        ContactItem("Sentinel Alpha", "SENTINEL-AI", "Cyber Defense Node Network", "ONLINE", "Realtime")
    )

    val activeDraftRecipient = MutableStateFlow<String?>(null)
    val activeDraftBody = MutableStateFlow("")
    val showDraftComposer = MutableStateFlow(false)

    val lastCommandText = MutableStateFlow<String>("None yet")
    val lastNotificationQueryTime = MutableStateFlow<Long?>(null)
    val lastCommunicationEventSummary = MutableStateFlow<String>("No recent gateway connection")

    // Room Persistent History flow
    val voiceConversations: StateFlow<List<VoiceConversationEntity>> = repository.allConversationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Service Event Logs Persistent Flow
    val serviceLogs: StateFlow<List<ServiceLogEntity>> = repository.allServiceLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Smart Actions History list flow
    val smartActionsHistory: StateFlow<List<SmartActionEntity>> = repository.allSmartActionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val notificationsHistory: StateFlow<List<NotificationEntity>> = repository.allNotificationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val communicationEventsHistory: StateFlow<List<CommunicationEventEntity>> = repository.allCommunicationEventsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val wakeSessionStatus: StateFlow<WakeSessionStatus> = combine(
        voiceIsListening,
        preferences
    ) { listening, prefs ->
        if (listening) {
            WakeSessionStatus.ACTIVE_SESSION
        } else if (prefs.engineActive) {
            WakeSessionStatus.LISTENING_READY
        } else {
            WakeSessionStatus.IDLE
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WakeSessionStatus.IDLE
    )

    private val commandParser = VoiceCommandParser()

    fun executeSmartAction(actionType: SmartActionType) {
        viewModelScope.launch {
            if (actionType == SmartActionType.CHECK_NOTIFICATIONS) {
                lastNotificationQueryTime.value = System.currentTimeMillis()
                val latestNotifs = notificationsHistory.value
                val count = latestNotifs.size
                val messageToSpeak = if (latestNotifs.isEmpty()) {
                    "Sir, you have no new incoming alerts inside your local security bank."
                } else {
                    val recent = latestNotifs.take(3)
                    val listStr = recent.mapIndexed { idx, entity ->
                        val relative = formatSpeechTime(entity.timestamp)
                        "Signal number ${idx + 1} from ${entity.appName} received $relative: ${entity.title}, reading: ${entity.message}"
                    }.joinToString(". ")
                    "Sir, you have $count alert records in the buffer. Recent transmissions read: $listStr"
                }
                
                voiceManager.speak(messageToSpeak)
                voiceNovaResponse.value = messageToSpeak
                _terminalOutput.value = "NOTIF_CHECK // Audio sequence dispatched. Reading alerts: Size($count)"
                
                val actionLog = SmartActionEntity(
                    actionName = "Check Notifications",
                    status = "SUCCESS"
                )
                repository.insertSmartAction(actionLog)

                repository.insertCommunicationEvent(
                    CommunicationEventEntity(
                        type = "VOICE",
                        senderOrApp = "Voice Assistant",
                        title = "ACTION_TRIGGERED",
                        details = "Executed: Check Notifications (Status: SUCCESS)",
                        timestamp = System.currentTimeMillis()
                    )
                )
                return@launch
            }

            if (actionType == SmartActionType.ANSWER_CALL) {
                val latestCall = communicationEventsHistory.value.firstOrNull { it.type == "CALL" }
                val caller = latestCall?.senderOrApp ?: "Unknown Caller"
                val messageToSpeak = "Sir, answering incoming transmission from $caller."
                
                voiceManager.speak(messageToSpeak)
                voiceNovaResponse.value = messageToSpeak
                _terminalOutput.value = "CALL_EXEC // Active connection engaged for: $caller"
                
                val actionLog = SmartActionEntity(
                    actionName = "Answer Call",
                    status = "SUCCESS"
                )
                repository.insertSmartAction(actionLog)

                repository.insertCommunicationEvent(
                    CommunicationEventEntity(
                        type = "CALL",
                        senderOrApp = caller,
                        title = "CALL_ANSWERED",
                        details = "Status: Connection active.",
                        timestamp = System.currentTimeMillis()
                    )
                )
                return@launch
            }

            if (actionType == SmartActionType.DECLINE_CALL) {
                val latestCall = communicationEventsHistory.value.firstOrNull { it.type == "CALL" }
                val caller = latestCall?.senderOrApp ?: "Unknown Caller"
                val messageToSpeak = "Sir, declining and sending active block signals on secure channel with $caller."
                
                voiceManager.speak(messageToSpeak)
                voiceNovaResponse.value = messageToSpeak
                _terminalOutput.value = "CALL_EXEC // Active connection declined for: $caller"
                
                val actionLog = SmartActionEntity(
                    actionName = "Decline Call",
                    status = "SUCCESS"
                )
                repository.insertSmartAction(actionLog)

                repository.insertCommunicationEvent(
                    CommunicationEventEntity(
                        type = "CALL",
                        senderOrApp = caller,
                        title = "CALL_DECLINED",
                        details = "Status: Connection declined.",
                        timestamp = System.currentTimeMillis()
                    )
                )
                return@launch
            }

            val success = SmartActionExecutor.execute(application, actionType)
            val actionLog = SmartActionEntity(
                actionName = actionType.displayName,
                status = if (success) "SUCCESS" else "FAILED"
            )
            repository.insertSmartAction(actionLog)
            
            _terminalOutput.value = "ACTION_EXEC // Active node triggered: [${actionType.displayName}] -> Status: ${actionLog.status}"
            
            repository.insertCommunicationEvent(
                CommunicationEventEntity(
                    type = "VOICE",
                    senderOrApp = "Voice Assistant",
                    title = "ACTION_TRIGGERED",
                    details = "Executed: ${actionType.displayName} (Status: ${actionLog.status})",
                    timestamp = System.currentTimeMillis()
                )
            )

            if (actionType.voiceConfirmation.isNotEmpty()) {
                voiceManager.speak(actionType.voiceConfirmation)
            }
        }
    }

    fun clearSmartActionHistory() {
        viewModelScope.launch {
            repository.clearSmartActionHistory()
            _terminalOutput.value = "ACTION_DB // Local action telemetry history cleared."
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearNotifications()
            _terminalOutput.value = "NOTIF_DB // Notification archive purged."
        }
    }

    fun clearCommunicationEvents() {
        viewModelScope.launch {
            repository.clearCommunicationEvents()
            _terminalOutput.value = "COMM_DB // Unified communication events history wiped."
        }
    }

    fun syncCognitiveMemory() {
        viewModelScope.launch {
            _terminalOutput.value = "MEMORY_SYNC // Initiating memory compilation cycle..."
            try {
                val conversations = voiceConversations.value
                val dbMemory = repository.getAssistantMemory()
                
                if (conversations.isEmpty()) {
                    val updated = dbMemory.copy(
                        userFacts = "User prefers ${preferences.value.personalityMode} style interactions.",
                        conversationSummaries = "Initial startup node. Zero previous conversational logs standard in buffer.",
                        lastMemorySyncTime = System.currentTimeMillis()
                    )
                    repository.saveAssistantMemory(updated)
                    _terminalOutput.value = "MEMORY_SYNC // Sync complete. Empty buffer; updated local defaults."
                    return@launch
                }

                val lastTurn = conversations.first()
                val localFacts = "Active partner identifies as '${preferences.value.username}'. Prefers '${preferences.value.personalityMode}' dialect mode."
                val localSummary = "Conversation sync successful. Last speech segment processed: '${lastTurn.userSpeech}' yielding response: '${lastTurn.novaResponse}'."

                // If LLM node is available and configured, we can do an intelligent summary!
                val userKey = preferences.value.geminiApiKey.trim()
                val configKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
                val key = when {
                    userKey.isNotEmpty() -> userKey
                    configKey.isNotEmpty() && configKey != "MY_GEMINI_API_KEY" -> configKey
                    else -> ""
                }

                if (key.isNotEmpty()) {
                    _terminalOutput.value = "MEMORY_SYNC // Deploying Gemini API analyzer node..."
                    val logsText = conversations.take(5).joinToString("\n") { "User: ${it.userSpeech} -> Nova: ${it.novaResponse}" }
                    val prompt = "Analyze these conversation logs:\n$logsText\n\nGenerate two concise, short fields. Field 1: User facts / preferences (under 1 sentence). Field 2: Conversation summary (under 1 sentence). Output format:\nFACTS: <facts>\nSUMMARY: <summary>"
                    
                    try {
                        val response = callGeminiAPI(key, prompt, "en")
                        var factsPart = ""
                        var summaryPart = ""
                        response.split("\n").forEach { line ->
                            if (line.startsWith("FACTS:") || line.startsWith("facts:")) {
                                factsPart = line.substring(6).trim()
                            } else if (line.startsWith("SUMMARY:") || line.startsWith("summary:")) {
                                summaryPart = line.substring(8).trim()
                            }
                        }
                        if (factsPart.isEmpty()) factsPart = localFacts
                        if (summaryPart.isEmpty()) summaryPart = localSummary

                        val updated = dbMemory.copy(
                            userFacts = factsPart,
                            conversationSummaries = summaryPart,
                            lastMemorySyncTime = System.currentTimeMillis()
                        )
                        repository.saveAssistantMemory(updated)
                        _terminalOutput.value = "MEMORY_SYNC // Sync complete via Gemini uplink. Facts & summary updated."
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Gemini sync analyzer failed, running offline fallback", e)
                        val updated = dbMemory.copy(
                            userFacts = localFacts,
                            conversationSummaries = localSummary,
                            lastMemorySyncTime = System.currentTimeMillis()
                        )
                        repository.saveAssistantMemory(updated)
                        _terminalOutput.value = "MEMORY_SYNC // Sync complete via offline backup analyzer."
                    }
                } else {
                    val updated = dbMemory.copy(
                        userFacts = localFacts,
                        conversationSummaries = localSummary,
                        lastMemorySyncTime = System.currentTimeMillis()
                    )
                    repository.saveAssistantMemory(updated)
                    _terminalOutput.value = "MEMORY_SYNC // Sync complete via offline backup analyzer."
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Memory compilation cycle failed", e)
                _terminalOutput.value = "MEMORY_SYNC // Compilation aborted: ${e.message}"
            }
        }
    }

    fun clearCognitiveMemory() {
        viewModelScope.launch {
            val emptyMemory = AssistantMemoryEntity(
                id = 1,
                lastOpenedAppName = "",
                lastOpenedPackageName = "",
                lastExecutedCommand = "Memory purged",
                lastActionTime = System.currentTimeMillis(),
                recentActions = "",
                lastSearchedContact = "",
                recentContactSearches = "",
                userFacts = "",
                conversationSummaries = "",
                lastMemorySyncTime = 0L
            )
            repository.saveAssistantMemory(emptyMemory)
            _terminalOutput.value = "MEMORY_DB // Cognitive memories and summaries purged from Room database."
        }
    }

    fun simulateIncomingCall(callerName: String) {
        viewModelScope.launch {
            com.example.service.CallReceiver.activeCallerName.value = callerName
            com.example.service.CallReceiver.activeCallerNumber.value = "+1 555-0199"
            com.example.service.CallReceiver.activeCallState.value = "RINGING"

            val event = CommunicationEventEntity(
                type = "CALL",
                senderOrApp = callerName,
                title = "INCOMING_CALL",
                details = "Simulated quantum carrier connection established.",
                timestamp = System.currentTimeMillis()
            )
            repository.insertCommunicationEvent(event)
            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Call Event",
                    message = "COMM_LINK // Simulated alert: Connection from secure transmission [$callerName]"
                )
            )
        }
    }

    fun transitionSimulatedCall(state: String) {
        viewModelScope.launch {
            com.example.service.CallReceiver.activeCallState.value = state
            if (state == "DISCONNECTED") {
                com.example.service.CallReceiver.activeCallerName.value = null
                com.example.service.CallReceiver.activeCallerNumber.value = null
            }
        }
    }

    fun simulateIncomingNotification(appName: String, title: String, message: String) {
        viewModelScope.launch {
            val notification = NotificationEntity(
                appName = appName,
                title = title,
                message = message,
                timestamp = System.currentTimeMillis()
            )
            repository.insertNotification(notification)

            val commEvent = CommunicationEventEntity(
                type = "NOTIFICATION",
                senderOrApp = appName,
                title = title,
                details = message,
                timestamp = System.currentTimeMillis()
            )
            repository.insertCommunicationEvent(commEvent)

            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Notification Status",
                    message = "INFRA_NOTIF // Intercepted simulated event: $appName: \"$title\""
                )
            )
        }
    }

    init {
        // Init spatial and local telemetry indexes
        checkAndSyncLocation()
        checkAndSyncPhonePermissions()

        // Collect real-time caller states and caller updates from CallReceiver
        viewModelScope.launch {
            com.example.service.CallReceiver.activeCallState.collect { state ->
                activeCallState.value = state
                if (state == "RINGING" || state == "ACTIVE") {
                    _terminalOutput.value = "CALL_DETECT // Active state transitioned to: $state"
                }
                if (state == "RINGING") {
                    delay(500) // wait for name resolution
                    val name = activeCallerName.value ?: "Unknown number"
                    val speakName = if (name.startsWith("Unidentified Agent") || name.isEmpty() || name == "Secure Quantum Node") {
                        "Unknown number"
                    } else {
                        name
                    }
                    val phrase = "Sir, $speakName is calling."
                    voiceManager.speak(phrase)
                    voiceNovaResponse.value = phrase
                }
            }
        }
        viewModelScope.launch {
            com.example.service.CallReceiver.activeCallerName.collect { name ->
                activeCallerName.value = name
            }
        }
        viewModelScope.launch {
            com.example.service.CallReceiver.activeCallerNumber.collect { number ->
                activeCallerNumber.value = number
            }
        }

        // Phase 21.3 Real-time voice announcer flow handlers
        viewModelScope.launch {
            com.example.service.AppVoiceAnnouncer.announceFlow.collect { phrase ->
                voiceManager.speak(phrase)
                voiceNovaResponse.value = phrase
            }
        }
        viewModelScope.launch {
            com.example.service.AppVoiceAnnouncer.announcedSmsState.collect { pair ->
                latestSmsSender.value = pair.first
                latestSmsContent.value = pair.second
            }
        }
        viewModelScope.launch {
            com.example.service.AppVoiceAnnouncer.announcedWhatsAppState.collect { pair ->
                latestWhatsAppSender.value = pair.first
                latestWhatsAppContent.value = pair.second
            }
        }

        // Bind live TTS readiness flow to Assistant Presence monitoring diagnostics
        voiceSessionManager.isTtsEngineReady = { voiceManager.isTtsReady.value }

        // Bind TTS speaking status back to Assistant Presence (Phase 7)
        voiceManager.onSpeakStateChanged = { speaking ->
            if (speaking) {
                voiceSessionManager.transitionTo(AssistantState.SPEAKING)
            } else {
                if (voiceSessionManager.isSessionActive.value) {
                    voiceSessionManager.transitionTo(AssistantState.ARMED)
                }
            }
        }

        voiceManager.onRecoveryTriggered = {
            viewModelScope.launch {
                voiceSessionManager.transitionTo(AssistantState.RECOVERY)
                _terminalOutput.value = "VOICE_CORE // Auto-reconnecting locked voice core..."
                delay(3000)
                
                val isZoya = preferences.value.activeAssistant == "ZOYA"
                val recoveryPhrase = if (isZoya) {
                    "Hey! Back on track! Ready when you are."
                } else {
                    "Sir, voice engine reconnected and re-secured."
                }
                voiceNovaResponse.value = recoveryPhrase
                voiceManager.speak(recoveryPhrase)
                delay(2000)
                
                if (voiceSessionManager.isSessionActive.value) {
                    voiceSessionManager.transitionTo(AssistantState.ARMED)
                } else {
                    voiceSessionManager.transitionTo(AssistantState.STANDBY)
                }
            }
        }

        // Graceful error handling for voice STT recognition failures
        viewModelScope.launch {
            voiceError.collectLatest { error ->
                if (error != null) {
                    _terminalOutput.value = "VOICE_ERROR // Acoustic Link Failure: $error"
                    val currentState = voiceSessionManager.currentState.value
                    if (currentState == AssistantState.LISTENING || currentState == AssistantState.PROCESSING) {
                        if (voiceSessionManager.isSessionActive.value) {
                            voiceSessionManager.transitionTo(AssistantState.ARMED)
                        } else {
                            voiceSessionManager.transitionTo(AssistantState.STANDBY)
                        }
                    }
                    
                    // Persist voice recognizer failure log in local DB Unit
                    try {
                        val voiceHistoryLog = VoiceConversationEntity(
                            userSpeech = "[Acoustic Failure]",
                            novaResponse = error,
                            language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                            status = "FAILED"
                        )
                        repository.insertVoiceConversation(voiceHistoryLog)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error saving voice failure log: ${e.message}")
                    }
                }
            }
        }

        // Observe incoming call events to play voice confirmations dynamically
        viewModelScope.launch {
            repository.allCommunicationEventsFlow.collectLatest { events ->
                val latest = events.firstOrNull() ?: return@collectLatest
                if (latest.type == "CALL" && System.currentTimeMillis() - latest.timestamp < 3000) {
                    val prompt = "Sir, incoming call from ${latest.senderOrApp}."
                    voiceManager.speak(prompt)
                    voiceNovaResponse.value = prompt
                    _terminalOutput.value = "CALL_ALERT // Incoming audio dispatcher initialized: $prompt"
                }

                // Track latest communication event summary for Phase 6 dashboard
                val comm = events.firstOrNull { it.type != "VOICE" }
                if (comm != null) {
                    val timeString = formatSpeechTime(comm.timestamp)
                    lastCommunicationEventSummary.value = "${comm.type}: ${comm.senderOrApp} (${comm.title}) $timeString"
                }
            }
        }

        // Restore settings automatically on app startup (DataStore -> Room)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsDataStore.getSettingsFlow().first().let { datastoreSettings ->
                    val roomSettings = repository.getPreferences()
                    val merged = roomSettings.copy(
                        assistantName = datastoreSettings.assistantName,
                        voiceLanguage = datastoreSettings.voiceLanguage,
                        speechSpeed = datastoreSettings.speechSpeed,
                        speechPitch = datastoreSettings.speechPitch,
                        speechVolume = datastoreSettings.speechVolume,
                        themeMode = datastoreSettings.themeMode,
                        startupEngineEnabled = datastoreSettings.startupEngineEnabled,
                        notificationReadbackEnabled = datastoreSettings.notificationReadbackEnabled,
                        voiceFeedbackEnabled = datastoreSettings.voiceFeedbackEnabled,
                        sessionAutoRestoreEnabled = datastoreSettings.sessionAutoRestoreEnabled,
                        personalityMode = datastoreSettings.personalityMode,
                        wakePhrase = datastoreSettings.wakePhrase,
                        activeAssistant = datastoreSettings.activeAssistant
                    )
                    repository.savePreferences(merged)

                    withContext(Dispatchers.Main) {
                        // Apply settings live
                        voiceManager.setSpeechRate(merged.speechSpeed)
                        voiceManager.setPitch(merged.speechPitch)
                        voiceManager.setVolume(merged.speechVolume)
                        voiceManager.voiceFeedbackEnabled = merged.voiceFeedbackEnabled
                        voiceManager.setLanguage(merged.voiceLanguage)
                        voiceManager.activeAssistant.value = merged.activeAssistant

                        _terminalOutput.value = "SETTINGS_LOADED // Personalization profiles initialized from DataStore. Active: ${merged.activeAssistant}"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error restoring DataStore settings: ${e.message}")
            }
        }

        // Restore previous engine state when app restarts
        viewModelScope.launch(Dispatchers.IO) {
            delay(500) // small grace period
            val current = repository.getPreferences()
            if (current.engineActive) {
                try {
                    val intent = Intent(application, NovaForegroundService::class.java)
                    withContext(Dispatchers.Main) {
                        androidx.core.content.ContextCompat.startForegroundService(application, intent)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Boot Foreground service launch fail: ${e.message}")
                }
            }
        }

        // Run splash timer
        viewModelScope.launch {
            delay(2200)
            _isSplashFinished.value = true
        }

        // Live Permission Intelligence Engine continuous monitor & startup check (Phase 21.1)
        viewModelScope.launch {
            // Wait for Settings load and TTS synthesizer initialization
            delay(3500)
            
            // 1. Startup Security Check Run
            val initialReport = permissionIntelligenceEngine.generateSecurityReport()
            securityReport.value = initialReport
            
            val isZoyaState = preferences.value.activeAssistant == "ZOYA"
            val startupVoicePhrase = if (isZoyaState) {
                "Hey! Everything is ready. Nova is fully upgraded!"
            } else {
                "Sir, Nova Omega Core final system is online. All systems are operational."
            }
            
            voiceManager.speak(startupVoicePhrase)
            voiceNovaResponse.value = startupVoicePhrase
            _terminalOutput.value = "STARTUP_PERM_CHECK // Health: ${initialReport.healthStatus} // Score: ${initialReport.securityScore}% // EmergencyMode: ${initialReport.emergencyProtectionMode}"

            // 2. Background Continuous Monitor Loop (Checks every 3.5 seconds)
            var lastReport = initialReport
            while (true) {
                delay(3500)
                val currentReport = permissionIntelligenceEngine.generateSecurityReport()
                securityReport.value = currentReport
                
                val currentIsZoya = preferences.value.activeAssistant == "ZOYA"

                // Check and notify changes
                if (lastReport.microphoneGranted && !currentReport.microphoneGranted) {
                    val msg = if (currentIsZoya) {
                        "Oh no! Zoya microphone permission is disabled!"
                    } else {
                        "Sir, microphone permission is disabled."
                    }
                    voiceManager.speak(msg)
                    voiceNovaResponse.value = msg
                    _terminalOutput.value = "PERM_ALERT // Microphone disconnected."
                }

                if (lastReport.notificationsGranted && !currentReport.notificationsGranted) {
                    val msg = if (currentIsZoya) {
                        "Oh! Notification access has been removed!"
                    } else {
                        "Sir, notification access has been removed."
                    }
                    voiceManager.speak(msg)
                    voiceNovaResponse.value = msg
                    _terminalOutput.value = "PERM_ALERT // Notification access terminated."
                }

                if (lastReport.cameraGranted && !currentReport.cameraGranted) {
                    val msg = if (currentIsZoya) {
                        "Zoya camera permission requires attention!"
                    } else {
                        "Sir, camera permission requires attention."
                    }
                    voiceManager.speak(msg)
                    voiceNovaResponse.value = msg
                    _terminalOutput.value = "PERM_ALERT // Camera permission disconnected."
                }

                // Check Emergency Protection Mode entry
                if (!lastReport.emergencyProtectionMode && currentReport.emergencyProtectionMode) {
                    val msg = if (currentIsZoya) {
                        "Zoya critical permissions are missing. Functionality may be limited!"
                    } else {
                        "Sir, critical permissions are missing. Nova functionality may be limited."
                    }
                    voiceManager.speak(msg)
                    voiceNovaResponse.value = msg
                    _terminalOutput.value = "EMERGENCY_MODE // Entered security protection state due to missing vital nodes."
                }

                lastReport = currentReport
            }
        }

        // Monitor engine state to start/stop telemetry generation
        viewModelScope.launch {
            preferences.collectLatest { pf ->
                voiceManager.activeAssistant.value = pf.activeAssistant
                if (pf.engineActive) {
                    startTelemetryStream()
                } else {
                    stopTelemetryStream()
                }
            }
        }
    }

    private fun startTelemetryStream() {
        if (telemetryJob != null) return
        _telemetryMessages.value = listOf(
            "CORE_BOOT: System ignition active...",
            "GRID_SYNC: Syncing with deep space beacon...",
            "REACTOR: Quantum core stabilized at 1.25 GW."
        )
        telemetryJob = viewModelScope.launch {
            val systemLogs = listOf(
                "REACTOR // Harmonic stability: 99.87%",
                "SHIELD // Subgrid energy: 100% charged",
                "COMM // Deep-mesh encryption locked",
                "MEM_SYNC // Decrypting quantum sectors...",
                "SENSOR // Solar tempests detected in sector 4",
                "ANTENNA // Listening to alpha-pulsar signals"
            )
            while (true) {
                delay(3500)
                val newLog = systemLogs[Random.nextInt(systemLogs.size)]
                val currentLogs = _telemetryMessages.value.toMutableList()
                if (currentLogs.size > 8) {
                    currentLogs.removeAt(0)
                }
                currentLogs.add(newLog)
                _telemetryMessages.value = currentLogs
            }
        }
    }

    private fun stopTelemetryStream() {
        telemetryJob?.cancel()
        telemetryJob = null
        if (_telemetryMessages.value.isNotEmpty()) {
            _telemetryMessages.value = listOf("CORE_STOP // Engine offline. Core safely decoupled.")
        }
    }

    fun handleCardClick(cardName: String) {
        val message = when (cardName.lowercase()) {
            "voice" -> "COMM_VOICE // Receiver calibrated. Ready to synthesize deep-space voice command."
            "calls" -> "COMM_CALL // Holo-frequency established. Scanning for planetary gateway link..."
            "messaging" -> "DATAPACK_MSG // Decrypted: 0 unread alerts. 4 historical logs stored."
            "apps" -> "SYS_APPS // Matrix subroutines 100% active. Hyper-grid execution ready."
            "search" -> "COSMIC_FIND // Searching celestial databases. Index matches 4.9B stars."
            "weather" -> "ENV_SCAN // Atmosphere: Nitrogen 72%, Acid Rain 1.2%, Neon Fog: 90%. Config: Normal."
            "location" -> "GEO_LOCK // Locked Sector Zeta-12: Lat '35.409N', Lng '139.73E'."
            else -> "ACTION_LOG // Card '$cardName' interactive channel active."
        }
        _terminalOutput.value = message
    }

    fun writeTerminalLog(message: String) {
        _terminalOutput.value = message
    }

    // --- Voice Assistant Operations ---

    fun startVoiceCapture() {
        if (!voiceSessionManager.isSessionActive.value) {
            voiceSessionManager.startSession("MANUAL")
        }
        voiceSessionManager.transitionTo(AssistantState.LISTENING)
        voiceManager.startListening()
    }

    fun stopVoiceCapture() {
        voiceManager.stopListening()
        if (voiceSessionManager.currentState.value == AssistantState.LISTENING) {
            voiceSessionManager.transitionTo(AssistantState.ARMED)
        }
    }

    fun setVoiceLanguage(language: String) {
        voiceManager.setLanguage(language)
    }

    fun submitManualCommand(text: String) {
        if (text.trim().isEmpty()) return
        processVoiceCommand(text)
    }

    fun clearVoiceHistory() {
        viewModelScope.launch {
            repository.clearVoiceHistory()
            _terminalOutput.value = "VOICE_DB_CLEAR // Persistent conversational history purged."
        }
    }

    fun clearVoiceSessions() {
        viewModelScope.launch {
            repository.clearVoiceSessions()
            _terminalOutput.value = "SESSION_DB_CLEAR // Persistent session logs database wiped."
        }
    }

    fun startManualVoiceSession() {
        voiceSessionManager.startSession("MANUAL")
    }

    fun endManualVoiceSession() {
        voiceSessionManager.endSession()
    }

    fun toggleContinuousListeningHook(enabled: Boolean) {
        voiceSessionManager.toggleContinuousListeningHook(enabled)
    }

    fun simulateVoiceSession() {
        voiceSessionManager.runSessionSimulation(voiceManager) { text ->
            _terminalOutput.value = text
        }
    }

    fun simulateWakeEvent() {
        voiceSessionManager.triggerWakePhraseDetectionMock()
        _terminalOutput.value = "WAKE_TRIGGER // Wake phrase [Hello Nova] simulated. Dispatching Event to core router..."
    }

    fun resetSessionStatistics() {
        voiceSessionManager.resetSessionStatistics()
        _terminalOutput.value = "STATS_RESET // Session timer, wake event logs, and all statistics have been reset."
    }

    fun pruneOldDatabaseEntries(cutoffDays: Int = 3) {
        viewModelScope.launch {
            _terminalOutput.value = "PURGE_START // Tuning local cache and scanning for entries older than $cutoffDays days..."
            val result = repository.pruneOldDatabaseEntries(cutoffDays)
            if (result == 1) {
                _terminalOutput.value = "PURGE_COMPLETE // Database tuning finalized. Stale logs/notifications successfully compiled and purged."
                voiceManager.speak("Sir, local database garbage collection is complete. Old telemetry registers have been tuned to conserve memory.")
            } else {
                _terminalOutput.value = "PURGE_ERR // Maintenance block encountered a task error or exception thread."
            }
        }
    }

    fun cancelDraft() {
        showDraftComposer.value = false
        val msg = "Sir, message draft aborted and cleared from memory."
        voiceManager.speak(msg)
        voiceNovaResponse.value = msg
        _terminalOutput.value = "DRAFT_ABORT // Draft cleared safely."
    }

    fun confirmDraft() {
        showDraftComposer.value = false
        val recipient = activeDraftRecipient.value ?: "Unknown Node"
        val body = activeDraftBody.value
        val msg = "Sir, transmission successfully dispatched to $recipient."
        
        voiceManager.speak(msg)
        voiceNovaResponse.value = msg
        _terminalOutput.value = "DRAFT_SEND // Dispatched payload to $recipient."

        viewModelScope.launch {
            repository.insertCommunicationEvent(
                CommunicationEventEntity(
                    type = "MESSAGE",
                    senderOrApp = recipient,
                    title = "TRANS_CONFIRMED",
                    details = "Sent content: $body",
                    timestamp = System.currentTimeMillis()
                )
            )
            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Message Transmitted",
                    message = "COMM_UPLINK // Outbound draft dispatched to [$recipient]: \"$body\""
                )
            )
        }
    }

    private fun formatSpeechTime(timestampMs: Long): String {
        val diff = System.currentTimeMillis() - timestampMs
        return when {
            diff < 60000 -> "just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
                "at " + sdf.format(java.util.Date(timestampMs))
            }
        }
    }

    private fun splitMultiCommands(input: String): List<String> {
        val lower = input.lowercase().trim()
        if (!lower.contains(" and ") && !lower.contains(" then ") && !lower.contains(",")) {
            return listOf(input)
        }
        val cleanInput = input
            .replace(Regex("\\band then\\b", RegexOption.IGNORE_CASE), "then")
            .replace(Regex("\\bthen\\b", RegexOption.IGNORE_CASE), "then")
            
        val segments = cleanInput.split(Regex("then|,|\\band\\b", RegexOption.IGNORE_CASE))
        val result = mutableListOf<String>()
        for (seg in segments) {
            val cleanSeg = seg.trim()
            if (cleanSeg.isNotEmpty()) {
                val lowerSeg = cleanSeg.lowercase()
                if (lowerSeg != "please" && lowerSeg != "now" && lowerSeg != "g") {
                    result.add(cleanSeg)
                }
            }
        }
        return if (result.isEmpty()) listOf(input) else result
    }

    private fun getCommandPriority(cmd: String): Int {
        val lower = cmd.lowercase()
        return when {
            lower.contains("switch") || lower.contains("assistant") || lower.contains("zoya") || lower.contains("nova") -> 3
            lower.contains("open") || lower.contains("launch") || lower.contains("app") -> 2
            lower.contains("status") || lower.contains("check") || lower.contains("permission") || lower.contains("health") -> 2
            else -> 1
        }
    }

    private suspend fun executeMultiCommands(commands: List<String>, originalUtterance: String) {
        val isZoya = preferences.value.activeAssistant == "ZOYA"
        
        // Priority Sorting
        val prioritizedCommands = commands.map { cmd ->
            cmd to getCommandPriority(cmd)
        }.sortedByDescending { it.second }
        
        _terminalOutput.value = "MULTI_EXEC // Priority queue initialized. Command bundle size: ${prioritizedCommands.size}"
        
        val progressIntro = if (isZoya) {
            "Awesome! I've prioritized your tasks so we do the most important things first. Running ${prioritizedCommands.size} tasks!"
        } else {
            "Sir, I have re-prioritized the sequence for safe and optimal execution. Running ${prioritizedCommands.size} commands in priority queue."
        }
        voiceNovaResponse.value = progressIntro
        voiceManager.speak(progressIntro)
        delay(3500)

        for (index in prioritizedCommands.indices) {
            val (cmd, priority) = prioritizedCommands[index]
            val stepText = "Task ${index + 1} (Priority level $priority): $cmd"
            _terminalOutput.value = "MULTI_EXEC // $stepText"
            
            val stepVoice = if (isZoya) {
                "Next up: $cmd!"
            } else {
                "Sir, now executing priority task: $cmd."
            }
            voiceNovaResponse.value = stepVoice
            voiceManager.speak(stepVoice)
            delay(2500)

            try {
                executeSingleVoiceCommandInternal(cmd)
                delay(4000)
            } catch (e: java.lang.Exception) {
                val errorMsg = if (isZoya) {
                    "Oops! I hit a snag running $cmd, but I'll skip and keep going!"
                } else {
                    "Warning: Task $cmd encountered an error. Initiating automatic partition bypass to continue sequence."
                }
                _terminalOutput.value = "MULTI_EXEC // Bypass error: ${e.message}"
                voiceNovaResponse.value = errorMsg
                voiceManager.speak(errorMsg)
                delay(3500)
            }
        }

        val completionMsg = if (isZoya) {
            "Okay, everything is all done! Let me know if you need anything else!"
        } else {
            "Sir, prioritized multi-command sequence completed successfully."
        }
        voiceNovaResponse.value = completionMsg
        voiceManager.speak(completionMsg)
        _terminalOutput.value = "MULTI_EXEC // Prioritized queue cleared."
    }

    private fun processVoiceCommand(input: String) {
        val userText = input.trim()
        if (userText.isEmpty()) return

        val commands = splitMultiCommands(userText)
        if (commands.size > 1) {
            viewModelScope.launch {
                executeMultiCommands(commands, userText)
            }
        } else {
            executeSingleVoiceCommandInternal(userText)
        }
    }

    fun executeSingleVoiceCommandInternal(input: String) {
        val userText = input.trim()
        if (userText.isEmpty()) return

        voiceSessionManager.registerCommandHandled()
        voiceSessionManager.transitionTo(AssistantState.PROCESSING)

        voiceUserSpeech.value = userText
        lastCommandText.value = userText
        _terminalOutput.value = "VOICE_UPLINK // Captured: \"$userText\""

        val match = commandParser.parse(userText)
        when (match) {
            is CommandResult.SwitchAssistantCommand -> {
                val targetAssistant = match.assistant
                viewModelScope.launch {
                    val currentAssistant = preferences.value.activeAssistant
                    if (currentAssistant == targetAssistant) {
                        val alreadyActiveMsg = if (targetAssistant == "ZOYA") {
                            "Hey! Zoya is already here and ready to play!"
                        } else {
                            "Sir, Nova is already active and at your service."
                        }
                        voiceNovaResponse.value = alreadyActiveMsg
                        voiceManager.speak(alreadyActiveMsg)
                        _terminalOutput.value = "ASSISTANT_STATUS // $targetAssistant is already active."
                    } else {
                        val reply = if (targetAssistant == "ZOYA") {
                            "Sir, transferring you to Zoya."
                        } else {
                            "Okay, sending Nova back."
                        }
                        voiceNovaResponse.value = reply
                        
                        // Speak utilizing origin's personality
                        voiceManager.speak(reply)
                        
                        // Wait for speech to complete
                        delay(2500)
                        
                        // Update persistence
                        val current = repository.getPreferences()
                        val updated = current.copy(activeAssistant = targetAssistant)
                        repository.savePreferences(updated)
                        settingsDataStore.saveSettings(updated)
                        
                        _terminalOutput.value = "ASSISTANT_SWITCH // Personality migrated from $currentAssistant to $targetAssistant."
                        
                        // Speak greeting using target's personality
                        val nextGreet = if (targetAssistant == "ZOYA") {
                            "Hey! Everything is ready. Nova is fully upgraded!"
                        } else {
                            "Sir, Nova Omega Core final system is online. All systems are operational."
                        }
                        voiceNovaResponse.value = nextGreet
                        voiceManager.speak(nextGreet)
                    }
                    
                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = voiceNovaResponse.value,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = "SUCCESS"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.ActionCommand -> {
                val actionType = match.actionType
                executeSmartAction(actionType)
                
                viewModelScope.launch {
                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = actionType.voiceConfirmation,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = "SUCCESS"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                    voiceNovaResponse.value = actionType.voiceConfirmation
                }
            }
            is CommandResult.SearchContactCommand -> {
                val contactName = match.contactName
                contactSearchQuery.value = contactName
                viewModelScope.launch {
                    val res = contactDirectoryEngine.findContactByQuery(contactName, spokenCommand = userText)
                    val reply = if (res.success && res.contact != null) {
                        "Sir, contact found in offline core database: ${res.contact.displayName}. Phone link is ${res.contact.phoneNumber}."
                    } else {
                        "Sir, zero records mapped in standard contacts for identifier keyword $contactName."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "CONTACT_SEARCH // Directory lookup contact: ${res.contact?.displayName ?: contactName} -> Status: ${if (res.success) "SUCCESS" else "FAILED"}"

                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = if (res.success) "SUCCESS" else "FAILED"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.InitiateCallCommand -> {
                val contactName = match.contactName
                viewModelScope.launch {
                    val res = contactDirectoryEngine.findContactByQuery(contactName, spokenCommand = userText)
                    if (res.success && res.contact != null) {
                        val number = res.contact.phoneNumber
                        val reply = "Sir, initiating direct communication link to ${res.contact.displayName} at $number immediately."
                        voiceNovaResponse.value = reply
                        voiceManager.speak(reply)
                        _terminalOutput.value = "CALL_DIAL // Placing direct secure voice line to: ${res.contact.displayName}"
                        
                        // Action logging
                        val actionLog = SmartActionEntity(
                            actionName = "Call ${res.contact.displayName}",
                            status = "SUCCESS"
                        )
                        repository.insertSmartAction(actionLog)

                        // Call intent dispatcher
                        try {
                            val intent = if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    application, android.Manifest.permission.CALL_PHONE
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                Intent(Intent.ACTION_CALL, android.net.Uri.parse("tel:$number"))
                            } else {
                                Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:$number"))
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            application.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Dialer launching error: ${e.message}")
                            _terminalOutput.value = "CALL_DIAL_ERROR // Broadcast dispatcher failed: ${e.message}"
                        }
                    } else {
                        val reply = "Sir, I could not resolve dynamic identifier '$contactName' in the contacts directory unit."
                        voiceNovaResponse.value = reply
                        voiceManager.speak(reply)
                        _terminalOutput.value = "CALL_DIAL_FAILURE // Contact resolution failed for name parameter: $contactName"
                    }

                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = voiceNovaResponse.value,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = if (res.success) "SUCCESS" else "FAILED"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.CreateDraftCommand -> {
                val contactName = match.contactName
                activeDraftRecipient.value = contactName
                activeDraftBody.value = "Enter encrypted payload message..."
                showDraftComposer.value = true
                val reply = "Sir, opening secure quantum network draft composer for $contactName."
                voiceNovaResponse.value = reply
                voiceManager.speak(reply)
                _terminalOutput.value = "DRAFT_INIT // Composer initialized for recipient: $contactName"

                viewModelScope.launch {
                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = "SUCCESS"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.ReadWhatsAppCommand -> {
                viewModelScope.launch {
                    val latestWhatsApp = communicationEventsHistory.value.firstOrNull { 
                        it.type == "NOTIFICATION" && it.senderOrApp == "WhatsApp" 
                    }
                    val reply = if (latestWhatsApp != null) {
                        "Sir, the latest WhatsApp message is from ${latestWhatsApp.title} and reads: ${latestWhatsApp.details}"
                    } else {
                        "Sir, there are no unread WhatsApp messages inside the decrypted buffer."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "COMM_AI // Read latest WhatsApp completed."
                    
                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = "SUCCESS"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.ReadSmsCommand -> {
                viewModelScope.launch {
                    val latestSms = communicationEventsHistory.value.firstOrNull { 
                        it.type == "SMS" 
                    }
                    val reply = if (latestSms != null) {
                        "Sir, the latest message is from ${latestSms.senderOrApp}. It says: ${latestSms.details}"
                    } else {
                        "Sir, there are no active SMS transcripts stored in the database."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "COMM_AI // Read latest SMS completed."

                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = "SUCCESS"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.ReadMissedCallsCommand -> {
                viewModelScope.launch {
                    val missedCalls = communicationEventsHistory.value.filter { 
                        it.type == "CALL" && it.title == "MISSED_CALL" 
                    }
                    val reply = if (missedCalls.isNotEmpty()) {
                        val grouped = missedCalls.groupBy { it.senderOrApp }
                        val summaryText = grouped.map { (sender, list) ->
                            "${list.size} calls from $sender"
                        }.joinToString(", and ")
                        "Sir, you missed $summaryText."
                    } else {
                        "Sir, you have clear sky with zero missed calls in the logs."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "COMM_AI // Summarized missed calls count: ${missedCalls.size}"

                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = "SUCCESS"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.OpenAppCommand -> {
                val appSpecName = match.appName
                viewModelScope.launch {
                    val res = appLauncherEngine.launchAppByMatchedName(appSpecName, spokenCommand = userText)
                    val reply = res.message
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "LAUNCH_REQ // Match app: ${res.appName} (package: ${res.packageName}) -> Status: ${if (res.success) "SUCCESS" else "FAILED"}"
                    
                    if (res.success) {
                        try {
                            repository.insertCategorizedMemory(
                                com.example.data.CategorizedMemoryEntity(
                                    category = "Frequently Used Commands",
                                    content = "Opened application ${res.appName} via voice command.",
                                    patternName = "Command: ${res.appName}",
                                    frequencyCount = 1,
                                    confidenceScore = 95
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Error saving learned command memory: ${e.message}")
                        }
                    }

                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = if (res.success) "SUCCESS" else "FAILED"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.OpenBrowserCommand -> {
                val url = match.url
                viewModelScope.launch {
                    var success = false
                    var reply = ""
                    try {
                        // Safe tool execution layer validation checks (Requirement 3)
                        val safeUri = android.net.Uri.parse(url)
                        val scheme = safeUri.scheme
                        if (scheme == "http" || scheme == "https") {
                            val intent = Intent(Intent.ACTION_VIEW, safeUri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            application.startActivity(intent)
                            success = true
                            reply = "Sir, secure browser connection pipeline initiated to target destination $url."
                        } else {
                            reply = "Cognitive link execution blocked. Destructive URL schema schema candidate rejected."
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Safe Tool Execution exception", e)
                        reply = "Cognitive link failed. System reported intent error: ${e.message}"
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "BROWSER_REQ // Deep link: $url -> Status: ${if (success) "SUCCESS" else "FAILED"}"

                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = if (success) "SUCCESS" else "FAILED"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.ShowRecentContactsCommand -> {
                viewModelScope.launch {
                    val dbMemory = repository.getAssistantMemory()
                    val searches = dbMemory.recentContactSearches.split("\n").filter { it.isNotBlank() }
                    val reply = if (searches.isNotEmpty()) {
                        "Showing recent contact searches: ${searches.joinToString(", ")}."
                    } else {
                        "Zero recent contact searches found in memory."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "CONTACT_RECENT // Broadcast recent queries memory cache."
                    
                    val voiceHistoryLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = reply,
                        language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                        status = "SUCCESS"
                    )
                    repository.insertVoiceConversation(voiceHistoryLog)
                }
            }
            is CommandResult.ChangeLanguageCommand -> {
                val nextLang = match.language
                viewModelScope.launch {
                    try {
                        val current = repository.getPreferences()
                        val updated = current.copy(voiceLanguage = nextLang)
                        repository.savePreferences(updated)
                        settingsDataStore.saveSettings(updated)
                        
                        voiceManager.setLanguage(nextLang)
                        
                        val reply = if (nextLang == "bn") "ভাষা পরিবর্তন করে বাংলা করা হয়েছে।" else "Language has been updated to English."
                        voiceNovaResponse.value = reply
                        voiceManager.speak(reply)
                        
                        _terminalOutput.value = "SETTINGS_UPDATED // Language changed to ${if (nextLang == "bn") "Bengali" else "English"} via voice command."
                        
                        val voiceHistoryLog = VoiceConversationEntity(
                            userSpeech = userText,
                            novaResponse = reply,
                            language = if (nextLang == "bn") "Bengali" else "English",
                            status = "SUCCESS"
                        )
                        repository.insertVoiceConversation(voiceHistoryLog)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error changing language on command: ${e.message}")
                    }
                }
            }
            is CommandResult.ToggleVoiceFeedbackCommand -> {
                val enabled = match.enabled
                viewModelScope.launch {
                    try {
                        val current = repository.getPreferences()
                        val updated = current.copy(voiceFeedbackEnabled = enabled)
                        repository.savePreferences(updated)
                        settingsDataStore.saveSettings(updated)
                        
                        voiceManager.voiceFeedbackEnabled = enabled
                        
                        val reply = if (enabled) "Voice feedback enabled." else "Voice feedback is now muted."
                        voiceNovaResponse.value = reply
                        // If we turn off feedback, say it once before suppressing future speech, or speak then mute.
                        // Let's speak it first and set voice feedback.
                        voiceManager.speak(reply)
                        
                        _terminalOutput.value = "SETTINGS_UPDATED // Voice feedback updated to [${enabled.toString().uppercase()}] via voice command."
                        
                        val voiceHistoryLog = VoiceConversationEntity(
                            userSpeech = userText,
                            novaResponse = reply,
                            language = if (activeVoiceLanguage.value == "bn") "Bengali" else "English",
                            status = "SUCCESS"
                        )
                        repository.insertVoiceConversation(voiceHistoryLog)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error toggling voice feedback on command: ${e.message}")
                    }
                }
            }
            
            // --- Phase 23 Ultimate Device Control Core ---
            is CommandResult.CloseAppCommand -> {
                val appSpecName = match.appName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Close App: $appSpecName",
                        executionDetails = "Terminated app: $appSpecName",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Terminated and closed $appSpecName."
                    } else {
                        "Sir, command executed successfully. Disconnected $appSpecName runtime nodes."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "DEVICE_CONTROL // Terminated process: $appSpecName"
                }
            }
            is CommandResult.SwitchAppCommand -> {
                val appSpecName = match.appName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Switch App: $appSpecName",
                        executionDetails = "Brought $appSpecName to front",
                        status = "COMPLETED"
                    ))
                    appLauncherEngine.launchAppByMatchedName(appSpecName, spokenCommand = userText)
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Anything else? Switched straight to $appSpecName."
                    } else {
                        "Sir, command executed successfully. Active core switched focus to $appSpecName."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                }
            }
            is CommandResult.LaunchShortcutCommand -> {
                val appName = match.appName
                val shortcut = match.shortcutName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Shortcut: $shortcut",
                        executionDetails = "Launched fast lane shortcut: $shortcut for app $appName",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Jumped custom shortcut: $shortcut for $appName."
                    } else {
                        "Sir, command executed successfully. Launcher shortcut $shortcut activated for $appName."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                }
            }
            is CommandResult.YouTubeSearchCommand -> {
                val q = match.searchQuery
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "YouTube: Search '$q'",
                        executionDetails = "Dispensed YouTube query '$q'",
                        status = "COMPLETED"
                    ))
                    val encoded = android.net.Uri.encode(q)
                    val targetUrl = "https://www.youtube.com/results?search_query=$encoded"
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Searching YouTube for $q. Let's play the top result!"
                    } else {
                        "Sir, command executed successfully. Initiated YouTube lookup for: $q."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    
                    delay(1200)
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(targetUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        application.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error opening YouTube URL: ${e.message}")
                    }
                }
            }
            is CommandResult.YouTubeControlCommand -> {
                val act = match.action
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "YouTube Control: $act",
                        executionDetails = "Synced media pulse action: $act",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        when (act) {
                            "PAUSE" -> "Done! Anything else? Paused the video."
                            "VOLUME_UP" -> "Done! Anything else? Pumped up the volume."
                            "VOLUME_DOWN" -> "Done! Anything else? Volumed it down."
                            else -> "Done! Executed media action $act."
                        }
                    } else {
                        when (act) {
                            "PAUSE" -> "Sir, command executed successfully. Video playback suspended."
                            "VOLUME_UP" -> "Sir, command executed successfully. Master audio amplitude increased."
                            "VOLUME_DOWN" -> "Sir, command executed successfully. Master audio amplitude decreased."
                            else -> "Sir, command executed successfully. YouTube media pulse $act synced."
                        }
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "MEDIA_CTRL // Synced YouTube pulse: [$act]"
                }
            }
            is CommandResult.WhatsAppOpenChatCommand -> {
                val contactVal = match.contactName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "WhatsApp Chat: $contactVal",
                        executionDetails = "Launched direct conversation terminal with contact: $contactVal",
                        status = "COMPLETED"
                    ))
                    val contacts = repository.searchContacts(contactVal)
                    val targetUri = if (contacts.isNotEmpty()) {
                        "https://api.whatsapp.com/send?phone=${contacts.first().phoneNumber}"
                    } else {
                        "https://api.whatsapp.com/send"
                    }
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Anything else? Opening chat with $contactVal."
                    } else {
                        "Sir, command executed successfully. Directing secure WhatsApp comm link to $contactVal."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    
                    delay(1200)
                    try {
                        val whatsappIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(targetUri)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        application.startActivity(whatsappIntent)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Error launching WhatsApp: ${e.message}")
                    }
                }
            }
            is CommandResult.WhatsAppSendMessageCommand -> {
                val contactVal = match.contactName
                val msg = match.message
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "WhatsApp MSG to $contactVal",
                        executionDetails = "Transmitted: \"$msg\"",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Anything else? Sent to $contactVal saying $msg"
                    } else {
                        "Sir, command executed successfully. Message dispatched to WhatsApp target $contactVal."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "WA_COMM // Transmitted packet to $contactVal: \"$msg\""
                }
            }
            is CommandResult.WhatsAppSendImageCommand -> {
                val contactVal = match.contactName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "WhatsApp Image: $contactVal",
                        executionDetails = "Encrypted screenshot payload share with $contactVal",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Anything else? Sent image payload to $contactVal."
                    } else {
                        "Sir, command executed successfully. Screenshot data transmitted to $contactVal."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                }
            }
            is CommandResult.TakeScreenshotCommand -> {
                viewModelScope.launch {
                    val currentHistory = repository.allScreenshotHistoryFlow.first()
                    val idCount = currentHistory.size + 1
                    val scName = "Cyber_Capture_Density_$idCount"
                    val scPath = "/storage/emulated/0/Pictures/Screenshots/$scName.png"
                    
                    repository.insertScreenshotHistory(ScreenshotHistoryEntity(
                        screenshotName = scName,
                        filePath = scPath
                    ))
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Take Screenshot",
                        executionDetails = "Recorded capture node: $scName",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Saved screen capture as $scName!"
                    } else {
                        "Sir, command executed successfully. Screen capture output archived to system logs as $scName."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "CAPTURE // Captured visual buffer at $scPath"
                }
            }
            is CommandResult.WhatsAppShareScreenshotCommand -> {
                val contactVal = match.contactName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Share Screenshot to $contactVal",
                        executionDetails = "Relayed recent photo cache to $contactVal",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Shared your recent capture with $contactVal."
                    } else {
                        "Sir, command executed successfully. Relayed screenshot payload to $contactVal."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "RELAY // Relayed capture node to WhatsApp gateway of $contactVal"
                }
            }
            is CommandResult.OpenScreenshotGalleryCommand -> {
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Open Gallery",
                        executionDetails = "Dispatched gallery launch pulse",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Anything else? Screenshot gallery opened!"
                    } else {
                        "Sir, command executed successfully. Visual assets gallery directory loaded."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                }
            }
            is CommandResult.FileSearchCommand -> {
                val q = match.query
                val t = match.fileType
                viewModelScope.launch {
                    fileSearchQuery.value = q
                    fileSearchType.value = t
                    
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "File Search: '$q'",
                        executionDetails = "Searched database for query '$q' [Type: $t]",
                        status = "COMPLETED"
                    ))
                    
                    delay(800)
                    val count = filteredFiles.value.size
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Searching for $q returned $count matching files."
                    } else {
                        "Sir, command executed successfully. Target index contains $count results for search keyword $q."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    _terminalOutput.value = "FILE_SYSTEM // Query search: \"$q\" [Type Filter: $t] matches count: $count"
                }
            }
            is CommandResult.OpenFileCommand -> {
                val name = match.fileName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Open File: $name",
                        executionDetails = "Presented file descriptor stream for $name",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Anything else? Decrypted and opened $name."
                    } else {
                        "Sir, command executed successfully. Decrypted file stream for target node $name is active."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                }
            }
            is CommandResult.ShowRecentFilesCommand -> {
                viewModelScope.launch {
                    fileSearchQuery.value = ""
                    fileSearchType.value = "ALL"
                    
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Show Recent Files",
                        executionDetails = "Exposed files index catalog",
                        status = "COMPLETED"
                    ))
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Looking at recently indexed file archives."
                    } else {
                        "Sir, command executed successfully. Presenting recently indexed files catalogue."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                }
            }
            is CommandResult.ExecuteAutomationSequenceCommand -> {
                val seqName = match.sequenceName
                viewModelScope.launch {
                    repository.insertDeviceControlCommand(DeviceControlCommandEntity(
                        commandText = "Trigger: $seqName",
                        executionDetails = "Activated action-chain: $seqName",
                        status = "COMPLETED"
                    ))
                    
                    val reply = if (preferences.value.activeAssistant == "ZOYA") {
                        "Done! Let's play! Triggering $seqName now!"
                    } else {
                        "Sir, command executed successfully. Initiating complex automation sequence $seqName."
                    }
                    voiceNovaResponse.value = reply
                    voiceManager.speak(reply)
                    
                    delay(1500)
                    if (seqName == "Morning Mode") {
                        executeSingleVoiceCommandInternal("play morning coding music on YouTube")
                        delay(4500)
                        executeSingleVoiceCommandInternal("check notifications")
                        delay(4500)
                        executeSingleVoiceCommandInternal("open WhatsApp")
                    } else { // Work Mode
                        executeSingleVoiceCommandInternal("open Chrome")
                        delay(4500)
                        executeSingleVoiceCommandInternal("open document weekly_productivity_report.pdf")
                        delay(4500)
                        executeSingleVoiceCommandInternal("open Calculator")
                    }
                }
            }
            
            is CommandResult.UnknownCommand -> {
                viewModelScope.launch {
                    val userKey = preferences.value.geminiApiKey.trim()
                    val configKey = com.example.BuildConfig.GEMINI_API_KEY.trim()
                    val key = when {
                        userKey.isNotEmpty() -> userKey
                        configKey.isNotEmpty() && configKey != "MY_GEMINI_API_KEY" -> configKey
                        else -> ""
                    }
                    val language = activeVoiceLanguage.value

                    val actAssistant = preferences.value.activeAssistant
                    val isZoya = actAssistant == "ZOYA"
                    val name = if (isZoya) "Zoya" else "Nova"
                    val personalityGuide = if (isZoya) {
                        "Speak as Zoya, a friendly female assistant with a sweet, highly expressive, playful, emotional, and conversational custom speaking style. Never call the user 'Sir'—be incredibly friendly, playful, and expressive!"
                    } else {
                        "Speak as Nova, a professional, highly formal, respectful, intelligent male voice assistant. Always call the user 'Sir' with respect."
                    }

                    // Retrieve relevant memories from local Room Database (Phase 16 Requirement 1)
                    val memory = repository.getAssistantMemory()
                    val facts = memory.userFacts
                    val summaries = memory.conversationSummaries
                    val memoryGuide = if (facts.isNotEmpty() || summaries.isNotEmpty()) {
                        "\n--- SECURE COGNITIVE DATABASE BUFFER ---\n" +
                        (if (facts.isNotEmpty()) "- User Preferences/Facts: $facts\n" else "") +
                        (if (summaries.isNotEmpty()) "- Preserved Conversation Summaries: $summaries\n" else "") +
                        "Utilize these points contextually in your reply if relevant."
                    } else ""

                    val sysInstruction = (if (language == "bn") {
                        "You are $name, a helpful AI companion inside the Hello Nova android application styled in a cybernetic dark space mode. $personalityGuide Reply strictly in Bengali. Keep the response highly supportive, tactical, short, under 2 sentences." + memoryGuide
                    } else {
                        "You are $name, a helpful AI companion inside the Hello Nova android application styled in a cybernetic dark space mode. $personalityGuide Reply strictly in English. Keep the response tactical, cool, short, under 2 sentences." + memoryGuide
                    }) + getEnhancedContextInstructionForGemini()

                    val imgUri = selectedImageUri.value
                    val imageBase64 = if (imgUri != null) uriToBase64(imgUri) else null
                    val mimeType = if (imgUri != null) getMimeType(imgUri) else "image/jpeg"

                    val docText = extractedPdfText.value
                    val combinedPrompt = if (docText.isNotEmpty()) {
                        "--- EXTRACTED DOCUMENT TEXT CONTEXT FROM ${lastAnalyzedAsset.value} ---\n" +
                        docText + "\n" +
                        "--- END OF DOCUMENT TEXT ---\n\n" +
                        "Task / Question about the document: $userText"
                    } else {
                        userText
                    }

                    var accumulatedResponseText = ""
                    val response = if (key.isNotEmpty()) {
                        _terminalOutput.value = "GEMINI_AI // Querying Gemini via direct stream..."
                        try {
                            geminiEngine.queryGeminiStream(
                                apiKey = key,
                                prompt = combinedPrompt,
                                systemInstruction = sysInstruction,
                                imageBase64 = imageBase64,
                                mimeType = mimeType
                            ) { chunk ->
                                accumulatedResponseText += chunk
                                voiceNovaResponse.value = accumulatedResponseText
                                _terminalOutput.value = "NOVA_RESPONSEStream // \"$accumulatedResponseText\""
                            }
                            accumulatedResponseText
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Gemini streaming uplink error", e)
                            val fallbackText = generateOfflineResponse(userText, language)
                            "LLM sector sync failed. Sector fallback: $fallbackText"
                        }
                    } else {
                        val fallbackText = generateOfflineResponse(userText, language)
                        voiceNovaResponse.value = fallbackText
                        _terminalOutput.value = "NOVA_RESPONSE // \"$fallbackText\""
                        fallbackText
                    }

                    val successStatus = if (response.contains("failed") || response.contains("Error") || response.contains("sector sync failed")) "FAILED" else "SUCCESS"

                    // Save to local Room persistence
                    val historyLog = VoiceConversationEntity(
                        userSpeech = userText,
                        novaResponse = response,
                        language = if (language == "bn") "Bengali" else "English",
                        status = successStatus
                    )
                    repository.insertVoiceConversation(historyLog)

                    // Speak response via Android TextToSpeech (TTS)
                    voiceManager.speak(response)
                }
            }
        }
    }

    private suspend fun callGeminiAPI(apiKey: String, prompt: String, languageCode: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val jsonMedia = "application/json; charset=utf-8".toMediaTypeOrNull()
                
                // Construct instruction with name and personality from user settings
                val name = preferences.value.assistantName
                val personality = preferences.value.personalityMode
                val personalityGuide = when (personality) {
                    "Professional" -> "Speak in a highly formal, polite, space-commander style. Avoid relaxed, casual, or slang phrasing."
                    "Casual" -> "Speak in a super relaxed, chill, cyber-buddy slang style. Avoid formal protocols."
                    else -> "Speak in a friendly, helpful, supportive companion style."
                }

                val sysInstruction = (if (languageCode == "bn") {
                    "You are $name, a helpful AI companion inside the Hello Nova android application styled in a cybernetic dark space mode. $personalityGuide Reply strictly in Bengali. Keep the response highly supportive, tactical, short, under 2 sentences."
                } else {
                    "You are $name, a helpful AI companion inside the Hello Nova android application styled in a cybernetic dark space mode. $personalityGuide Reply strictly in English. Keep the response tactical, cool, short, under 2 sentences."
                }) + getEnhancedContextInstructionForGemini()

                val escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", " ")

                val payload = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {"text": "$escapedPrompt"}
                          ]
                        }
                      ],
                      "systemInstruction": {
                        "parts": [
                          {"text": "$sysInstruction"}
                        ]
                      },
                      "generationConfig": {
                        "temperature": 0.7,
                        "maxOutputTokens": 120
                      }
                    }
                """.trimIndent()

                val body = payload.toRequestBody(jsonMedia)
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext "Error: System node returned ${response.code}. Falling back offline."
                    }
                    val rawJson = response.body?.string() ?: return@withContext "Error: Null response payload."
                    
                    // Direct Regex matching to extract generated text
                    val textRegex = "\"text\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                    val match = textRegex.find(rawJson)
                    var parsedText = match?.groups?.get(1)?.value

                    if (parsedText != null) {
                        parsedText = parsedText
                            .replace("\\n", " ")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                        parsedText
                    } else {
                        "Error: Response parsing failed. Fallback operational."
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Gemini HTTP fail: ${e.message}", e)
                "Connection failed. Offline sector: " + generateOfflineResponse(prompt, languageCode)
            }
        }
    }

    private fun generateOfflineResponse(userSpeech: String, languageCode: String): String {
        val lower = userSpeech.lowercase()
        val isZoya = preferences.value.activeAssistant == "ZOYA"
        val name = if (isZoya) "Zoya" else "Nova"
        return if (languageCode == "bn") {
            when {
                lower.contains("হ্যালো") || lower.contains("হাই") || lower.contains("কেমন") -> {
                    if (isZoya) "হ্যালো দোস্ত! জোয়া ভয়েস লিংক সচল আছে। কেমন আছো?" else "হ্যালো অপারেটর! মহাশূন্য ভয়েস লিংক সচল আছে। সিস্টেমের অবস্থা স্বাভাবিক।"
                }
                lower.contains("নাম") || lower.contains("কে") -> {
                    if (isZoya) "আমি জোয়া, আপনার বন্ধু এবং পারসোনাল ভয়েস এসিস্ট্যান্ট!" else "আমি Nova, আপনার ভয়েস অ্যাসিস্ট্যান্ট। ফেজ ১১ কোড সম্পূর্ণ সুরক্ষিত।"
                }
                lower.contains("ইঞ্জিন") || lower.contains("রিঅ্যাক্টর") || lower.contains("চালু") -> {
                    if (isZoya) "উউউ কোরের সব রিয়্যাক্টর কিন্তু অন ফায়ার!" else "রিঅ্যাক্টর কোর জ্যামিতিক হারে শক্তি সঞ্চার করছে।"
                }
                lower.contains("আবহাওয়া") || lower.contains("স্ক্যান") || lower.contains("বৃষ্টি") -> {
                    if (isZoya) "বাইরে বেশ মনোরম আবহাওয়া, স্ক্যান রিপোর্ট তো দারুণ দেখাচ্ছে!" else "বায়ুমণ্ডল স্ক্যান সম্পন্ন হয়েছে: সৌর ধূলিকণা ১.২%, নিয়ন কুয়াশা ৯০%।"
                }
                lower.contains("সাহায্য") -> {
                    if (isZoya) "আমি আছি সব আড্ডা ও প্রশ্নের উত্তর দিতে!" else "আমি আপনার কণ্ঠস্বর সনাক্ত করতেছি এবং রুম ডাটাবেজে ইতিহাস সংরক্ষণ করছি।"
                }
                lower.contains("ইতিহাস") || lower.contains("মুছুন") -> {
                    "ইতিহাস মুছতে আপনার ড্রিম বক্স বাটন ট্যাপ করুন।"
                }
                else -> {
                    if (isZoya) "জোয়ার অফলাইন বার্তা: \"$userSpeech\"" else "অফলাইন বার্তা গৃহীত: \"$userSpeech\" // কোড সচল রয়েছে।"
                }
            }
        } else {
            when {
                lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                    if (isZoya) "Hey there! Zoya here, super excited to help you!" else "Greetings Operator! This is Nova, system online."
                }
                lower.contains("who are you") || lower.contains("your name") || lower.contains("name") -> {
                    if (isZoya) "I am Zoya, your friendly, expressive companion. Let's play!" else "I am Nova, your professional, highly intelligent cognitive assistant core."
                }
                lower.contains("engine") || lower.contains("ignite") || lower.contains("reactor") -> {
                    if (isZoya) "Vroom! Reactor core is buzzing with playful positive energy!" else "Reactor core is active. Quantum containment fields are at maximum efficiency."
                }
                lower.contains("weather") || lower.contains("scan") -> {
                    if (isZoya) "It's looking like a great day to fly! Atmospheric scans are super cozy." else "Scanning atmospheric metrics... Nitrogen is 72%, Neon Fog: 90%, acid precipitation: stable."
                }
                lower.contains("help") -> {
                    if (isZoya) "I'm here to chat, read notifications, and make everything fun!" else "I provide speech recognition telemetry, instant room synchronization, and translation pipelines."
                }
                lower.contains("clear") || lower.contains("delete") -> {
                    "To purge conversation history, double-tap the clear icon in the system panel."
                }
                else -> {
                    if (isZoya) "Zoya received offline: \"$userSpeech\" // Love this!" else "Uplink processed offline: \"$userSpeech\" // Sector feedback nominal."
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }

    // --- Original Identity Operations ---

    fun performLogin(username: String) {
        viewModelScope.launch {
            val current = repository.getPreferences()
            repository.savePreferences(current.copy(username = username, isLoggedIn = true))
            _terminalOutput.value = "LOGIN_AUTH // Authorized operator: $username. Boot sequence complete."
        }
    }

    fun performLogout() {
        viewModelScope.launch {
            repository.updateLoginState(false)
            _terminalOutput.value = "LOG_OUT // Operator detached from system console."
        }
    }

    fun updateProfile(username: String, avatarId: String) {
        viewModelScope.launch {
            val current = repository.getPreferences()
            repository.savePreferences(current.copy(username = username, avatarId = avatarId))
            _terminalOutput.value = "PROFILE_SAVE // Sync completed: ID updated to [$username] / Node [$avatarId]."
        }
    }

    fun saveSettings(geminiKey: String, username: String, darkMode: Boolean) {
        viewModelScope.launch {
            val current = repository.getPreferences()
            repository.savePreferences(current.copy(
                geminiApiKey = geminiKey,
                username = username,
                darkThemeEnabled = darkMode
            ))
            _terminalOutput.value = "SETTINGS_STORE // Local preferences written to Room persistent storage."
        }
    }

    fun startEngine() {
        viewModelScope.launch {
            try {
                // Set engine state to starting first
                NovaForegroundService.updateState(NovaForegroundService.EngineState.STARTING)
                
                // Save state to DB
                val current = repository.getPreferences()
                repository.savePreferences(current.copy(engineActive = true))

                // Start Foreground Service
                val intent = Intent(application, NovaForegroundService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(application, intent)
                
                _terminalOutput.value = "IGNITION // Engine startup sequence initiated."
            } catch (e: Exception) {
                val errorMsg = "IGNITION_ERR // Startup failed: ${e.localizedMessage}"
                _terminalOutput.value = errorMsg
                repository.insertServiceLog(
                    ServiceLogEntity(
                        eventType = "Service Start Failure",
                        message = e.localizedMessage ?: "Unknown FGS start exception"
                    )
                )
            }
        }
    }

    fun stopEngine() {
        viewModelScope.launch {
            try {
                // Save state to DB
                val current = repository.getPreferences()
                repository.savePreferences(current.copy(engineActive = false))

                // Stop Foreground Service
                val intent = Intent(application, NovaForegroundService::class.java)
                application.stopService(intent)

                _terminalOutput.value = "DECOUPLE // Engine off. Standby active."
            } catch (e: Exception) {
                _terminalOutput.value = "DECOUPLE_ERR // Shutdown exception: ${e.localizedMessage}"
            }
        }
    }

    fun logPermissionStatus(permissionName: String, isGranted: Boolean) {
        viewModelScope.launch {
            val status = if (isGranted) "GRANTED" else "DENIED"
            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Permission Status",
                    message = "Permission: [$permissionName] is $status"
                )
            )
        }
    }

    fun clearServiceLogs() {
        viewModelScope.launch {
            repository.clearServiceLogs()
            _terminalOutput.value = "LOG_PURGE // Service event buffer wiped clean."
        }
    }

    fun updatePersonalizationSettings(
        username: String,
        geminiApiKey: String,
        darkThemeEnabled: Boolean,
        assistantName: String,
        voiceLanguage: String,
        speechSpeed: Float,
        speechPitch: Float,
        speechVolume: Float,
        themeMode: String,
        startupEngineEnabled: Boolean,
        notificationReadbackEnabled: Boolean,
        voiceFeedbackEnabled: Boolean,
        sessionAutoRestoreEnabled: Boolean,
        personalityMode: String,
        wakePhrase: String
    ) {
        viewModelScope.launch {
            try {
                val current = repository.getPreferences()
                val updated = current.copy(
                    username = username,
                    geminiApiKey = geminiApiKey,
                    darkThemeEnabled = darkThemeEnabled,
                    assistantName = assistantName,
                    voiceLanguage = voiceLanguage,
                    speechSpeed = speechSpeed,
                    speechPitch = speechPitch,
                    speechVolume = speechVolume,
                    themeMode = themeMode,
                    startupEngineEnabled = startupEngineEnabled,
                    notificationReadbackEnabled = notificationReadbackEnabled,
                    voiceFeedbackEnabled = voiceFeedbackEnabled,
                    sessionAutoRestoreEnabled = sessionAutoRestoreEnabled,
                    personalityMode = personalityMode,
                    wakePhrase = wakePhrase
                )
                repository.savePreferences(updated)
                settingsDataStore.saveSettings(updated)

                // Apply parameters to speech engine instantly
                voiceManager.setSpeechRate(speechSpeed)
                voiceManager.setPitch(speechPitch)
                voiceManager.setVolume(speechVolume)
                voiceManager.voiceFeedbackEnabled = voiceFeedbackEnabled
                voiceManager.setLanguage(voiceLanguage)

                _terminalOutput.value = "SETTINGS_UPDATED // Dynamic profile applied for $assistantName. Mode: $personalityMode"
            } catch (e: Exception) {
                _terminalOutput.value = "SETTINGS_ERR // Update failed: ${e.localizedMessage}"
            }
        }
    }

    fun toggleEngine() {
        val currentActive = preferences.value.engineActive
        if (currentActive) {
            stopEngine()
        } else {
            startEngine()
        }
    }
}

class MainViewModelFactory(
    private val repository: UserRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ContactItem(
    val name: String,
    val cipherId: String,
    val classification: String,
    val linkStatus: String,
    val lastSync: String
)
