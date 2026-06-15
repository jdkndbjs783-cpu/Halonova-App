package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AutoLearningEngine(
    private val context: Context,
    private val repository: UserRepository,
    private val scope: CoroutineScope
) {
    private val _learningScore = MutableStateFlow(32) // Initial synaptic score
    val learningScore = _learningScore.asStateFlow()

    private val _brainStatus = MutableStateFlow("SYNAPSE_ONLINE") // SYNAPSE_ONLINE, ANALYZING, PATTERNS_OPTIMIZED
    val brainStatus = _brainStatus.asStateFlow()

    private val _lastLearnedPattern = MutableStateFlow("Awaiting telemetry signals...")
    val lastLearnedPattern = _lastLearnedPattern.asStateFlow()

    private val _smartSuggestions = MutableStateFlow<List<String>>(
        listOf(
            "Check Missed Calls (Security Protocol)",
            "Sync Cognitive Memory Uplink",
            "Perform System Diagnostics Scan"
        )
    )
    val smartSuggestions = _smartSuggestions.asStateFlow()

    init {
        // Run initial analysis and seed mock history if empty to demonstrate auto-learning
        scope.launch {
            seedMockHistoryIfEmpty()
            analyzeUsagePatterns()
        }
    }

    /**
     * Seeds initial user action logs to show learning system in action on fresh install
     */
    private suspend fun seedMockHistoryIfEmpty() {
        withContext(Dispatchers.IO) {
            try {
                val launches = repository.allLaunchHistoryFlow.first()
            // We'll peek if there are any launches
            val memories = repository.getAllCategorizedMemories()
            if (memories.isEmpty()) {
                // Seed some basic user preferences to start with
                repository.insertCategorizedMemory(
                    CategorizedMemoryEntity(
                        category = "Personal Preferences",
                        content = "Prefers high-performance dark cybernetic dashboard views."
                    )
                )
                repository.insertCategorizedMemory(
                    CategorizedMemoryEntity(
                        category = "Assistant Learning Notes",
                        content = "Synaptic nodes calibrated for real-time speech processing."
                    )
                )

                // Insert 2 launch events of YouTube and WhatsApp to simulate routine
                repository.insertLaunchEvent(
                    AppLaunchHistoryEntity(
                        appName = "YouTube",
                        packageName = "com.google.android.youtube",
                        launchTime = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                        status = "SUCCESS"
                    )
                )
                repository.insertLaunchEvent(
                    AppLaunchHistoryEntity(
                        appName = "WhatsApp",
                        packageName = "com.whatsapp",
                        launchTime = System.currentTimeMillis() - 3600000 * 2 + 120000, // 2 mins after YouTube
                        status = "SUCCESS"
                    )
                )

                // Repeat it to establish a statistical pattern (YouTube + WhatsApp)
                repository.insertLaunchEvent(
                    AppLaunchHistoryEntity(
                        appName = "YouTube",
                        packageName = "com.google.android.youtube",
                        launchTime = System.currentTimeMillis() - 3600000 * 24, // 24 hours ago
                        status = "SUCCESS"
                    )
                )
                repository.insertLaunchEvent(
                    AppLaunchHistoryEntity(
                        appName = "WhatsApp",
                        packageName = "com.whatsapp",
                        launchTime = System.currentTimeMillis() - 3600000 * 24 + 180000, // 3 mins after YouTube
                        status = "SUCCESS"
                    )
                )
            }
                Log.d("AutoLearningEngine", "Mock history seeding completed or not needed")
            } catch (e: Exception) {
                Log.e("AutoLearningEngine", "Error seeding mock history: ${e.message}")
            }
            Unit
        }
    }

    /**
     * Examines database events and derives patterns
     */
    suspend fun analyzeUsagePatterns() = withContext(Dispatchers.IO) {
        _brainStatus.value = "ANALYZING_PATTERNS"
        try {
            val memories = repository.getAllCategorizedMemories()
            
            // Query histories
            val launchHistory = repository.allLaunchHistoryFlow.first()
            val contactHistory = repository.allContactSearchHistoryFlow.first()
            
            val learnedPatterns = mutableListOf<String>()
            var newlyLearnedCount = 0

            // 1. Analyze Favorite Apps & Habits
            val appCounts = launchHistory.filter { it.status == "SUCCESS" }
                .groupBy { it.appName }
                .mapValues { it.value.size }

            var favoriteApp: String? = null
            var maxLaunches = 0
            appCounts.forEach { (appName, count) ->
                if (count > maxLaunches) {
                    maxLaunches = count
                    favoriteApp = appName
                }
            }

            if (favoriteApp != null && maxLaunches >= 2) {
                // Check if this Habit is already saved
                val existingHabit = memories.any { it.category == "Daily Habits" && it.content.contains(favoriteApp!!) }
                if (!existingHabit) {
                    val contentText = "Frequently opens $favoriteApp app (Est. frequency: $maxLaunches times)."
                    repository.insertCategorizedMemory(
                        CategorizedMemoryEntity(
                            category = "Daily Habits",
                            content = contentText,
                            patternName = "Frequent App: $favoriteApp",
                            frequencyCount = maxLaunches
                        )
                    )
                    learnedPatterns.add("Freq App: $favoriteApp")
                    newlyLearnedCount++
                }
            }

            // 2. Clear command sequences patterns (e.g. YouTube followed by WhatsApp within 5 mins)
            var hasSequence = false
            val sortedLaunches = launchHistory.sortedBy { it.launchTime }
            for (i in 0 until sortedLaunches.size - 1) {
                val current = sortedLaunches[i]
                val next = sortedLaunches[i + 1]
                val timeDiff = next.launchTime - current.launchTime
                if (current.appName == "YouTube" && next.appName == "WhatsApp" && timeDiff in 1..300000) {
                    hasSequence = true
                    break
                }
            }

            if (hasSequence) {
                val existingSeq = memories.any { it.category == "Daily Habits" && it.content.contains("YouTube -> WhatsApp") }
                if (!existingSeq) {
                    val contentText = "Launches WhatsApp immediately after YouTube pattern."
                    repository.insertCategorizedMemory(
                        CategorizedMemoryEntity(
                            category = "Daily Habits",
                            content = contentText,
                            patternName = "Sequence: YouTube -> WhatsApp",
                            frequencyCount = 2
                        )
                    )
                    learnedPatterns.add("Sequence: YouTube -> WhatsApp")
                    newlyLearnedCount++
                }
            }

            // 3. Command history frequency (simulate checking frequently used commands)
            val preferences = repository.getPreferences()
            val hasToneMemo = memories.any { it.category == "Personal Preferences" && it.content.contains("personality Mode") }
            if (!hasToneMemo) {
                repository.insertCategorizedMemory(
                    CategorizedMemoryEntity(
                        category = "Personal Preferences",
                        content = "Prefers ${preferences.personalityMode} style speech responses.",
                        patternName = "Preferred Tone"
                    )
                )
            }

            // 4. Update Score & Suggestions
            val updatedMemories = repository.getAllCategorizedMemories()
            val totalMemories = updatedMemories.size
            _learningScore.value = (35 + (totalMemories * 7)).coerceAtMost(100)

            // Formulate suggestions
            val suggestionsList = mutableListOf<String>()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isMorning = hour in 5..11

            if (isMorning && updatedMemories.any { it.content.contains("YouTube") }) {
                suggestionsList.add("Launch YouTube (Morning Routine)")
            } else {
                suggestionsList.add("Perform System Diagnostics Scan")
            }

            if (hasSequence) {
                suggestionsList.add("Open WhatsApp (Post-YouTube Uplink)")
            }

            suggestionsList.add("Check Missed Calls (Security Protocol)")
            _smartSuggestions.value = suggestionsList

            // Announce newly learned habit if any
            if (newlyLearnedCount > 0 && learnedPatterns.isNotEmpty()) {
                val latestPattern = learnedPatterns.first()
                _lastLearnedPattern.value = latestPattern
                
                val contentDesc = when {
                    latestPattern.contains("Sequence") -> "you often use WhatsApp after YouTube"
                    latestPattern.contains("YouTube") -> "you frequently open YouTube"
                    latestPattern.contains("WhatsApp") -> "you frequently open WhatsApp"
                    else -> latestPattern
                }

                val phrase = if (preferences.activeAssistant == "ZOYA") {
                    "Hey! I learned something new about you! I noticed that $contentDesc."
                } else {
                    "Sir, I have learned a new habit. My cognitive nodes detected that $contentDesc."
                }
                
                // Broadcast announcement via singleton
                AppVoiceAnnouncer.announce(phrase)
                
                repository.insertServiceLog(
                    ServiceLogEntity(
                        eventType = "AI Memory",
                        message = "COGNITIVE_LEARN // Discovered habit: $latestPattern"
                    )
                )
            } else {
                val lastMemo = updatedMemories.firstOrNull { it.category == "Daily Habits" }
                _lastLearnedPattern.value = lastMemo?.patternName ?: "Pattern stability confirmed."
            }

            _brainStatus.value = "PATTERNS_OPTIMIZED"
        } catch (e: Exception) {
            Log.e("AutoLearningEngine", "Error analyzing learning usage: ${e.message}")
            _brainStatus.value = "SYNAPSE_ONLINE"
        }
    }

    /**
     * Manually simulates a user habit creation for instant testing & visual demonstration
     */
    fun simulateHabitLearning(appName: String) {
        scope.launch {
            _brainStatus.value = "ANALYZING_PATTERNS"
            try {
                // Record launcher entry twice to trigger habit detection
                repository.insertLaunchEvent(
                    AppLaunchHistoryEntity(
                        appName = appName,
                        packageName = "com.sample.$appName",
                        launchTime = System.currentTimeMillis() - 10000,
                        status = "SUCCESS"
                    )
                )
                repository.insertLaunchEvent(
                    AppLaunchHistoryEntity(
                        appName = appName,
                        packageName = "com.sample.$appName",
                        launchTime = System.currentTimeMillis(),
                        status = "SUCCESS"
                    )
                )

                // Force analysis
                analyzeUsagePatterns()
            } catch (e: Exception) {
                Log.e("AutoLearningEngine", "Simulated habit creation error: ${e.message}")
            }
        }
    }
}
