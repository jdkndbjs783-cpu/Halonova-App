package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.util.UUID

class GeminiEngine(private val context: Context) {
    private val TAG = "GeminiEngine"

    // --- State & Diagnostics (Phase 15 Requirement 6) ---
    private val _connectionStatus = MutableStateFlow("UNKNOWN")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _activeSessionInfo = MutableStateFlow("Uninitialized (0 turns)")
    val activeSessionInfo = _activeSessionInfo.asStateFlow()

    private val _lastResponseTimeMs = MutableStateFlow(0L)
    val lastResponseTimeMs = _lastResponseTimeMs.asStateFlow()

    private val _modelStatus = MutableStateFlow("gemini-3.5-flash-active")
    val modelStatus = _modelStatus.asStateFlow()

    // --- Vision & Multimodal Diagnostics (Phase 17 Requirement 4) ---
    private val _visionEngineStatus = MutableStateFlow("ONLINE")
    val visionEngineStatus = _visionEngineStatus.asStateFlow()

    private val _lastAnalyzedAsset = MutableStateFlow("None")
    val lastAnalyzedAsset = _lastAnalyzedAsset.asStateFlow()

    private val _analysisTimeMs = MutableStateFlow(0L)
    val analysisTimeMs = _analysisTimeMs.asStateFlow()

    private val _multimodalSessionStatus = MutableStateFlow("IDLE")
    val multimodalSessionStatus = _multimodalSessionStatus.asStateFlow()

    fun updateVisionStatus(status: String) {
        _visionEngineStatus.value = status
    }

    fun updateLastAnalyzedAsset(asset: String) {
        _lastAnalyzedAsset.value = asset
    }

    fun updateAnalysisTime(timeMs: Long) {
        _analysisTimeMs.value = timeMs
    }

    fun updateMultimodalSessionStatus(status: String) {
        _multimodalSessionStatus.value = status
    }

    // Sliding window of conversation turns (Requirement 1 & 3)
    data class Turn(val role: String, val text: String)
    private val conversationHistory = mutableListOf<Turn>()
    private val maxHistoryTurns = 12 // Keeps the conversation context within 12 turns (6 users, 6 models) for token safety
    private var sessionId = UUID.randomUUID().toString().take(8)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Regex to match double-quoted strings with potential escapes perfectly
    private val textRegex = "\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()

    init {
        updateSessionState()
        checkInitialConnectivity()
    }

    private fun checkInitialConnectivity() {
        _connectionStatus.value = if (isNetworkAvailable()) "HEALTHY" else "OFFLINE"
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking network state: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking network state: ${e.message}")
            false
        }
    }

    fun clearSession() {
        synchronized(conversationHistory) {
            conversationHistory.clear()
            sessionId = UUID.randomUUID().toString().take(8)
            updateSessionState()
        }
    }

    private fun updateSessionState() {
        val count = conversationHistory.size
        _activeSessionInfo.value = "Session-$sessionId ($count turns)"
    }

    /**
     * Executes a streamed multi-turn query to Gemini supporting Multimodal Assets (Requirement 1, 2, 3)
     */
    suspend fun queryGeminiStream(
        apiKey: String,
        prompt: String,
        systemInstruction: String,
        imageBase64: String? = null,
        mimeType: String = "image/jpeg",
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        _connectionStatus.value = "CONNECTING"
        if (imageBase64 != null) {
            _visionEngineStatus.value = "ANALYZING"
        }

        if (!isNetworkAvailable()) {
            _connectionStatus.value = "OFFLINE"
            if (imageBase64 != null) {
                _visionEngineStatus.value = "ERROR"
            }
            throw Exception("Local device neural receptor shows zero network link.")
        }

        // 1. Context preservation sliding window (Requirement 3)
        val currentPrompt = prompt.trim()
        val payloadHistory = synchronized(conversationHistory) {
            conversationHistory.add(Turn("user", currentPrompt))
            // Enforce sliding window size class
            while (conversationHistory.size > maxHistoryTurns) {
                conversationHistory.removeAt(0)
            }
            updateSessionState()
            ArrayList(conversationHistory)
        }

        // 2. Format request payload (Direct REST model) with Multimodal support
        val historyJson = payloadHistory.mapIndexed { index, turn ->
            val cleanRole = turn.role
            val escapedText = escapeJsonString(turn.text)
            if (index == payloadHistory.lastIndex && imageBase64 != null && cleanRole == "user") {
                """{"role": "$cleanRole", "parts": [{"text": "$escapedText"}, {"inlineData": {"mimeType": "$mimeType", "data": "$imageBase64"}}]}"""
            } else {
                """{"role": "$cleanRole", "parts": [{"text": "$escapedText"}]}"""
            }
        }.joinToString(separator = ",")

        val escapedSysInstruction = escapeJsonString(systemInstruction)
        val payload = """
            {
              "contents": [ $historyJson ],
              "systemInstruction": {
                "parts": [{"text": "$escapedSysInstruction"}]
              },
              "generationConfig": {
                "temperature": 0.75,
                "maxOutputTokens": 1024
              }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = payload.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:streamGenerateContent?key=$apiKey")
            .post(requestBody)
            .build()

        val fullResponseBuilder = StringBuilder()

        try {
            _connectionStatus.value = "STREAMING"
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    _connectionStatus.value = "ERROR"
                    if (imageBase64 != null) {
                        _visionEngineStatus.value = "ERROR"
                    }
                    val code = response.code
                    val errorMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini api HTTP error: $code - $errorMsg")
                    throw Exception("Neural LLM Node returned diagnostic error code $code.")
                }

                val body = response.body ?: throw Exception("Gemini returned blank response payload.")
                body.charStream().buffered().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        // Find matches of "text": "..." on this line and yield
                        textRegex.findAll(currentLine).forEach { match ->
                            val escapedChunk = match.groupValues[1]
                            val chunk = unescapeJsonString(escapedChunk)
                            if (chunk.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    onChunk(chunk)
                                }
                                fullResponseBuilder.append(chunk)
                            }
                        }
                    }
                }
            }

            val finalResponseText = fullResponseBuilder.toString().trim()
            if (finalResponseText.isEmpty()) {
                throw Exception("Stream terminated with empty response.")
            }

            // 3. Save model turn to memory context (Requirement 3)
            synchronized(conversationHistory) {
                conversationHistory.add(Turn("model", finalResponseText))
                updateSessionState()
            }

            // Diagnose timing
            val duration = System.currentTimeMillis() - startTime
            _lastResponseTimeMs.value = duration
            _analysisTimeMs.value = duration
            _connectionStatus.value = "HEALTHY"
            _visionEngineStatus.value = "ONLINE"

            finalResponseText
        } catch (e: Exception) {
            Log.e(TAG, "Gemini network call error: ${e.message}", e)
            _connectionStatus.value = "OFFLINE"
            _visionEngineStatus.value = "ERROR"
            
            // Remove the failed user turn to keep context clean
            synchronized(conversationHistory) {
                if (conversationHistory.isNotEmpty() && conversationHistory.last().role == "user") {
                    conversationHistory.removeAt(conversationHistory.lastIndex)
                    updateSessionState()
                }
            }
            throw e
        }
    }

    private fun escapeJsonString(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJsonString(input: String): String {
        return input
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
