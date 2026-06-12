package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.UserRepository
import com.example.data.VoiceSessionEntity
import com.example.data.ServiceLogEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceSessionManager(
    private val context: Context,
    private val repository: UserRepository,
    private val scope: CoroutineScope
) {
    private val TAG = "VoiceSessionManager"

    // Live binding for Text-to-Speech Engine diagnostics
    var isTtsEngineReady: () -> Boolean = { false }

    // --- Wake Event Manager Instance (Phase 8 Requirement 1 & 3) ---
    val wakeEventManager = WakeEventManager(context, repository, scope)

    // --- Core Assistant State (Requirement 2 & 3) ---
    private val _currentState = MutableStateFlow(AssistantState.STANDBY)
    val currentState = _currentState.asStateFlow()

    // --- Voice Session Statistics State (Requirement 4 & 6) ---
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive = _isSessionActive.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(0L)
    val sessionStartTime = _sessionStartTime.asStateFlow()

    private val _sessionEndTime = MutableStateFlow(0L)
    val sessionEndTime = _sessionEndTime.asStateFlow()

    private val _commandCount = MutableStateFlow(0)
    val commandCount = _commandCount.asStateFlow()

    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds = _sessionDurationSeconds.asStateFlow()

    // --- Session Recovery Fields (Requirement 7) ---
    private val _lastSessionStartTime = MutableStateFlow(0L)
    val lastSessionStartTime = _lastSessionStartTime.asStateFlow()

    private val _lastSessionEndTime = MutableStateFlow(0L)
    val lastSessionEndTime = _lastSessionEndTime.asStateFlow()

    private val _lastSessionCommandCount = MutableStateFlow(0)
    val lastSessionCommandCount = _lastSessionCommandCount.asStateFlow()

    private val _lastSessionDurationMs = MutableStateFlow(0L)
    val lastSessionDurationMs = _lastSessionDurationMs.asStateFlow()

    // --- Wake Session Prep & Modular Hooks ---
    private val _wakeWordVerified = MutableStateFlow(false) 
    val wakeWordVerified = _wakeWordVerified.asStateFlow()

    private val _isContinuousListeningHookEnabled = MutableStateFlow(false)
    val isContinuousListeningHookEnabled = _isContinuousListeningHookEnabled.asStateFlow()

    private val _wakePhraseStatus = MutableStateFlow("STANDBY") // STANDBY, ARMED, TRIGGERED
    val wakePhraseStatus = _wakePhraseStatus.asStateFlow()

    // --- Service Monitoring (Requirement 5 & 8) ---
    private val _foregroundServiceStatus = MutableStateFlow("UNKNOWN")
    val foregroundServiceStatus = _foregroundServiceStatus.asStateFlow()

    private val _voiceEngineStatus = MutableStateFlow("UNKNOWN")
    val voiceEngineStatus = _voiceEngineStatus.asStateFlow()

    private val _notificationListenerStatus = MutableStateFlow("UNKNOWN")
    val notificationListenerStatus = _notificationListenerStatus.asStateFlow()

    private val _permissionReadinessStatus = MutableStateFlow("UNKNOWN")
    val permissionReadinessStatus = _permissionReadinessStatus.asStateFlow()

    private val _databaseStatus = MutableStateFlow("UNKNOWN")
    val databaseStatus = _databaseStatus.asStateFlow()

    private var durationLoggerJob: Job? = null

    init {
        // Restore previous engine state and last session information (Requirement 7)
        scope.launch {
            try {
                val prefs = repository.getPreferences()
                if (prefs.engineActive) {
                    _currentState.value = AssistantState.ARMED
                    _wakePhraseStatus.value = "ARMED"
                } else {
                    _currentState.value = AssistantState.STANDBY
                    _wakePhraseStatus.value = "STANDBY"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore engine state", e)
                _currentState.value = AssistantState.STANDBY
            }

            // Observe and recover last session record
            repository.allSessionsFlow.collect { sessions ->
                if (sessions.isNotEmpty()) {
                    val last = sessions.first()
                    _lastSessionStartTime.value = last.startTime
                    _lastSessionEndTime.value = last.endTime
                    _lastSessionCommandCount.value = last.commandCount
                    _lastSessionDurationMs.value = last.durationMs
                }
            }
        }

        observeServicesHealth()
    }

    /**
     * Start Assistant Presence Session (Requirement 1, 3, 6, 7)
     */
    fun startSession(sessionType: String = "MANUAL") {
        if (_isSessionActive.value) {
            Log.d(TAG, "startSession: Session already running.")
            return
        }

        _isSessionActive.value = true
        val startVal = System.currentTimeMillis()
        _sessionStartTime.value = startVal
        _sessionEndTime.value = 0L
        _commandCount.value = 0
        _sessionDurationSeconds.value = 0L

        // Transition assistant state to LISTENING
        _currentState.value = AssistantState.LISTENING

        scope.launch {
            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Voice Session Start",
                    message = "SESSION_START // Managed uplink initiated via $sessionType trigger."
                )
            )
        }

        // Start real-time live duration counter
        startDurationTimer()
    }

    /**
     * End Assistant Presence Session and persist (Requirement 6)
     */
    fun endSession() {
        if (!_isSessionActive.value) {
            Log.d(TAG, "endSession: No active session to end.")
            return
        }

        stopDurationTimer()
        _isSessionActive.value = false
        val endVal = System.currentTimeMillis()
        _sessionEndTime.value = endVal

        val startVal = _sessionStartTime.value
        val finalDurationMs = endVal - startVal
        val finalCount = _commandCount.value

        // Transition assistant back to ARMED state
        _currentState.value = AssistantState.ARMED

        scope.launch {
            // Save to database
            val sessionRecord = VoiceSessionEntity(
                startTime = startVal,
                endTime = endVal,
                commandCount = finalCount,
                durationMs = finalDurationMs,
                sessionType = "MANUAL"
            )
            repository.insertVoiceSession(sessionRecord)

            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Voice Session End",
                    message = "SESSION_END // Saved record. Captured $finalCount commands, Duration: ${finalDurationMs / 1000}s"
                )
            )
        }
    }

    /**
     * Increment the count of voice commands handled in this session
     */
    fun registerCommandHandled() {
        if (_isSessionActive.value) {
            _commandCount.value += 1
        }
    }

    /**
     * Manage Transitions between states
     */
    fun transitionTo(state: AssistantState) {
        _currentState.value = state
        // If state changed to Standby or Armed, save the preference for recovery (Requirement 7)
        if (state == AssistantState.STANDBY || state == AssistantState.ARMED) {
            scope.launch {
                repository.updateEngineState(state == AssistantState.ARMED)
            }
        }
    }

    /**
     * Start/Stop duration ticking
     */
    private fun startDurationTimer() {
        durationLoggerJob?.cancel()
        durationLoggerJob = scope.launch {
            while (isActive && _isSessionActive.value) {
                delay(1000)
                val diffMs = System.currentTimeMillis() - _sessionStartTime.value
                _sessionDurationSeconds.value = diffMs / 1000
            }
        }
    }

    private fun stopDurationTimer() {
        durationLoggerJob?.cancel()
        durationLoggerJob = null
    }

    // --- Wake Session Prep & Modular Hooks ---

    fun toggleContinuousListeningHook(enabled: Boolean) {
        _isContinuousListeningHookEnabled.value = enabled
        _wakePhraseStatus.value = if (enabled) "ARMED" else "STANDBY"
        
        val targetState = if (enabled) AssistantState.ARMED else AssistantState.STANDBY
        transitionTo(targetState)

        scope.launch {
            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Wake Session Hook Toggle",
                    message = "WAKE_PREP // Future continuous wake word hook: " + (if (enabled) "ARMED" else "STANDBY")
                )
            )
        }
    }

    fun triggerWakePhraseDetectionMock() {
        scope.launch {
            _wakePhraseStatus.value = "TRIGGERED"
            _wakeWordVerified.value = true

            // Record wake event in DB
            wakeEventManager.recordWakeEvent("Hello Nova Wake Phrase", "VERIFIED")

            // Auto start session upon simulation trigger
            startSession("WAKE_PHRASE")
            transitionTo(AssistantState.LISTENING)

            delay(1505)
            _wakePhraseStatus.value = if (_isContinuousListeningHookEnabled.value) "ARMED" else "STANDBY"
            _wakeWordVerified.value = false
        }
    }

    // --- Service/Environment Monitoring (Requirement 5 & 8) ---

    fun observeServicesHealth() {
        scope.launch {
            while (isActive) {
                // Check Foreground service health
                val fgRunning = NovaForegroundService.engineState.value != NovaForegroundService.EngineState.OFFLINE
                _foregroundServiceStatus.value = if (fgRunning) "HEALTHY" else "INACTIVE"

                // Check Notification Listener Service helper
                val listenerAlive = NovaNotificationListenerService.isServiceConnected()
                _notificationListenerStatus.value = if (listenerAlive) "CONNECTED" else "UNAUTHORIZED"

                // Check Speech synthesizer readiness
                _voiceEngineStatus.value = if (isTtsEngineReady()) "HEALTHY" else "INITIALIZING"

                // Check Database Status
                try {
                    repository.getPreferences()
                    _databaseStatus.value = "HEALTHY"
                } catch (e: Exception) {
                    _databaseStatus.value = "ERROR"
                }

                // Check Permission Readiness Status
                val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                val notifGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else true

                _permissionReadinessStatus.value = if (audioGranted && notifGranted) {
                    "HEALTHY"
                } else if (audioGranted) {
                    "PARTIAL"
                } else {
                    "DENIED"
                }

                delay(2000)
            }
        }
    }

    // --- Testing & Simulation Utilities (Requirement 9) ---

    fun resetSessionStatistics() {
        scope.launch {
            // Reset Session timer and active stats
            stopDurationTimer()
            _isSessionActive.value = false
            _sessionStartTime.value = 0L
            _sessionEndTime.value = 0L
            _commandCount.value = 0
            _sessionDurationSeconds.value = 0L

            _lastSessionStartTime.value = 0L
            _lastSessionEndTime.value = 0L
            _lastSessionCommandCount.value = 0
            _lastSessionDurationMs.value = 0L

            // Clear wake event records and session persistent logs
            repository.clearVoiceSessions()
            repository.clearWakeEvents()
            
            // Reset engine start time (uptime)
            wakeEventManager.resetUptime()

            repository.insertServiceLog(
                ServiceLogEntity(
                    eventType = "Stats Reset",
                    message = "DIAGNOSTICS // Local session statistics and log database wiped."
                )
            )
        }
    }

    /**
     * Runs an aesthetic, step-by-step automatic audio lifecycle simulation
     */
    fun runSessionSimulation(voiceManager: VoiceManager, onTerminalLog: (String) -> Unit) {
        scope.launch {
            onTerminalLog("SIM_START // Initiating Assistant Presence simulation sequence.")
            
            // 1. Start Session
            startSession("SIMULATION")
            onTerminalLog("SIM_LOG // Session initialized. State: LISTENING")
            transitionTo(AssistantState.LISTENING)
            delay(1000)

            // 2. Continuous listening simulated trigger
            onTerminalLog("SIM_LOG // Wake Phrase match verification triggered.")
            _wakePhraseStatus.value = "TRIGGERED"
            _wakeWordVerified.value = true
            
            // Record simulated wake event in DB (Requirement 9)
            wakeEventManager.recordWakeEvent("Hello Nova Simulation Phrase", "VERIFIED")

            delay(1000)
            _wakePhraseStatus.value = if (_isContinuousListeningHookEnabled.value) "ARMED" else "STANDBY"
            _wakeWordVerified.value = false
            onTerminalLog("SIM_LOG // State transit: LISTENING (Simulating audio recording buffer)")
            delay(1000)

            // 3. Simulated user spoken input
            registerCommandHandled()
            onTerminalLog("SIM_INPUT // Captured audio transcript: \"system quantum status check\"")
            
            // 4. Processing stage
            onTerminalLog("SIM_LOG // State transit: PROCESSING (Sending context matrix to Nova LLM Core)")
            transitionTo(AssistantState.PROCESSING)
            delay(1500)

            // 5. Speaking response
            val response = "Simulation confirmed. All quantum cognitive subsystems are operating at nominal capacity."
            onTerminalLog("SIM_LOG // State transit: SPEAKING (Broadcasting voice response via TextToSpeech)")
            transitionTo(AssistantState.SPEAKING)
            voiceManager.speak(response)
            delay(3000)

            // 6. Complete and clean end
            onTerminalLog("SIM_LOG // Transition back to ARMED")
            transitionTo(AssistantState.ARMED)
            delay(500)
            
            endSession()
            onTerminalLog("SIM_END // Simulation sequence finalized and saved to Room Database.")
        }
    }
}
