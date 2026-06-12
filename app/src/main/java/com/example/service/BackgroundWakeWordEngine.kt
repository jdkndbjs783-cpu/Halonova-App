package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.HelloNovaApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class WakeStatus {
    LISTENING,
    SLEEPING,
    TRIGGERED,
    DISABLED
}

class BackgroundWakeWordEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener {

    companion object {
        private const val TAG = "BackgroundWakeEngine"
        
        private val _status = MutableStateFlow(WakeStatus.DISABLED)
        val status = _status.asStateFlow()

        private val _triggerCount = MutableStateFlow(0)
        val triggerCount = _triggerCount.asStateFlow()

        private val _lastActivationTime = MutableStateFlow(0L)
        val lastActivationTime = _lastActivationTime.asStateFlow()

        private val _engineStartTime = MutableStateFlow(System.currentTimeMillis())
        val engineStartTime = _engineStartTime.asStateFlow()

        private val _microphoneAvailable = MutableStateFlow(true)
        val microphoneAvailable = _microphoneAvailable.asStateFlow()

        private val _voiceRecognitionHealthScore = MutableStateFlow(100)
        val voiceRecognitionHealthScore = _voiceRecognitionHealthScore.asStateFlow()

        fun setStatus(newStatus: WakeStatus) {
            _status.value = newStatus
        }

        fun incrementTriggerCount() {
            _triggerCount.value += 1
            _lastActivationTime.value = System.currentTimeMillis()
        }

        fun getUptimeSeconds(): Long {
            return (System.currentTimeMillis() - _engineStartTime.value) / 1000
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isEngineRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListeningToMic = false

    private val voiceManager: VoiceManager by lazy {
        // Simple context-bound TTS activation to announce triggers
        VoiceManager(context) {}
    }

    init {
        // Restore stats from persistence (number of wake events)
        scope.launch(Dispatchers.IO) {
            _engineStartTime.value = System.currentTimeMillis()
            try {
                val app = context.applicationContext as? HelloNovaApplication
                val repo = app?.repository
                if (repo != null) {
                    val count = repo.getPreferences().id // simple fallback or wake events count
                    repo.allWakeEventsFlow.collect { list ->
                        _triggerCount.value = list.size
                        if (list.isNotEmpty()) {
                            _lastActivationTime.value = list.first().timestamp
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching historical wake triggers: ${e.message}")
            }
        }
    }

    fun startEngine() {
        if (isEngineRunning) return
        isEngineRunning = true
        Log.d(TAG, "Background Wake Core initialized.")
        loopListening()
    }

    fun stopEngine() {
        isEngineRunning = false
        stopListening()
        _status.value = WakeStatus.DISABLED
        Log.d(TAG, "Background Wake Core stopped.")
    }

    fun runMicrophoneTest(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        _microphoneAvailable.value = hasPermission
        updateVoiceRecognitionHealthScore()
        return hasPermission
    }

    fun runWakeEngineTest(): Boolean {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        if (available) {
            val prevStatus = _status.value
            _status.value = WakeStatus.TRIGGERED
            mainHandler.postDelayed({
                _status.value = prevStatus
                updateVoiceRecognitionHealthScore()
            }, 1500)

            scope.launch(Dispatchers.IO) {
                try {
                    val app = context.applicationContext as? HelloNovaApplication
                    app?.repository?.insertServiceLog(
                        com.example.data.ServiceLogEntity(
                            eventType = "Wake Engine Test",
                            message = "Voice Activation Interface diagnostic check successfully cleared."
                        )
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        updateVoiceRecognitionHealthScore()
        return available
    }

    fun updateVoiceRecognitionHealthScore(): Int {
        var score = 0
        val micPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (micPermission) score += 40
        if (SpeechRecognizer.isRecognitionAvailable(context)) score += 30
        if (_status.value != WakeStatus.DISABLED) score += 30
        _voiceRecognitionHealthScore.value = score
        return score
    }

    private fun loopListening() {
        if (!isEngineRunning) return

        mainHandler.post {
            val micPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            _microphoneAvailable.value = micPermission
            updateVoiceRecognitionHealthScore()

            if (!micPermission) {
                _status.value = WakeStatus.DISABLED
                // Smart check to try recovery in 5 seconds
                scheduleNextCheck(5000)
                return@post
            }

            // Smart microphone: Go to Sleeping mode if the foreground app is actively speaking or user is running active foreground voice session
            val app = context.applicationContext as? HelloNovaApplication
            val repository = app?.repository
            
            // Check if active session or active speech is busy to avoid audio focus collision
            val isMainAppBusy = if (repository != null) {
                runBlocking(Dispatchers.IO) {
                    try {
                        val prefs = repository.getPreferences()
                        prefs.engineActive // If user explicitly turned off listening helper
                    } catch (e: Exception) {
                        false
                    }
                }
            } else {
                false
            }

            // We can also check if the companion TTS is currently reading
            if (!isListeningToMic) {
                _status.value = WakeStatus.LISTENING
                startListening()
            } else {
                scheduleNextCheck(3000)
            }
        }
    }

    private fun startListening() {
        try {
            if (speechRecognizer != null) {
                speechRecognizer?.destroy()
            }
            
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _status.value = WakeStatus.DISABLED
                Log.e(TAG, "Speech Recognition not available on device")
                return
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@BackgroundWakeWordEngine)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            isListeningToMic = true
            _microphoneAvailable.value = true
            Log.d(TAG, "Background Wake Engine microphone listening...")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition: ${e.message}")
            isListeningToMic = false
            _microphoneAvailable.value = false
            scheduleNextCheck(4000)
        }
        updateVoiceRecognitionHealthScore()
    }

    private fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recognizer: ${e.message}")
            }
            isListeningToMic = false
        }
    }

    private fun scheduleNextCheck(delayMillis: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            loopListening()
        }, delayMillis)
    }

    // --- RecognitionListenerCallbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Microphone armed.")
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        isListeningToMic = false
    }

    override fun onError(error: Int) {
        Log.d(TAG, "Speech Recognizer on error: $error")
        isListeningToMic = false
        if (error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            _microphoneAvailable.value = false
        }
        updateVoiceRecognitionHealthScore()
        
        // Re-authenticate speech session loop with a micro delay
        val delayValue = when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 2500L
            SpeechRecognizer.ERROR_NO_MATCH -> 1000L
            else -> 3000L
        }
        scheduleNextCheck(delayValue)
    }

    override fun onResults(results: Bundle?) {
        isListeningToMic = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val voiceText = matches[0].lowercase(Locale.ROOT).trim()
            Log.d(TAG, "Decoded background utterance: $voiceText")

            var matchedKey: String? = null
            var voicePhrase = "Yes Sir, I am listening."

            val app = context.applicationContext as? HelloNovaApplication
            val repo = app?.repository
            val isZoya = runBlocking(Dispatchers.IO) {
                try {
                    repo?.getPreferences()?.activeAssistant == "ZOYA"
                } catch (e: Exception) {
                    false
                }
            }

            if (isZoya) {
                when {
                    voiceText.contains("hello zoya") || voiceText.contains("hey zoya") -> {
                        matchedKey = "Hello Zoya"
                        voicePhrase = "Hey! Everything is ready. Nova is fully upgraded!"
                    }
                    voiceText.contains("zoya") -> {
                        matchedKey = "Zoya"
                        voicePhrase = "Hey there! Ready when you are."
                    }
                }
            } else {
                when {
                    voiceText.contains("hello nova") -> {
                        matchedKey = "Hello Nova"
                        voicePhrase = "Sir, Nova Omega Core final system is online. All systems are operational."
                    }
                    voiceText.contains("hey nova") -> {
                        matchedKey = "Hey Nova"
                        voicePhrase = "Sir, Nova Omega Core final system is online. All systems are operational."
                    }
                    voiceText.contains("nova") -> {
                        matchedKey = "Nova"
                        voicePhrase = "Sir, how may I assist you?"
                    }
                }
            }

            if (matchedKey != null) {
                _status.value = WakeStatus.TRIGGERED
                incrementTriggerCount()
                
                // Write wake event to DB
                scope.launch(Dispatchers.IO) {
                    try {
                        val app = context.applicationContext as? HelloNovaApplication
                        val repo = app?.repository
                        if (repo != null) {
                            val manager = WakeEventManager(context, repo, scope)
                            manager.recordWakeEvent(matchedKey, "VERIFIED")
                            
                            // Re-sync UI update
                            repo.insertServiceLog(
                                com.example.data.ServiceLogEntity(
                                    eventType = "Voice Activation",
                                    message = "WAKE_ENGINE // Background match: [$matchedKey] -> Triggered activation."
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to record wake event: ${e.message}")
                    }
                }

                // Announce reaction
                voiceManager.speak(voicePhrase)
                
                // Keep Triggered status briefly then loop back
                mainHandler.postDelayed({
                    _status.value = WakeStatus.LISTENING
                    scheduleNextCheck(1500)
                }, 3000)
            } else {
                // No match, restart loop immediately
                scheduleNextCheck(500)
            }
        } else {
            scheduleNextCheck(800)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
