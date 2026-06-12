package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onResultCallback: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    val isListening = MutableStateFlow(false)
    val recognizedText = MutableStateFlow("")
    val errorState = MutableStateFlow<String?>(null)
    val isTtsReady = MutableStateFlow(false)

    // Supports "en" (English) and "bn" (Bengali)
    val activeLanguageCode = MutableStateFlow("en")
    val activeAssistant = MutableStateFlow("NOVA")

    var onSpeakStateChanged: ((Boolean) -> Unit)? = null
    var onRecoveryTriggered: (() -> Unit)? = null
    private var consecutiveErrorsCount = 0

    init {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }

    fun initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceManager)
                }
            } else {
                errorState.value = "Speech recognition service is not available on this emulator or device."
            }
        } catch (e: Exception) {
            errorState.value = "STT Init Error: ${e.localizedMessage}"
            Log.e("VoiceManager", "Error initializing speech recognizer: ${e.message}")
        }
    }

    private fun initializeTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            isTtsReady.value = false
            errorState.value = "TTS Daemon Constructor Exception: ${e.localizedMessage}"
            Log.e("VoiceManager", "Error initializing TTS: ${e.message}", e)
        }
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager

    fun requestTransientAudioFocus(): Boolean {
        return try {
            val result = audioManager?.requestAudioFocus(
                null,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
            result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (e: Exception) {
            Log.w("VoiceManager", "Audio focus request failed: ${e.message}")
            false
        }
    }

    fun abandonAudioFocus() {
        try {
            audioManager?.abandonAudioFocus(null)
        } catch (e: Exception) {
            Log.w("VoiceManager", "Audio focus abandon failed: ${e.message}")
        }
    }

    fun startListening() {
        if (isListening.value) {
            Log.d("VoiceManager", "Duplicate session check: SpeechRecognizer is already active. Ignoring.")
            return
        }

        // Interruption handling: Immediately stop any reading TTS playback when listening begins
        stopSpeaking()

        errorState.value = null
        recognizedText.value = ""

        // Check RECORD_AUDIO permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            errorState.value = "Microphone access denied. Please grant audio recording permissions."
            isListening.value = false
            return
        }

        requestTransientAudioFocus()

        // Clean up any stale recognizer to prevent ERROR_RECOGNIZER_BUSY or client freeze issues
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("VoiceManager", "Error clearing stale recognizer: ${e.message}")
        }
        speechRecognizer = null
        
        initializeSpeechRecognizer()
        
        if (speechRecognizer == null) {
            errorState.value = "Speech recognition engine unavailable on this device configuration."
            isListening.value = false
            abandonAudioFocus()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langTag = if (activeLanguageCode.value == "bn") "bn-BD" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            isListening.value = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            errorState.value = "Error starting capture: ${e.localizedMessage}"
            isListening.value = false
            abandonAudioFocus()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping listening: ${e.message}")
        }
        isListening.value = false
        abandonAudioFocus()
    }

    var voiceFeedbackEnabled: Boolean = true
    var voiceVolume: Float = 1.0f

    fun setVolume(volume: Float) {
        voiceVolume = volume.coerceIn(0.0f, 1.0f)
    }

    fun speak(text: String) {
        if (!voiceFeedbackEnabled) {
            Log.d("VoiceManager", "Voice feedback disabled. Speak suppressed: $text")
            return
        }
        val tts = textToSpeech
        if (tts == null) {
            Log.e("VoiceManager", "TTS engine is null and uninitialized")
            errorState.value = "TTS platform instance is not set up."
            return
        }
        if (!isTtsReady.value) {
            Log.w("VoiceManager", "TTS engine is currently offline or loading. Attempting to restart on demand.")
            initializeTextToSpeech()
            return
        }

        requestTransientAudioFocus()
        
        try {
            val locale = if (activeLanguageCode.value == "bn") {
                Locale("bn", "BD")
            } else {
                Locale.US
            }
            val res = tts.setLanguage(locale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Try alternate simple tag
                val alt = if (activeLanguageCode.value == "bn") Locale("bn") else Locale.ENGLISH
                val altRes = tts.setLanguage(alt)
                if (altRes == TextToSpeech.LANG_MISSING_DATA || altRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault())
                }
            }
            
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume)
            }
            if (activeAssistant.value.equals("ZOYA", ignoreCase = true)) {
                tts.setPitch(1.30f)
                tts.setSpeechRate(1.15f)
            } else {
                tts.setPitch(0.85f)
                tts.setSpeechRate(1.00f)
            }
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "NovaResponse")
        } catch (e: Exception) {
            Log.e("VoiceManager", "TTS speak failed: ${e.message}", e)
            abandonAudioFocus()
        }
    }

    fun setLanguage(lang: String) {
        if (lang == "bn" || lang == "en") {
            activeLanguageCode.value = lang
            val locale = if (lang == "bn") Locale("bn", "BD") else Locale.US
            textToSpeech?.setLanguage(locale)
        }
    }

    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            onSpeakStateChanged?.invoke(false)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping speech: ${e.message}")
        } finally {
            abandonAudioFocus()
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Cleanup failed: ${e.message}")
        }
    }

    // --- SpeechListener callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        isListening.value = true
        recognizedText.value = "Listening closely..."
        consecutiveErrorsCount = 0
    }

    override fun onBeginningOfSpeech() {
        recognizedText.value = "Processing acoustics..."
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        isListening.value = false
    }

    override fun onError(error: Int) {
        isListening.value = false
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check mic."
            SpeechRecognizer.ERROR_CLIENT -> "Client error. Verify Google App updates."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions denied on system level."
            SpeechRecognizer.ERROR_NETWORK -> "Network error occurred."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try talking clearer."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "STT daemon is busy. Retrying..."
            SpeechRecognizer.ERROR_SERVER -> "Server disconnected."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timed out waiting for voice input."
            else -> "Speech Recognition Error (code $error)"
        }
        errorState.value = message
        recognizedText.value = ""
        
        consecutiveErrorsCount++
        Log.w("VoiceManager", "Speech recognition error: $message (consecutive count: $consecutiveErrorsCount)")
        if (consecutiveErrorsCount >= 3 || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            Log.e("VoiceManager", "Consecutive errors threshold reached. Triggering voice engine recovery protocol...")
            consecutiveErrorsCount = 0
            onRecoveryTriggered?.invoke()
        }
    }

    override fun onResults(results: Bundle?) {
        isListening.value = false
        consecutiveErrorsCount = 0
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            recognizedText.value = text
            onResultCallback(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        consecutiveErrorsCount = 0
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            recognizedText.value = matches[0]
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- TTS Init Callback ---

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady.value = true
            try {
                val locale = if (activeLanguageCode.value == "bn") Locale("bn", "BD") else Locale.US
                val res = textToSpeech?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val alt = if (activeLanguageCode.value == "bn") Locale("bn") else Locale.ENGLISH
                    val altRes = textToSpeech?.setLanguage(alt) ?: TextToSpeech.LANG_NOT_SUPPORTED
                    if (altRes == TextToSpeech.LANG_MISSING_DATA || altRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.setLanguage(Locale.getDefault())
                        errorState.value = "Target voice pack unsupported. Using system default locale."
                    }
                }
            } catch (e: Exception) {
                Log.e("VoiceManager", "Error checking languages in onInit: ${e.message}", e)
            }

            try {
                textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakStateChanged?.invoke(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakStateChanged?.invoke(false)
                        abandonAudioFocus()
                    }

                    override fun onError(utteranceId: String?) {
                        onSpeakStateChanged?.invoke(false)
                        abandonAudioFocus()
                    }
                })
            } catch (e: Exception) {
                Log.e("VoiceManager", "Error registry progress listener in onInit: ${e.message}", e)
            }
        } else {
            isTtsReady.value = false
            errorState.value = "TTS Initialization error (Status code: $status)"
            Log.e("VoiceManager", "Error initializing TTS engine status: $status")
        }
    }
}
