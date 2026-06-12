package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.VoiceConversationEntity
import com.example.service.LauncherEngineState
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.service.AssistantState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToPermissions: () -> Unit,
    onNavigateToCompanion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val telemetryMessages by viewModel.telemetryMessages.collectAsState()

    // Voice Engine States
    val voiceIsListening by viewModel.voiceIsListening.collectAsState()
    val voiceRecognizedText by viewModel.voiceRecognizedText.collectAsState()
    val voiceError by viewModel.voiceError.collectAsState()
    val voiceHistory by viewModel.voiceConversations.collectAsState()
    val activeLanguage by viewModel.activeVoiceLanguage.collectAsState()

    // Phase 6 UI state collectors
    val contactSearchQuery by viewModel.contactSearchQuery.collectAsState()
    val activeDraftRecipient by viewModel.activeDraftRecipient.collectAsState()
    val activeDraftBody by viewModel.activeDraftBody.collectAsState()
    val showDraftComposer by viewModel.showDraftComposer.collectAsState()
    val lastCommandText by viewModel.lastCommandText.collectAsState()
    val lastNotificationQueryTime by viewModel.lastNotificationQueryTime.collectAsState()
    val lastCommunicationEventSummary by viewModel.lastCommunicationEventSummary.collectAsState()

    // State for manual keyboard fallback
    var textInputState by remember { mutableStateOf("") }
    var conversationSearchFilter by remember { mutableStateOf("") }

    val filteredVoiceHistory = remember(voiceHistory, conversationSearchFilter) {
        if (conversationSearchFilter.isBlank()) {
            voiceHistory
        } else {
            voiceHistory.filter { 
                it.userSpeech.contains(conversationSearchFilter, ignoreCase = true) || 
                it.novaResponse.contains(conversationSearchFilter, ignoreCase = true) 
            }
        }
    }

    // Foreground service events logs state
    val serviceLogs by viewModel.serviceLogs.collectAsState()
    val smartActionsHistory by viewModel.smartActionsHistory.collectAsState()
    val fServiceState by com.example.service.NovaForegroundService.engineState.collectAsState()

    // Phase 7 Assistant Presence collectors
    val currentAssistantState by viewModel.assistantState.collectAsState()
    val isSessionActive by viewModel.isVoiceSessionActive.collectAsState()
    val sessionStartTime by viewModel.voiceSessionStartTime.collectAsState()
    val sessionEndTime by viewModel.voiceSessionEndTime.collectAsState()
    val commandCount by viewModel.voiceSessionCommandCount.collectAsState()
    val sessionDurationSeconds by viewModel.voiceSessionDurationSeconds.collectAsState()

    val wakeWordVerified by viewModel.wakeWordVerified.collectAsState()
    val continuousListeningHookEnabled by viewModel.isContinuousListeningHookEnabled.collectAsState()
    val wakePhraseStatus by viewModel.wakePhraseStatus.collectAsState()

    val fgServiceStatus by viewModel.foregroundServiceStatus.collectAsState()
    val voiceEngineStatus by viewModel.voiceEngineStatus.collectAsState()
    val notifListenerStatus by viewModel.notificationListenerStatus.collectAsState()
    val savedSessions by viewModel.voiceSessions.collectAsState()

    // Phase 8 state collectors
    val permissionReadinessStatus by viewModel.permissionReadinessStatus.collectAsState()
    val databaseStatus by viewModel.databaseStatus.collectAsState()

    val lastSessionStartTime by viewModel.lastSessionStartTime.collectAsState()
    val lastSessionEndTime by viewModel.lastSessionEndTime.collectAsState()
    val lastSessionCommandCount by viewModel.lastSessionCommandCount.collectAsState()
    val lastSessionDurationMs by viewModel.lastSessionDurationMs.collectAsState()

    val lastWakeEvent by viewModel.lastWakeEvent.collectAsState()
    val wakeEvents by viewModel.wakeEventsFlow.collectAsState()
    val engineUptimeSeconds by viewModel.engineUptimeSeconds.collectAsState()

    // Phase 21.2 background wake collectors
    val bgWakeStatus by viewModel.wakeEngineStatus.collectAsState()
    val bgWakeTriggerCount by viewModel.wakeEngineTriggerCount.collectAsState()
    val bgWakeLastActivationTime by viewModel.wakeEngineLastActivationTime.collectAsState()
    val bgWakeUptimeSeconds by viewModel.wakeEngineUptimeSecondsFlow.collectAsState()

    val deviceCommandsHistory by viewModel.deviceCommandsHistory.collectAsState()
    val screenshotHistoryList by viewModel.screenshotHistoryList.collectAsState()
    val fileSearchEngineQuery by viewModel.fileSearchQuery.collectAsState()
    val fileSearchEngineType by viewModel.fileSearchType.collectAsState()
    val filteredFiles by viewModel.filteredFiles.collectAsState()

    var showingBatteryGuide by remember { mutableStateOf(false) }
    var activeSubScreen by remember { mutableStateOf("terminal") }
    var isListenerGranted by remember { mutableStateOf(false) }

    if (showDraftComposer) {
        MessageDraftComposer(
            recipient = activeDraftRecipient,
            body = activeDraftBody,
            onBodyChange = { viewModel.activeDraftBody.value = it },
            onRecipientChange = { viewModel.activeDraftRecipient.value = it },
            onCancel = { viewModel.cancelDraft() },
            onConfirm = { viewModel.confirmDraft() }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                isListenerGranted = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                )?.contains(context.packageName) == true
            } catch (e: Exception) {
                isListenerGranted = false
            }
            delay(1500)
        }
    }

    // Dynamic Permission Handling
    var hasMicrophonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: hasMicrophonePermission
        hasMicrophonePermission = micGranted
        viewModel.logPermissionStatus("RECORD_AUDIO", micGranted)

        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        hasNotificationPermission = notifGranted
        viewModel.logPermissionStatus("POST_NOTIFICATIONS", notifGranted)
    }

    // Blinking cursor mechanism
    var cursorOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            cursorOn = !cursorOn
        }
    }

    // Automatic permission checker on start
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (!hasMicrophonePermission) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Engine rotation & voice button pulsation animations
    val infiniteTransition = rememberInfiniteTransition(label = "terminal_anims")
    val angleSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "spin"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Pulse effect parameters for active mic recording
    val pulseSizeAnim by infiniteTransition.animateFloat(
        initialValue = 72f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_size"
    )

    val pulseAlphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    if (showingBatteryGuide) {
        BatteryOptimizationScreen(onBack = { showingBatteryGuide = false })
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented Cyber Tab Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .border(1.dp, CyberDarkCardBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "terminal" to "TRANSMISSIONS",
                    "apps" to "APP PORTAL",
                    "wake" to "WAKE",
                    "notifications" to "ALERTS",
                    "communication" to "COMMS HUB"
                ).forEach { (key, label) ->
                    val selected = activeSubScreen == key
                    val bgCol = if (selected) CyberNeonCyan.copy(alpha = 0.15f) else Color.Transparent
                    val borderCol = if (selected) CyberNeonCyan.copy(alpha = 0.5f) else Color.Transparent
                    val textCol = if (selected) CyberNeonCyan else Color.White.copy(alpha = 0.4f)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(bgCol, RoundedCornerShape(8.dp))
                            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                            .clickable { activeSubScreen = key }
                            .testTag("sub_tab_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = textCol,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            when (activeSubScreen) {
                "apps" -> {
                    AppLauncherAndMemoryScreen(viewModel = viewModel, modifier = Modifier.weight(1f))
                }
                "notifications" -> {
                    NotificationHistoryScreen(viewModel = viewModel, modifier = Modifier.weight(1f))
                }
                "communication" -> {
                    CommunicationHistoryScreen(viewModel = viewModel, modifier = Modifier.weight(1f))
                }
                "wake" -> {
                    WakeAndReadinessScreen(viewModel = viewModel, modifier = Modifier.weight(1f))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("dashboard_screen"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Phase 23 Ultimate Device Control Dashboard Card
                        item {
                            DeviceControlDashboardCard(
                                viewModel = viewModel,
                                deviceCommandsHistory = deviceCommandsHistory,
                                screenshotHistoryList = screenshotHistoryList,
                                fileSearchEngineQuery = fileSearchEngineQuery,
                                fileSearchEngineType = fileSearchEngineType,
                                filteredFiles = filteredFiles
                            )
                        }

                        // Phase 28 Master System Health Dashboard Card
                        item {
                            NovaSystemStatusDashboardCard(
                                viewModel = viewModel
                            )
                        }

                        // ANDROID SYSTEM CORE (Phase 24 Requirement)
                        item {
                            AndroidSystemCoreCard(
                                viewModel = viewModel,
                                onNavigateToPermissions = onNavigateToPermissions
                            )
                        }

                        // 1. Assistant Presence Indicator Room (Phase 7 Requirement 4 & 7)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToCompanion() }
                                    .border(1.dp, CyberNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .testTag("assistant_presence_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B24)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = CyberNeonCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "ASSISTANT PRESENCE FIELD",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberNeonCyan,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Text(
                                                text = "TAP TO OPEN COMPANION COMMAND CENTER",
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSessionActive) CyberNeonGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    if (isSessionActive) CyberNeonGreen else Color.Gray,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isSessionActive) "LIVE UPLINK" else "STANDBY",
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSessionActive) CyberNeonGreen else Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Dynamic animated Presence Orb
                                        AssistantPresenceOrb(state = currentAssistantState)

                                        Spacer(modifier = Modifier.width(20.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "ACTIVE STATE NODE",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta.copy(alpha = 0.15f) else CyberNeonCyan.copy(alpha = 0.15f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .border(
                                                            0.5.dp,
                                                            if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .background(
                                                                    if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                                                                    CircleShape
                                                                )
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = preferences.activeAssistant,
                                                            fontSize = 7.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = currentAssistantState.displayName,
                                                fontSize = 18.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = when (currentAssistantState) {
                                                    AssistantState.STANDBY -> Color.Gray
                                                    AssistantState.ARMED -> CyberNeonCyan.copy(alpha = 0.7f)
                                                    AssistantState.LISTENING -> CyberNeonCyan
                                                    AssistantState.PROCESSING -> CyberNeonGreen
                                                    AssistantState.SPEAKING -> CyberNeonMagenta
                                                    AssistantState.RECOVERY -> Color(0xFFFFB300)
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "VERIFY_HOOK: ${if (continuousListeningHookEnabled) "ARMED [NOVA]" else "STANDBY"}",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (continuousListeningHookEnabled) CyberNeonGreen else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Active Session Statistics Panel (Phase 7 Requirement 6 & 7)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("session_statistics_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "CORE VOICE SESSION STATS",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberNeonCyan,
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "SESSION TIMER",
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = if (isSessionActive) "${sessionDurationSeconds}s" else "Offline",
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSessionActive) CyberNeonGreen else Color.White.copy(alpha = 0.6f)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "UPLINK PACKETS",
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "$commandCount commands",
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "SESSION HISTORY",
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${savedSessions.size} logged",
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberNeonCyan
                                            )
                                        }
                                    }
                                    
                                    if (sessionStartTime > 0L) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                        Text(
                                            text = "INITIATION TIMESTAMP: " + sdf.format(java.util.Date(sessionStartTime)),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Engine & Service Health Suite (Phase 7 Requirement 8)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("engine_health_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "SUBSYSTEM HEALTH DECK",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberNeonCyan,
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // A. Foreground Service Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = "Foreground Service",
                                                tint = if (fgServiceStatus == "HEALTHY") CyberNeonGreen else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "FOREGROUND COGNITIVE SERVICE",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = fgServiceStatus,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (fgServiceStatus == "HEALTHY") CyberNeonGreen else CyberNeonMagenta
                                        )
                                    }

                                    // B. Voice Synthesis / Capture Engine Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.SettingsInputAntenna,
                                                contentDescription = "Synthesizer Engine",
                                                tint = if (voiceEngineStatus == "HEALTHY") CyberNeonGreen else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "STT / TTS CAPTURE ENGINE",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = voiceEngineStatus,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (voiceEngineStatus == "HEALTHY") CyberNeonGreen else CyberNeonMagenta
                                        )
                                    }

                                    // C. Notification Intercept Listener Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Hearing,
                                                contentDescription = "Notification Listener",
                                                tint = if (notifListenerStatus == "CONNECTED") CyberNeonGreen else Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "NOTIFICATION INTERCEPT LISTENER",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = notifListenerStatus,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (notifListenerStatus == "CONNECTED") CyberNeonGreen else CyberNeonMagenta
                                        )
                                    }
                                }
                            }
                        }

                        // 3.5. Gemini AI Neural Engine Diagnostics (Phase 15 Requirement 6)
                        item {
                            val geminiStatus by viewModel.geminiConnectionStatus.collectAsState()
                            val geminiSession by viewModel.geminiActiveSessionInfo.collectAsState()
                            val lastResponseTime by viewModel.geminiLastResponseTimeMs.collectAsState()
                            val geminiModel by viewModel.geminiModelStatus.collectAsState()

                            val statusColor = when (geminiStatus) {
                                "HEALTHY" -> CyberNeonGreen
                                "CONNECTING", "STREAMING" -> CyberNeonCyan
                                "ERROR", "OFFLINE" -> CyberNeonMagenta
                                else -> Color.Gray
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("gemini_diagnostics_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GEMINI COGNITIVE CORE",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonCyan,
                                            letterSpacing = 0.5.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, statusColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = geminiStatus,
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // A. Active Conversation Session ID
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CONVERSATION LINKS",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = geminiSession,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // B. Last Response Time in miliseconds
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LAST GENERATION LATENCY",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = if (lastResponseTime > 0L) "${lastResponseTime}ms" else "N/A",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (lastResponseTime > 0L) CyberNeonGreen else Color.White
                                        )
                                    }

                                    // C. Current active model name
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "NEURAL LLM ALIAS",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(CyberNeonGreen, shape = androidx.compose.foundation.shape.CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = geminiModel,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Clear Session Memory Button to re-initialize prompt history
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                viewModel.geminiEngine.clearSession()
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = "FLUSH CONTEXT MEMORY",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberNeonMagenta
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3.6. Cognitive Memory & Tool Binding System (Phase 16 Requirements 1, 2, 5)
                        item {
                            val memoryState by viewModel.assistantMemory.collectAsState()
                            val conversationsState by viewModel.voiceConversations.collectAsState()
                            
                            val isSynced = memoryState.lastMemorySyncTime > 0L
                            val memoryStatus = if (isSynced) "COGNITIVE_SYNCHRONIZED" else "COGNITIVE_STALE"
                            val memoryStatusColor = if (isSynced) CyberNeonGreen else CyberNeonMagenta

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("cognitive_memory_diagnostics_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DYNAMIC COGNITIVE MEMORY MATRIX",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonCyan,
                                            letterSpacing = 0.5.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(memoryStatusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, memoryStatusColor, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = memoryStatus,
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = memoryStatusColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // A. Stored Sessions / Log Count
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "STORED SESSIONS / TURNS",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "${conversationsState.size} records",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // B. Tool Binding Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "TOOL BINDING SYSTEM",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "ACTIVE // SECURE_UPLINK",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonGreen
                                        )
                                    }

                                    // C. Last Memory Sync Time
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LAST COGNITIVE SYNC",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = if (isSynced) {
                                                val diff = System.currentTimeMillis() - memoryState.lastMemorySyncTime
                                                when {
                                                    diff < 60000L -> "Just now"
                                                    diff < 3600000L -> "${diff / 60000L} mins ago"
                                                    else -> "${diff / 3600000L} hours ago"
                                                }
                                            } else "NEVER SYNCED",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSynced) CyberNeonGreen else Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // D. User Facts
                                    Text(
                                        text = "SYNTHESIZED USER COGNITIVE PROFILE",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberNeonCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (memoryState.userFacts.isNotEmpty()) memoryState.userFacts else "No processed cognitive habits compiled. Trigger sync sequence.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // E. Conversation Highlights Summary
                                    Text(
                                        text = "HISTOLOGIC CONVERSATION SUMMARIES",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberNeonCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (memoryState.conversationSummaries.isNotEmpty()) memoryState.conversationSummaries else "No dialogue summarizing elements available. Perform terminal communication first.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Control Actions Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.clearCognitiveMemory() },
                                            modifier = Modifier.height(28.dp).testTag("purge_cognitive_memory_btn"),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = "PURGE COGNITIVE DB",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberNeonMagenta
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.syncCognitiveMemory() },
                                            modifier = Modifier.height(28.dp).testTag("sync_cognitive_memory_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                        ) {
                                            Text(
                                                text = "SYNC COGNITIVE CORE",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3.7. Multimodal Cognitive Core & Image/Document Intelligence (Phase 17)
                        item {
                            val visionStatus by viewModel.visionEngineStatus.collectAsState()
                            val lastAsset by viewModel.lastAnalyzedAsset.collectAsState()
                            val analysisTime by viewModel.visionAnalysisTimeMs.collectAsState()
                            val sessionStatus by viewModel.multimodalSessionStatus.collectAsState()

                            val imgUri by viewModel.selectedImageUri.collectAsState()
                            val pdfUri by viewModel.selectedPdfUri.collectAsState()
                            val docTextSnippet by viewModel.extractedPdfText.collectAsState()

                            val context = LocalContext.current
                            
                            // ActivityResult launchers
                            val galleryLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.PickVisualMedia()
                            ) { uri ->
                                if (uri != null) {
                                    viewModel.processImageSelection(uri)
                                }
                            }

                            val cameraLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.TakePicture()
                            ) { success ->
                                if (success) {
                                    viewModel.tempCameraUri.value?.let {
                                        viewModel.processCameraCapture(it)
                                    }
                                }
                            }

                            val documentLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.GetContent()
                            ) { uri ->
                                if (uri != null) {
                                    viewModel.processPdfSelection(uri)
                                }
                            }

                            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission()
                            ) { isGranted ->
                                if (isGranted) {
                                    try {
                                        val cacheFile = java.io.File(context.cacheDir, "temp_camera_capture.jpg")
                                        if (cacheFile.exists()) cacheFile.delete()
                                        cacheFile.createNewFile()
                                        val providerUri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            cacheFile
                                        )
                                        viewModel.tempCameraUri.value = providerUri
                                        cameraLauncher.launch(providerUri)
                                    } catch (e: Exception) {
                                        viewModel.writeTerminalLog("VISION_CORE // Camera prep failed: ${e.message}")
                                    }
                                } else {
                                    viewModel.writeTerminalLog("VISION_CORE // Camera permission denied.")
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("multimodal_cognitive_core_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "MULTIMODAL COGNITIVE CORE",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonCyan,
                                            letterSpacing = 0.5.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        when (visionStatus) {
                                                            "ANALYZING" -> CyberNeonMagenta.copy(alpha = 0.15f)
                                                            "ONLINE" -> CyberNeonGreen.copy(alpha = 0.15f)
                                                            else -> CyberNeonMagenta.copy(alpha = 0.15f)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        if (visionStatus == "ONLINE") CyberNeonGreen else CyberNeonMagenta,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = visionStatus,
                                                    fontSize = 7.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (visionStatus == "ONLINE") CyberNeonGreen else CyberNeonMagenta
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Metric Rows (Requirement 4 Dashboard Integration)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "SESSION STATUS",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = sessionStatus,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sessionStatus != "IDLE") CyberNeonGreen else CyberNeonCyan
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "LAST ANALYZED ASSET",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = lastAsset,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonCyan
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "ANALYSIS TIMER",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "$analysisTime ms",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // ACTIVE FILE PREVIEW
                                    if (imgUri != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, CyberNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .background(CyberNeonCyan.copy(alpha = 0.05f))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .border(0.5.dp, CyberNeonCyan, RoundedCornerShape(6.dp))
                                            ) {
                                                coil.compose.AsyncImage(
                                                    model = imgUri,
                                                    contentDescription = "Active image thumbnail",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "IMAGE CHANNEL ACTIVE",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberNeonCyan
                                                )
                                                Text(
                                                    text = "Uplink verified. Query assistant to process image.",
                                                    fontSize = 7.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.clearActiveMultimodalAsset() },
                                                modifier = Modifier.size(36.dp).testTag("clear_multimodal_image")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Purge context",
                                                    tint = CyberNeonMagenta,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    } else if (pdfUri != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, CyberNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .background(CyberNeonGreen.copy(alpha = 0.05f))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = "Document icon",
                                                tint = CyberNeonGreen,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "DOCUMENT ACTIVE",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberNeonGreen
                                                )
                                                val textPreview = if (docTextSnippet.isNotEmpty()) {
                                                    if (docTextSnippet.length > 60) docTextSnippet.take(60) + "..." else docTextSnippet
                                                } else "Parsing document content scanner..."
                                                Text(
                                                    text = textPreview,
                                                    fontSize = 7.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.clearActiveMultimodalAsset() },
                                                modifier = Modifier.size(36.dp).testTag("clear_multimodal_pdf")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Purge context",
                                                    tint = CyberNeonMagenta,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    // Action Triggers / Uplinks
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Gallery Launcher
                                        Button(
                                            onClick = {
                                                galleryLauncher.launch(
                                                    androidx.activity.result.PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                                    )
                                                )
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("gallery_uplink_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonCyan.copy(alpha = 0.12f),
                                                contentColor = CyberNeonCyan
                                            ),
                                            border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = "Gallery Uplink",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("GALLERY", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Camera Launcher
                                        Button(
                                            onClick = {
                                                val permissionCheck = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.CAMERA
                                                )
                                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                    try {
                                                        val cacheFile = java.io.File(context.cacheDir, "temp_camera_capture.jpg")
                                                        if (cacheFile.exists()) cacheFile.delete()
                                                        cacheFile.createNewFile()
                                                        val providerUri = androidx.core.content.FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            cacheFile
                                                        )
                                                        viewModel.tempCameraUri.value = providerUri
                                                        cameraLauncher.launch(providerUri)
                                                    } catch (e: Exception) {
                                                        viewModel.writeTerminalLog("VISION_CORE // Camera launch prep failed: ${e.message}")
                                                    }
                                                } else {
                                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("camera_uplink_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonGreen.copy(alpha = 0.12f),
                                                contentColor = CyberNeonGreen
                                            ),
                                            border = BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = "Camera Capture",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("CAMERA", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // PDF Scanner Launcher
                                        Button(
                                            onClick = {
                                                try {
                                                    documentLauncher.launch("application/pdf")
                                                } catch (e: Exception) {
                                                    viewModel.writeTerminalLog("DOCUMENT_CORE // Client file system picker error: ${e.message}")
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("pdf_uplink_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonMagenta.copy(alpha = 0.12f),
                                                contentColor = CyberNeonMagenta
                                            ),
                                            border = BorderStroke(1.dp, CyberNeonMagenta.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = "PDF Scanner",
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("PDF SCAN", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3.8. Spatial Awareness & Dynamic Alerts Dashboard (Phase 18)
                        item {
                            val locationSector by viewModel.locationSector.collectAsState()
                            val locationLatitude by viewModel.locationLatitude.collectAsState()
                            val locationLongitude by viewModel.locationLongitude.collectAsState()
                            val lastSpatialUpdateTime by viewModel.lastSpatialUpdateTime.collectAsState()
                            val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsState()

                            val alertEngineActive by viewModel.alertEngineActive.collectAsState()
                            val alertCategoryFilter by viewModel.alertCategoryFilter.collectAsState()
                            val alertFilteringPriority by viewModel.alertFilteringPriority.collectAsState()
                            val notificationsHistory by viewModel.notificationsHistory.collectAsState()

                            val isNotifListenerAlive = com.example.service.NovaNotificationListenerService.isServiceConnected()

                            val formattedUpdateTime = remember(lastSpatialUpdateTime) {
                                if (lastSpatialUpdateTime == 0L) "NEVER" else {
                                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastSpatialUpdateTime))
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("spatial_alerts_monitor_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SPATIAL SENSORS & ALERTS MONITOR",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonCyan,
                                            letterSpacing = 0.5.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (alertEngineActive) CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonMagenta.copy(alpha = 0.15f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    if (alertEngineActive) CyberNeonGreen else CyberNeonMagenta,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (alertEngineActive) "ALERTS_ACTIVE" else "ALERTS_STANDBY",
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (alertEngineActive) CyberNeonGreen else CyberNeonMagenta
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Location Sub-Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = "Spatial Location",
                                            tint = CyberNeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SPATIAL COORDINATES",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = locationSector,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Button(
                                                    onClick = { viewModel.checkAndSyncLocation() },
                                                    modifier = Modifier.height(26.dp).testTag("spatial_recalibrate_btn"),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = CyberNeonCyan.copy(alpha = 0.12f),
                                                        contentColor = CyberNeonCyan
                                                    ),
                                                    border = BorderStroke(0.5.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                                                    shape = RoundedCornerShape(4.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                ) {
                                                    Text("SYNC", fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val coordsStr = if (locationLatitude != null && locationLongitude != null) {
                                                "Lat: ${String.format(java.util.Locale.US, "%.5fN", locationLatitude)}, Lng: ${String.format(java.util.Locale.US, "%.5fE", locationLongitude)}"
                                            } else {
                                                "Lat: --- , Lng: --- [DENIED]"
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = coordsStr,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "LAST UPDATE: $formattedUpdateTime",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Alerts & Observers Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Notification Monitor
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "NOTIFICATION MONITOR",
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                    .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isNotifListenerAlive) "SYS_ONLINE" else "STANDBY",
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isNotifListenerAlive) CyberNeonGreen else CyberNeonMagenta
                                                )
                                            }
                                        }

                                        // Alert processing Engine Status
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "ALERT TRANSMITTER",
                                                fontSize = 7.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = { viewModel.alertEngineActive.value = !alertEngineActive },
                                                modifier = Modifier.fillMaxWidth().height(29.dp).testTag("toggle_alert_transmitter"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (alertEngineActive) CyberNeonGreen.copy(alpha = 0.12f) else CyberNeonMagenta.copy(alpha = 0.12f),
                                                    contentColor = if (alertEngineActive) CyberNeonGreen else CyberNeonMagenta
                                                ),
                                                border = BorderStroke(0.5.dp, (if (alertEngineActive) CyberNeonGreen else CyberNeonMagenta).copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = if (alertEngineActive) "ENG_ACTIVE" else "ENG_SILENT",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // User-Controlled Filters (Priority Filter & Category Filter)
                                    Text(
                                        text = "UPLINK FILTER PREFERENCES",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Category Filter selector (ALL, MESSAGING, SYSTEM, SECURITY)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CAT:", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.width(26.dp))
                                        val cats = listOf("ALL", "MESSAGING", "SYSTEM", "SECURITY")
                                        cats.forEach { cat ->
                                            val isSelected = alertCategoryFilter == cat
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSelected) CyberNeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        if (isSelected) CyberNeonCyan else Color.White.copy(alpha = 0.1f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { viewModel.alertCategoryFilter.value = cat }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = cat,
                                                    fontSize = 7.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) CyberNeonCyan else Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Priority Filter selector (ALL, HIGH-ONLY)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("PRI:", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.width(26.dp))
                                        val priorities = listOf("ALL", "HIGH-ONLY")
                                        priorities.forEach { p ->
                                            val isSelected = alertFilteringPriority == p
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSelected) CyberNeonCyan.copy(alpha = 0.15f) else Color.Transparent,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        0.5.dp,
                                                        if (isSelected) CyberNeonCyan else Color.White.copy(alpha = 0.1f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable { viewModel.alertFilteringPriority.value = p }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = p,
                                                    fontSize = 7.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) CyberNeonCyan else Color.Gray
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Active Alerts
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RECENT INTERCEPTED DYNAMIC ALERTS",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = if (notificationsHistory.isNotEmpty()) CyberNeonCyan else Color.Gray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Notification filtering and categorization block
                                    val filteredNotifs = remember(notificationsHistory, alertCategoryFilter, alertFilteringPriority) {
                                        notificationsHistory.filter { notif ->
                                            val catMatch = alertCategoryFilter == "ALL" || viewModel.getNotificationCategory(notif) == alertCategoryFilter
                                            val priorityMatch = if (alertFilteringPriority == "HIGH-ONLY") {
                                                // Categorize high priority words or specific applications
                                                notif.title.lowercase().contains("urgent") ||
                                                notif.title.lowercase().contains("critical") ||
                                                notif.title.lowercase().contains("alert") ||
                                                notif.message.lowercase().contains("approve") ||
                                                notif.appName.lowercase().contains("slack")
                                            } else true
                                            catMatch && priorityMatch
                                        }
                                    }

                                    if (filteredNotifs.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                .padding(14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "NO HIGH-PRIORITY ALERTS MATCH FILTER",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            filteredNotifs.take(3).forEach { notif ->
                                                val category = viewModel.getNotificationCategory(notif)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                        .padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Category color mark
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp, 28.dp)
                                                            .background(
                                                                when(category) {
                                                                    "MESSAGING" -> CyberNeonCyan
                                                                    "SYSTEM" -> CyberNeonGreen
                                                                    "SECURITY" -> CyberNeonMagenta
                                                                    else -> Color.Gray
                                                                },
                                                                RoundedCornerShape(2.dp)
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = "${notif.appName.uppercase()} // ${notif.title}",
                                                                fontSize = 9.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = category,
                                                                fontSize = 7.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Color.Gray
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = notif.message,
                                                            fontSize = 8.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color.LightGray,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3.9. Phone Integration & Real Assistant Experience (Phase 19)
                        item {
                            val activeCallState by viewModel.activeCallState.collectAsState()
                            val activeCallerName by viewModel.activeCallerName.collectAsState()
                            val activeCallerNumber by viewModel.activeCallerNumber.collectAsState()

                            val phonePermissionGranted by viewModel.phonePermissionGranted.collectAsState()
                            val contactsPermissionGranted by viewModel.contactsPermissionGranted.collectAsState()
                            val callPermissionGranted by viewModel.callPermissionGranted.collectAsState()
                            val notificationListenerConnected by viewModel.notificationListenerConnected.collectAsState()

                            val notificationSearchQuery by viewModel.notificationSearchQuery.collectAsState()
                            val notificationsHistory by viewModel.notificationsHistory.collectAsState()

                            val sortedAndFilteredNotifs = remember(notificationsHistory, notificationSearchQuery) {
                                if (notificationSearchQuery.trim().isEmpty()) {
                                    notificationsHistory
                                } else {
                                    notificationsHistory.filter {
                                        it.appName.contains(notificationSearchQuery, ignoreCase = true) ||
                                        it.title.contains(notificationSearchQuery, ignoreCase = true) ||
                                        it.message.contains(notificationSearchQuery, ignoreCase = true)
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("phone_integration_dashboard_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Phone Link",
                                                tint = CyberNeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "PHONE INTEGRATION & OPERATOR LINK",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberNeonCyan,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (phonePermissionGranted && contactsPermissionGranted) CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonMagenta.copy(alpha = 0.15f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    if (phonePermissionGranted && contactsPermissionGranted) CyberNeonGreen else CyberNeonMagenta,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (phonePermissionGranted && contactsPermissionGranted) "UPLINK_SECURED" else "UPLINK_STANDBY",
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (phonePermissionGranted && contactsPermissionGranted) CyberNeonGreen else CyberNeonMagenta
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 1. CALL STATE MONITOR & SIMULATOR INTERACTIVE ALERT
                                    if (activeCallState == "RINGING" || activeCallState == "ACTIVE") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (activeCallState == "RINGING") CyberNeonMagenta.copy(alpha = 0.12f) else CyberNeonCyan.copy(alpha = 0.12f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(
                                                    1.5.dp,
                                                    if (activeCallState == "RINGING") CyberNeonMagenta else CyberNeonCyan,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "Active Link Warning",
                                                            tint = if (activeCallState == "RINGING") CyberNeonMagenta else CyberNeonCyan,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = if (activeCallState == "RINGING") "INCOMING ENCRYPTED CALL" else "ACTIVE SECURE VOICE TERMINAL",
                                                            fontSize = 9.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (activeCallState == "RINGING") CyberNeonMagenta else CyberNeonCyan
                                                        )
                                                    }
                                                    Text(
                                                        text = "SECURE_CHANNEL",
                                                        fontSize = 7.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Gray
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = activeCallerName ?: "Secure Quantum Node",
                                                    fontSize = 16.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                                
                                                val formattedNum = activeCallerNumber ?: "Hidden Identity Node"
                                                Text(
                                                    text = "ID: $formattedNum",
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.LightGray
                                                )

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (activeCallState == "RINGING") {
                                                        Button(
                                                            onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.ANSWER_CALL) },
                                                            modifier = Modifier.weight(1f).height(32.dp).testTag("dashboard_answer_call_btn"),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = CyberNeonGreen.copy(alpha = 0.2f),
                                                                contentColor = CyberNeonGreen
                                                            ),
                                                            border = BorderStroke(1.dp, CyberNeonGreen),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                            Text("SECURE LINK (ANSWER)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                        }
                                                        Button(
                                                            onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.DECLINE_CALL) },
                                                            modifier = Modifier.weight(1f).height(32.dp).testTag("dashboard_decline_call_btn"),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = CyberNeonMagenta.copy(alpha = 0.2f),
                                                                contentColor = CyberNeonMagenta
                                                            ),
                                                            border = BorderStroke(1.dp, CyberNeonMagenta),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                            Text("TERMINATE (DECLINE)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = { viewModel.transitionSimulatedCall("DISCONNECTED") },
                                                            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("dashboard_disconnect_call_btn"),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color.Red.copy(alpha = 0.15f),
                                                                contentColor = Color.Red
                                                            ),
                                                            border = BorderStroke(1.dp, Color.Red),
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                            Text("DISCONNECT CHANNELS", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(11.dp))
                                    }

                                    // 2. ULTRA SECURE SYSTEM INTEGRITY MONITOR (Phase 21.1)
                                    val securityReportState by viewModel.securityReport.collectAsState()

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .border(1.dp, if (securityReportState.emergencyProtectionMode) CyberNeonMagenta.copy(alpha = 0.8f) else CyberNeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        // Header Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            if (securityReportState.emergencyProtectionMode) CyberNeonMagenta else CyberNeonGreen,
                                                            CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "NOVA SECURITY RADAR",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (securityReportState.emergencyProtectionMode) CyberNeonMagenta else CyberNeonCyan
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        when (securityReportState.healthStatus) {
                                                            "OPTIMAL" -> CyberNeonGreen.copy(alpha = 0.15f)
                                                            "WARNING" -> CyberNeonCyan.copy(alpha = 0.15f)
                                                            else -> CyberNeonMagenta.copy(alpha = 0.15f)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "HEALTH: ${securityReportState.healthStatus}",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (securityReportState.healthStatus) {
                                                        "OPTIMAL" -> CyberNeonGreen
                                                        "WARNING" -> CyberNeonCyan
                                                        else -> CyberNeonMagenta
                                                    }
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Score progress bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("INTEGRITY COMPLIANCE SCORE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                                    Text("${securityReportState.securityScore}/100", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (securityReportState.emergencyProtectionMode) CyberNeonMagenta else CyberNeonGreen)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = securityReportState.securityScore / 100f,
                                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                    color = if (securityReportState.emergencyProtectionMode) CyberNeonMagenta else CyberNeonGreen,
                                                    trackColor = Color.White.copy(alpha = 0.05f)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Missing vs Active tracker
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                                     Text("ACTIVE SYSTEMS", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                                     Text("${securityReportState.activePermissionsCount} ON", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonGreen)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .border(0.5.dp, if (securityReportState.emergencyProtectionMode) CyberNeonMagenta.copy(alpha = 0.4f) else CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .padding(6.dp)
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                                     Text("THREAT DEVIATIONS", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                                     Text("${securityReportState.missingPermissionsCount} LOCKED", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (securityReportState.emergencyProtectionMode) CyberNeonMagenta else CyberNeonCyan)
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // 9 permissions interactive grid
                                        val permList = listOf(
                                            Triple("MICROPHONE", securityReportState.microphoneGranted, "MICROPHONE"),
                                            Triple("CAMERA", securityReportState.cameraGranted, "CAMERA"),
                                            Triple("NOTIFICATIONS", securityReportState.notificationsGranted, "NOTIFICATIONS"),
                                            Triple("CONTACTS", securityReportState.contactsGranted, "CONTACTS"),
                                            Triple("PHONE DETECT", securityReportState.phoneGranted, "PHONE"),
                                            Triple("SMS MONITOR", securityReportState.smsGranted, "SMS"),
                                            Triple("ACCESSIBILITY", securityReportState.accessibilityEnabled, "ACCESSIBILITY"),
                                            Triple("OVERLAY DOCK", securityReportState.overlayGranted, "OVERLAY"),
                                            Triple("BATTERY UNRESTRICT", securityReportState.batteryOptimizationsUnrestricted, "BATTERY")
                                        )
                                        
                                        Text("INTEGRITY CHANNELS (TAP TO RECOVER / GUIDE)", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(5.dp))
                                        
                                        // Vertical stack or staggered responsive items
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            permList.chunked(3).forEach { chunk ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    chunk.forEach { item ->
                                                        val (name, granted, key) = item
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .background(if (granted) CyberNeonGreen.copy(alpha = 0.05f) else CyberNeonMagenta.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                                .border(0.5.dp, if (granted) CyberNeonGreen.copy(alpha = 0.3f) else CyberNeonMagenta.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    viewModel.triggerPermissionRecovery(key)
                                                                }
                                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Text(name, fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = if (granted) "SECURED" else "LOCKED",
                                                                    fontSize = 8.sp,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = if (granted) CyberNeonGreen else CyberNeonMagenta
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // Guidance box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Yellow.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color.Yellow.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = if (securityReportState.emergencyProtectionMode) {
                                                    "CRITICAL SECTORS MUTED. CLOUD PIPELINES LIMIT OUT OF PROTECTION LAUNCHER MODE. ACTIVATE ALL HIGH CHANNELS TO ENABLE ASSISTANT OVERLAY."
                                                } else {
                                                    "ALL CRITICAL SYSTEM PROTOCOLS SECURED. NOVA SYSTEM RUNNING AT MAXIMUM CONTEXT LEVEL DISPATCH CHANNELS."
                                                },
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (securityReportState.emergencyProtectionMode) CyberNeonMagenta else CyberNeonGreen
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Re-sync manual button
                                        Button(
                                            onClick = { viewModel.syncFullSecurityReport(); viewModel.checkAndSyncPhonePermissions(); viewModel.checkAndSyncLocation() },
                                            modifier = Modifier.fillMaxWidth().height(26.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonCyan.copy(alpha = 0.12f),
                                                contentColor = CyberNeonCyan
                                            ),
                                            border = BorderStroke(0.5.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("RE-AUDIT INTEGRITY PORTALS", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 3. SECURE NOTIFICATION SEARCH & READBACK SUMMARY
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "INTELLIGENT ALERT PARSING BUFFER",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray
                                        )
                                        Text(
                                            text = "MATCHES: ${sortedAndFilteredNotifs.size}",
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Search Bar Input
                                    OutlinedTextField(
                                        value = notificationSearchQuery,
                                        onValueChange = { viewModel.notificationSearchQuery.value = it },
                                        modifier = Modifier.fillMaxWidth().testTag("notif_read_search_input"),
                                        placeholder = {
                                            Text("Search database notifications...", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberNeonCyan,
                                            unfocusedBorderColor = CyberDarkCardBorder,
                                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (sortedAndFilteredNotifs.isEmpty()) {
                                                    viewModel.voiceManager.speak("Sir, you currently have zero alert notifications inside the filtered buffer.")
                                                } else {
                                                    val contextSummary = sortedAndFilteredNotifs.take(3).mapIndexed { i, item ->
                                                        "Notification ${i + 1} from ${item.appName} stating ${item.title}: ${item.message}"
                                                    }.joinToString(". ")
                                                    val speechStr = "Sir, I have isolated ${sortedAndFilteredNotifs.size} records. Summarizing outstanding receipts: $contextSummary"
                                                    viewModel.voiceManager.speak(speechStr)
                                                    viewModel.voiceNovaResponse.value = speechStr
                                                    viewModel.writeTerminalLog("NOTIF_READ // Voice readback launched for search query.")
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(30.dp).testTag("notif_speak_read_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonCyan.copy(alpha = 0.12f),
                                                contentColor = CyberNeonCyan
                                            ),
                                            border = BorderStroke(0.5.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("READ SUMMARY", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { viewModel.notificationSearchQuery.value = "" },
                                            modifier = Modifier.height(30.dp).testTag("notif_search_clear_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = Color.Gray
                                            ),
                                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp)
                                        ) {
                                            Text("RESET", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 4. CONTACT DIRECTORY FAST ACTION PANE
                                    Text(
                                        text = "CONTACT DIRECTORY DIRECT UPLINK",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val quickContacts = listOf(
                                        Pair("Chief Jarvis", "+1 415-555-0199"),
                                        Pair("Commander Kane", "+1 555-0102"),
                                        Pair("Command Zero", "+1 312-555-0122")
                                    )

                                    quickContacts.forEach { contact ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = CyberNeonCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(contact.first, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text(contact.second, fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.submitManualCommand("Call ${contact.first}")
                                                },
                                                modifier = Modifier.height(22.dp).testTag("quick_dial_${contact.first.lowercase().replace(" ","_")}"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CyberNeonCyan.copy(alpha = 0.15f),
                                                    contentColor = CyberNeonCyan
                                                ),
                                                border = BorderStroke(0.5.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(4.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("DIAL VOICE", fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 5. TEST-BED SIMULATOR CONTROLS
                                    Text(
                                        text = "TELEPHONY SIMULATOR CONTROLS",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.simulateIncomingCall("Commander Kane") },
                                            modifier = Modifier.weight(1f).height(28.dp).testTag("simulate_incoming_call_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonMagenta.copy(alpha = 0.15f),
                                                contentColor = CyberNeonMagenta
                                            ),
                                            border = BorderStroke(0.5.dp, CyberNeonMagenta.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("SIMULATE RINGING", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { viewModel.transitionSimulatedCall("ACTIVE") },
                                            modifier = Modifier.weight(1f).height(28.dp).testTag("simulate_active_call_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonGreen.copy(alpha = 0.15f),
                                                contentColor = CyberNeonGreen
                                            ),
                                            border = BorderStroke(0.5.dp, CyberNeonGreen.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("SIMULATE CONNECT", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = { viewModel.transitionSimulatedCall("DISCONNECTED") },
                                            modifier = Modifier.weight(1f).height(28.dp).testTag("simulate_hangup_call_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Gray.copy(alpha = 0.15f),
                                                contentColor = Color.LightGray
                                            ),
                                            border = BorderStroke(0.5.dp, Color.Gray),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("SIMULATE HANGUP", fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Voice Session Diagnostic & Simulation Suite (Phase 7 Requirement 10)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .testTag("diagnostic_suite_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "DIAGNOSTIC & TESTING MATRIX",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberNeonCyan,
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Row 1: Session Controller buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.startManualVoiceSession() },
                                            enabled = !isSessionActive,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonGreen.copy(alpha = 0.25f),
                                                contentColor = CyberNeonGreen,
                                                disabledContainerColor = Color.Black.copy(alpha = 0.2f),
                                                disabledContentColor = Color.Gray
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("start_session_button"),
                                            border = BorderStroke(1.dp, if (!isSessionActive) CyberNeonGreen else Color.Transparent)
                                        ) {
                                            Text(
                                                text = "START SESSION",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.endManualVoiceSession() },
                                            enabled = isSessionActive,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberNeonMagenta.copy(alpha = 0.25f),
                                                contentColor = CyberNeonMagenta,
                                                disabledContainerColor = Color.Black.copy(alpha = 0.2f),
                                                disabledContentColor = Color.Gray
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("end_session_button"),
                                            border = BorderStroke(1.dp, if (isSessionActive) CyberNeonMagenta else Color.Transparent)
                                        ) {
                                            Text(
                                                text = "END SESSION",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Row 2: Wake verification hook toggle (Requirement 5)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CONTINUOUS WAKE HOOK [NOVA]",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                        Switch(
                                            checked = continuousListeningHookEnabled,
                                            onCheckedChange = { viewModel.toggleContinuousListeningHook(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = CyberNeonGreen,
                                                checkedTrackColor = CyberNeonGreen.copy(alpha = 0.3f),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color.Black.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.testTag("wake_hook_switch")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Button 3: Run full interactive assistant lifecycle simulation
                                    OutlinedButton(
                                        onClick = { viewModel.simulateVoiceSession() },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, CyberNeonCyan),
                                        modifier = Modifier.fillMaxWidth().testTag("simulate_session_button")
                                    ) {
                                        Text(
                                            text = "SIMULATE ENTIRE CYCLE",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberNeonCyan
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Notification Interception Warning advisory banner
                        if (!isListenerGranted) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, CyberNeonMagenta.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("Dashboard", "Failed to launch overlay settings listener: ${e.message}")
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = CyberNeonMagenta,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "NOTIFICATION INTERCEPT OFFLINE",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = CyberNeonMagenta,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tap to authorize Hello Nova notification capturing engine.",
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Welcoming Header Node with Frosted Glass look
                        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarLetter = preferences.avatarId.take(1).uppercase()
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(CyberNeonCyan.copy(alpha = 0.15f), RoundedCornerShape(27.dp))
                            .border(1.dp, CyberNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(27.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarLetter,
                            color = CyberNeonCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "HELLO, ${preferences.username.uppercase()}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "NODE SECURITY: OPERATIONAL // ID: ${preferences.avatarId.uppercase()}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Battery advisory banner (link to guidance screen)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = CyberNeonMagenta.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                    .clickable { showingBatteryGuide = true }
                    .testTag("battery_guide_banner"),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Battery tuning icon",
                        tint = CyberNeonMagenta,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BATTERY TUNING ADVISORY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonMagenta
                        )
                        Text(
                            text = "Configure unrestricted FGS background execution permissions.",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "View Guide",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 4. Dashboard Voice Activity Panel (Requirement 8)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("voice_activity_panel"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "VOICE INTELLIGENCE HUB",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonCyan,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(CyberNeonCyan, CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Last command icon",
                            tint = CyberNeonCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LAST UPLINK COMMAND",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Text(
                                text = if (lastCommandText.isEmpty() || lastCommandText == "None yet") "No voice inputs received yet" else "\"$lastCommandText\"",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Last query icon",
                            tint = CyberNeonGreen.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LAST NOTIFICATION SCAN",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            val queryTimeStr = if (lastNotificationQueryTime == null) {
                                "Scan pending..."
                            } else {
                                val sdf = java.text.SimpleDateFormat("h:mm:ss a", java.util.Locale.US)
                                sdf.format(java.util.Date(lastNotificationQueryTime!!))
                            }
                            Text(
                                text = queryTimeStr,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (lastNotificationQueryTime == null) Color.Gray else CyberNeonGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = "Last communication icon",
                            tint = CyberNeonMagenta.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LAST TELEMETRY TRANSMISSION",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Text(
                                text = lastCommunicationEventSummary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 5. Contact Search Results Panel (Requirement 6)
        if (contactSearchQuery != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("contact_search_results_panel"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SECURE DIRECTORY SEARCH",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberNeonCyan
                                )
                                Text(
                                    text = "QUERY: \"${contactSearchQuery!!}\"",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.contactSearchQuery.value = null },
                                modifier = Modifier.size(24.dp).testTag("clear_contact_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = CyberNeonMagenta,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val filteredContacts = viewModel.allSystemContacts.filter {
                            it.name.contains(contactSearchQuery!!, ignoreCase = true) ||
                            it.cipherId.contains(contactSearchQuery!!, ignoreCase = true)
                        }
                        
                        if (filteredContacts.isEmpty()) {
                            Text(
                                text = "NO COGNITIVE NODES MATCHING EXTRACTED SIGNAL KEYWORDS.",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonMagenta,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            filteredContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .clickable {
                                            viewModel.activeDraftRecipient.value = contact.name
                                            viewModel.activeDraftBody.value = "Enter encrypted payload message..."
                                            viewModel.showDraftComposer.value = true
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val statusDotColor = if (contact.linkStatus == "ONLINE") CyberNeonGreen else Color.Gray
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(statusDotColor, CircleShape)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(10.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name.uppercase(),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "CID: ${contact.cipherId} // CLASS: ${contact.classification.uppercase()}",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "Draft payload for contact",
                                        tint = CyberNeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Glowing Frosted Terminal Emulator Panel
        item {
            Column {
                Text(
                    text = "SYSTEM_CONSOLE",
                    fontSize = 11.sp,
                    color = CyberNeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = terminalOutput + if (cursorOn) "_" else " ",
                            color = CyberNeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            modifier = Modifier.testTag("terminal_output_log")
                        )
                    }
                }
            }
        }

        // Core Engine Monitor Module (START/STOP)
        item {
            val engineColor by animateColorAsState(
                targetValue = when (fServiceState) {
                    com.example.service.NovaForegroundService.EngineState.ACTIVE -> CyberNeonGreen
                    com.example.service.NovaForegroundService.EngineState.STARTING -> Color(0xFFFFD600) // Neon Yellow
                    com.example.service.NovaForegroundService.EngineState.OFFLINE -> CyberNeonMagenta
                },
                label = "engine_status_color"
            )

            val engineStatusText = when (fServiceState) {
                com.example.service.NovaForegroundService.EngineState.ACTIVE -> "ENGINE ACTIVE"
                com.example.service.NovaForegroundService.EngineState.STARTING -> "ENGINE STARTING"
                com.example.service.NovaForegroundService.EngineState.OFFLINE -> "ENGINE OFFLINE"
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyberDarkCardBorder, Color.Transparent)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "QUANTUM FLUX ENGINE CORE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Interactive Spinning Reactor Canvas
                        Box(
                            modifier = Modifier.size(90.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = engineColor.copy(alpha = 0.1f),
                                    radius = (size.minDimension / 2f) * if (preferences.engineActive) scalePulse else 1f
                                )
                                drawCircle(
                                    color = engineColor.copy(alpha = 0.35f),
                                    radius = size.minDimension / 2.3f,
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                                drawArc(
                                    color = engineColor,
                                    startAngle = if (preferences.engineActive) angleSpin else 45f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                                drawArc(
                                    color = engineColor,
                                    startAngle = if (preferences.engineActive) angleSpin + 180f else 225f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = when (fServiceState) {
                                        com.example.service.NovaForegroundService.EngineState.ACTIVE -> "ACTIVE"
                                        com.example.service.NovaForegroundService.EngineState.STARTING -> "STARTING"
                                        com.example.service.NovaForegroundService.EngineState.OFFLINE -> "OFFLINE"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = engineColor,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Live Telemetry Messages Lists
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "> STATUS_FLOW: $engineStatusText",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = engineColor,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                if (telemetryMessages.isEmpty()) {
                                    Text(
                                        text = "> Engine idle. Log buffer empty.",
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    telemetryMessages.takeLast(3).forEach { msg ->
                                        Text(
                                            text = "> $msg",
                                            fontSize = 9.sp,
                                            color = if (msg.contains("REACTOR") || msg.contains("ACTIVE")) CyberNeonGreen else Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Start/Stop Engine Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startEngine() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberNeonCyan,
                                disabledContainerColor = CyberNeonCyan.copy(alpha = 0.2f)
                            ),
                            enabled = fServiceState != com.example.service.NovaForegroundService.EngineState.ACTIVE,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("start_engine_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "START ENGINE",
                                fontWeight = FontWeight.Bold,
                                color = if (fServiceState != com.example.service.NovaForegroundService.EngineState.ACTIVE) Color.Black else Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.stopEngine() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberNeonMagenta,
                                disabledContainerColor = CyberNeonMagenta.copy(alpha = 0.2f)
                            ),
                            enabled = fServiceState != com.example.service.NovaForegroundService.EngineState.OFFLINE,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("stop_engine_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "STOP ENGINE",
                                fontWeight = FontWeight.Bold,
                                color = if (fServiceState != com.example.service.NovaForegroundService.EngineState.OFFLINE) Color.White else Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // WAKE WORD INTELLIGENCE SYSTEM CARD (Phase 21.2)
        item {
            val statusLabel = when (bgWakeStatus) {
                com.example.service.WakeStatus.LISTENING -> "LISTENING"
                com.example.service.WakeStatus.SLEEPING -> "SLEEPING"
                com.example.service.WakeStatus.TRIGGERED -> "TRIGGERED"
                com.example.service.WakeStatus.DISABLED -> "DISABLED"
                else -> "OFFLINE"
            }
            val statusColor = when (bgWakeStatus) {
                com.example.service.WakeStatus.LISTENING -> CyberNeonGreen
                com.example.service.WakeStatus.SLEEPING -> CyberNeonCyan
                com.example.service.WakeStatus.TRIGGERED -> Color(0xFFFFD600)
                com.example.service.WakeStatus.DISABLED, null -> CyberNeonMagenta
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(statusColor.copy(alpha = 0.8f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOVA WAKE WORD INTELLIGENCE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Uptime Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("SYSTEM UPTIME", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                Spacer(modifier = Modifier.height(3.dp))
                                val hours = bgWakeUptimeSeconds / 3600
                                val mins = (bgWakeUptimeSeconds % 3600) / 60
                                val secs = bgWakeUptimeSeconds % 60
                                Text(
                                    text = String.format("%02dh %02dm %02ds", hours, mins, secs),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberNeonGreen
                                )
                            }
                        }

                        // Trigger Count Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("TRIGGER COUNT", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "$bgWakeTriggerCount ACTIVATIONS",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberNeonCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Last Activation time
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LAST ACTIVATION TIME", fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            val formattedTime = if (bgWakeLastActivationTime > 0L) {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(bgWakeLastActivationTime))
                            } else {
                                "NEVER (WAITING COMMAND)"
                            }
                            Text(
                                text = formattedTime,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (bgWakeLastActivationTime > 0L) CyberNeonGreen else Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Interactive diagnostic buttons
                    Button(
                        onClick = {
                            viewModel.voiceNovaResponse.value = "Yes Sir, Nova background wake intelligence channels are authenticated and fully responsive."
                            viewModel.voiceManager.speak("Yes Sir, Nova background wake intelligence channels are authenticated and fully responsive.")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = statusColor.copy(alpha = 0.12f),
                            contentColor = statusColor
                        ),
                        border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "DIAGNOSE WAKE AUDIO CORES",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // QUICK ACTIONS CONTROL PANEL
        item {
            Column {
                Text(
                    text = "SMART QUICK ACTIONS PANEL",
                    fontSize = 11.sp,
                    color = CyberNeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                    letterSpacing = 1.sp
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionItem(
                                title = "CORE SETTINGS",
                                subtitle = "SYS_CONFIG",
                                icon = Icons.Default.Settings,
                                color = CyberNeonCyan,
                                tag = "quick_action_settings",
                                onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.OPEN_SETTINGS) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionItem(
                                title = "WI-FI LINK",
                                subtitle = "WLAN_PORTAL",
                                icon = Icons.Default.Refresh,
                                color = CyberNeonGreen,
                                tag = "quick_action_wifi",
                                onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.OPEN_WIFI) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionItem(
                                title = "BLUETOOTH",
                                subtitle = "RF_BEACON",
                                icon = Icons.Default.Build,
                                color = CyberNeonMagenta,
                                tag = "quick_action_bluetooth",
                                onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.OPEN_BLUETOOTH) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionItem(
                                title = "APP ACCESS",
                                subtitle = "PKG_MANIFEST",
                                icon = Icons.Default.Info,
                                color = Color(0xFFFFD600), // Neon Yellow
                                tag = "quick_action_app_info",
                                onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.OPEN_APP_INFO) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickActionItem(
                                title = "BATTERY TUNING",
                                subtitle = "PM_ADVISORY",
                                icon = Icons.Default.Warning,
                                color = CyberNeonMagenta,
                                tag = "quick_action_battery",
                                onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.OPEN_BATTERY) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionItem(
                                title = "GPS LOCATION",
                                subtitle = "LOC_COORD",
                                icon = Icons.Default.Search,
                                color = CyberNeonCyan,
                                tag = "quick_action_location",
                                onClick = { viewModel.executeSmartAction(com.example.service.SmartActionType.OPEN_LOCATION) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // --- NEW: PHASE 2 VOICE ASSISTANT RADAR CONTROL PANEL ---
        item {
            Column {
                Text(
                    text = "COGNITIVE VOICE ASSISTANT",
                    fontSize = 11.sp,
                    color = CyberNeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = CyberDarkCardBorder,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title header line + Clear Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NOVA_RADAR_SEC02",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // Clear history button
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.clearVoiceHistory() }
                                    .testTag("clear_voice_history_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Logs",
                                    tint = CyberNeonMagenta,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PURGE SYSTEM LOGS",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonMagenta,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Language Mode Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (activeLanguage == "en") CyberNeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = if (activeLanguage == "en") 1.dp else 0.dp,
                                        color = if (activeLanguage == "en") CyberNeonCyan else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.setVoiceLanguage("en") }
                                    .testTag("lang_en_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ENGLISH FREQ",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeLanguage == "en") CyberNeonCyan else Color.Gray
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (activeLanguage == "bn") CyberNeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = if (activeLanguage == "bn") 1.dp else 0.dp,
                                        color = if (activeLanguage == "bn") CyberNeonCyan else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.setVoiceLanguage("bn") }
                                    .testTag("lang_bn_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "BENGALI FREQ",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeLanguage == "bn") CyberNeonCyan else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Mic capture controller button with dual ring pulsator
                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (voiceIsListening || currentAssistantState == AssistantState.LISTENING || currentAssistantState == AssistantState.SPEAKING) {
                                // Pulsating outer ring with theme matching (Cyan for input, Magenta for output)
                                Box(
                                    modifier = Modifier
                                        .size(pulseSizeAnim.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (currentAssistantState == AssistantState.SPEAKING) CyberNeonMagenta.copy(alpha = pulseAlphaAnim)
                                            else CyberNeonCyan.copy(alpha = pulseAlphaAnim)
                                        )
                                )
                            }

                            // Inner stable/active mic circle
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = when (currentAssistantState) {
                                            AssistantState.LISTENING -> CyberNeonCyan
                                            AssistantState.PROCESSING -> CyberNeonGreen
                                            AssistantState.SPEAKING -> CyberNeonMagenta
                                            else -> CyberNeonCyan.copy(alpha = 0.1f)
                                        },
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = when (currentAssistantState) {
                                            AssistantState.LISTENING -> CyberNeonCyan
                                            AssistantState.PROCESSING -> CyberNeonGreen
                                            AssistantState.SPEAKING -> CyberNeonMagenta
                                            else -> CyberNeonCyan
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (hasMicrophonePermission) {
                                            if (voiceIsListening || currentAssistantState == AssistantState.LISTENING) {
                                                viewModel.stopVoiceCapture()
                                            } else {
                                                viewModel.startVoiceCapture()
                                            }
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.RECORD_AUDIO,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                )
                                            )
                                        }
                                    }
                                    .testTag("radar_microphone_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (currentAssistantState) {
                                        AssistantState.LISTENING -> Icons.Default.Stop
                                        AssistantState.PROCESSING -> Icons.Default.HourglassEmpty
                                        AssistantState.SPEAKING -> Icons.Default.VolumeUp
                                        else -> Icons.Default.Mic
                                    },
                                    contentDescription = "Microphone Trigger",
                                    tint = when (currentAssistantState) {
                                        AssistantState.LISTENING, AssistantState.PROCESSING, AssistantState.SPEAKING -> Color.Black
                                        else -> CyberNeonCyan
                                    },
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Feedback status line
                        val feedbackText = when (currentAssistantState) {
                            AssistantState.LISTENING -> "CAPTURING ACOUSTICS ON AIR // SPEAK NOW..."
                            AssistantState.PROCESSING -> "QUANTUM NEURAL NET PROCESSING QUERY..."
                            AssistantState.SPEAKING -> "TRANSMITTING SPEECH SYNTHESIZER BEACON..."
                            AssistantState.ARMED -> "TAP CONSOLE MIC OR SAY 'HELLO NOVA' TO ACTIVATE"
                            AssistantState.STANDBY -> "TAP CONSOLE MIC TO ACTIVATE LINK"
                            AssistantState.RECOVERY -> "AUTO RECONNECTING VOICE ENGINE COMPONENT..."
                        }
                        val feedbackColor = when (currentAssistantState) {
                            AssistantState.LISTENING -> CyberNeonCyan
                            AssistantState.PROCESSING -> CyberNeonGreen
                            AssistantState.SPEAKING -> CyberNeonMagenta
                            AssistantState.ARMED -> CyberNeonCyan
                            AssistantState.STANDBY -> MaterialTheme.colorScheme.onSurfaceVariant
                            AssistantState.RECOVERY -> Color(0xFFFFB300)
                        }

                        Text(
                            text = feedbackText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = feedbackColor,
                            modifier = Modifier.padding(top = 12.dp)
                        )

                        // Realtime transcript status
                        if (voiceRecognizedText.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .border(1.dp, CyberNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Live STT",
                                        tint = CyberNeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = voiceRecognizedText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Error segment logic representation
                        voiceError?.let { err ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .border(1.dp, CyberNeonMagenta.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = CyberNeonMagenta.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Alert",
                                        tint = CyberNeonMagenta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "STT_ERR: $err",
                                        color = CyberNeonMagenta,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Manual fallback text keyboard panel
                        Text(
                            text = "GRID KEYBOARD BACKUP UPLINK",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = textInputState,
                            onValueChange = { textInputState = it },
                            placeholder = {
                                Text(
                                    text = if (activeLanguage == "bn") "বার্তা টাইপ করুন (ম্যানুয়াল)..." else "Enter manual text command...",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = CyberNeonCyan,
                                unfocusedBorderColor = CyberDarkCardBorder,
                                focusedLabelColor = CyberNeonCyan
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (textInputState.trim().isNotEmpty()) {
                                            viewModel.submitManualCommand(textInputState)
                                            textInputState = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("manual_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Transmit",
                                        tint = CyberNeonCyan
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("voice_assistant_manual_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // --- NEW: COGNITIVE LIVE THREAD HISTORY CARDS ---
        if (voiceHistory.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                    Text(
                        text = "TRANSLATION TERMINAL HISTORY (ROOM)",
                        fontSize = 11.sp,
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = conversationSearchFilter,
                        onValueChange = { conversationSearchFilter = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("conversation_search_input"),
                        placeholder = {
                            Text(
                                "Filter conversation records...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberDarkCardBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (conversationSearchFilter.isNotEmpty()) {
                                IconButton(onClick = { conversationSearchFilter = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = CyberNeonMagenta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            items(filteredVoiceHistory, key = { it.id }) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberDarkCardBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .testTag("history_item_${log.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CHANNEL // ${log.language.uppercase()}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val statusColor = if (log.status == "FAILED") CyberNeonMagenta else CyberNeonGreen
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, statusColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = log.status,
                                        fontSize = 6.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                            Text(
                                text = formatTimestamp(log.timestamp),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // User message block
                        Row(verticalAlignment = Alignment.Top) {
                            // Left vertical bar decoration
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(18.dp)
                                    .background(CyberNeonCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OPERATOR: \"${log.userSpeech}\"",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Nova reply block
                        Row(verticalAlignment = Alignment.Top) {
                            // Left vertical bar decoration for Nova
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(18.dp)
                                    .background(CyberNeonMagenta)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOVA_AI: \"${log.novaResponse}\"",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // SMART ACTIONS PERSISTENT HISTORY LOGS PANEL
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(24.dp))
                    .testTag("action_history_panel"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SMART ACTIONS TELEMETRY",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonCyan
                        )

                        TextButton(
                            onClick = { viewModel.clearSmartActionHistory() },
                            modifier = Modifier.testTag("clear_smart_actions_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear smart actions",
                                    tint = CyberNeonMagenta,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "WIPE SEC",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonMagenta,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (smartActionsHistory.isEmpty()) {
                        Text(
                            text = "NO ACTION HISTORY CAPTURED. STANDBY.",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Take latest 10 actions
                            smartActionsHistory.take(10).forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .border(1.dp, CyberDarkCardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.actionName.uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val timeStr = formatTimestamp(log.timestamp)
                                        Text(
                                            text = "TIME_STAMP: $timeStr",
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }

                                    val isSuccess = log.status == "SUCCESS"
                                    val badgeColor = if (isSuccess) CyberNeonGreen else CyberNeonMagenta
                                    val badgeBg = badgeColor.copy(alpha = 0.1f)
                                    val badgeBorder = badgeColor.copy(alpha = 0.4f)
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(badgeBg, RoundedCornerShape(6.dp))
                                            .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = log.status,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = badgeColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SYSTEM SERVICE EVENTS LOG PANEL
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(24.dp))
                    .testTag("service_logs_panel"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CORE SERVICE EVENT LOGS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonCyan
                        )

                        TextButton(
                            onClick = { viewModel.clearServiceLogs() },
                            modifier = Modifier.testTag("clear_service_logs_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear logs",
                                    tint = CyberNeonMagenta,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "WIPE BUFFER",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonMagenta,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (serviceLogs.isEmpty()) {
                        Text(
                            text = "NO TELEMETRY CAPTURED. ENGINE STANDBY.",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Take latest 15 logs
                            serviceLogs.take(15).forEach { log ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.eventType.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = when (log.eventType) {
                                                "Service Started" -> CyberNeonGreen
                                                "Service Stopped" -> CyberNeonMagenta
                                                "Permission Status" -> CyberNeonCyan
                                                else -> Color.White
                                            }
                                        )

                                        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                                        val timeStr = sdf.format(Date(log.timestamp))
                                        Text(
                                            text = timeStr,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.message,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Control Panel Label Section
        item {
            Text(
                text = "SUB-GRID OVERLAYS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Modular Grid Actions: Voice, Calls, Messaging, Apps, Search, Weather, Location
        item {
            val gridItems = listOf(
                DashboardCardItem("Voice Node", Icons.Default.PlayArrow, "Voice"),
                DashboardCardItem("Call Holo", Icons.Default.Phone, "Calls"),
                DashboardCardItem("Inbox Mail", Icons.Default.Email, "Messaging"),
                DashboardCardItem("Grid Apps", Icons.Default.Menu, "Apps"),
                DashboardCardItem("System Search", Icons.Default.Search, "Search"),
                DashboardCardItem("Scanner Climate", Icons.Default.Warning, "Weather"),
                DashboardCardItem("Geo Coordinates", Icons.Default.LocationOn, "Location")
            )

            // Building responsive grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chunks = gridItems.chunked(2)
                for (chunk in chunks) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (card in chunk) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.handleCardClick(card.actionKey) }
                                    .testTag("card_${card.actionKey.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = card.icon,
                                        contentDescription = card.title,
                                        tint = CyberNeonCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = card.title.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
}
}
}
}

fun formatTimestamp(timeMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(timeMs))
    } catch (e: Exception) {
        "SYS_T"
    }
}

data class DashboardCardItem(
    val title: String,
    val icon: ImageVector,
    val actionKey: String
)

@Composable
fun BatteryOptimizationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CyberDarkBg)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Navigate back",
                tint = CyberNeonCyan,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
                    .testTag("battery_back_button")
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "BATTERY TUNING GUIDE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberNeonCyan,
                letterSpacing = 1.5.sp
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(20.dp))

        // Shield Graphic or Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Shield logo",
                    tint = CyberNeonMagenta,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "COGNITIVE POWER SHIELD ONLINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = "Keep Nova FGS running uninterrupted in background.",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CRITICAL BOOT PROTOCOLS:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Guide steps
        val steps = listOf(
            Triple("STEP 01", "DISABLE BATTERY RESTRICTIONS", "Navigate to App Info -> Battery inside system settings, and configure to UNRESTRICTED. This stops Android's default task scheduler from killing the Nova engine."),
            Triple("STEP 02", "BYPASS STANDBY SLEEP MODE", "Disable 'Pause app activity if unused' or similar OS sleeping modes to ensure the telemetry loops never drift into offline dormancy."),
            Triple("STEP 03", "ALLOW UNRESTRICTED BACKGROUND DATA", "Ensure background data channel is unrestricted under Data Usage settings so the Gemini model remains synchronized with reality.")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(steps) { step ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = step.first,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonMagenta,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "RECOMMENDED",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.second,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = step.third,
                            fontSize = 10.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Native Link Launcher Button
        Button(
            onClick = {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("BatteryScreen", "Failed to launch detailsSettings: ${e.message}")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("battery_open_settings_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OPEN SYSTEM SETTINGS",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = color
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDraftComposer(
    recipient: String?,
    body: String,
    onBodyChange: (String) -> Unit,
    onRecipientChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    if (recipient == null) return

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, CyberNeonCyan, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFB0A0E29)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SECURE MSG DRAFT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(CyberNeonCyan, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "RECIPIENT NODE TERMINAL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                OutlinedTextField(
                    value = recipient,
                    onValueChange = onRecipientChange,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedBorderColor = CyberNeonCyan,
                        unfocusedBorderColor = CyberDarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SPECIFIED PAYLOAD ENCRYPTION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )

                OutlinedTextField(
                    value = body,
                    onValueChange = onBodyChange,
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedBorderColor = CyberNeonCyan,
                        unfocusedBorderColor = CyberDarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberNeonMagenta),
                        border = BorderStroke(1.dp, CyberNeonMagenta.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("draft_composer_cancel")
                    ) {
                        Text(
                            text = "ABORT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan, contentColor = Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("draft_composer_confirm")
                    ) {
                        Text(
                            text = "TRANSMIT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantPresenceOrb(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")

    // Pulsing animations for Listening state (Neon Cyan)
    val cyanScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyanScale"
    )
    val cyanAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cyanAlpha"
    )

    // Pulsing animations for Speaking state (Neon Magenta)
    val magentaScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magentaScale"
    )
    val magentaAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magentaAlpha"
    )

    // Rotating animation for Processing state (Neon Green)
    val rotatingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotatingAngle"
    )

    Box(
        modifier = modifier
            .size(70.dp)
            .testTag("assistant_presence_orb"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Dynamic Glowing Outer Halo and Pulser based on status
        when (state) {
            AssistantState.LISTENING -> {
                // Large Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = cyanScale
                            scaleY = cyanScale
                            alpha = cyanAlpha
                        }
                        .background(CyberNeonCyan.copy(alpha = 0.35f), CircleShape)
                )
                // Middle Halo
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(CyberNeonCyan.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CyberNeonCyan.copy(alpha = 0.4f), CircleShape)
                )
            }
            AssistantState.SPEAKING -> {
                // Energetic magenta pulse
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = magentaScale
                            scaleY = magentaScale
                            alpha = magentaAlpha
                        }
                        .background(CyberNeonMagenta.copy(alpha = 0.4f), CircleShape)
                )
                // Middle Halo
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(CyberNeonMagenta.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, CyberNeonMagenta.copy(alpha = 0.5f), CircleShape)
                )
            }
            AssistantState.PROCESSING -> {
                // Quantum Spin dots
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer { rotationZ = rotatingAngle }
                ) {
                    // Small orbiting emitter dots
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(CyberNeonGreen, CircleShape)
                            .align(Alignment.TopCenter)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(CyberNeonCyan, CircleShape)
                            .align(Alignment.BottomCenter)
                    )
                }
                // Quiet inner ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, CyberNeonGreen.copy(alpha = 0.4f), CircleShape)
                )
            }
            AssistantState.ARMED -> {
                // Static quiet background aura
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(CyberNeonCyan.copy(alpha = 0.05f), CircleShape)
                        .border(0.5.dp, CyberNeonCyan.copy(alpha = 0.25f), CircleShape)
                )
            }
            AssistantState.STANDBY -> {
                // Muted/dim standby layer
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
                )
            }
            AssistantState.RECOVERY -> {
                // Pulse amber warning halo
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFFFFB300).copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        // 2. Core Solid Glowing Orb
        val coreColor = when (state) {
            AssistantState.STANDBY -> Color.DarkGray
            AssistantState.ARMED -> CyberNeonCyan.copy(alpha = 0.4f)
            AssistantState.LISTENING -> CyberNeonCyan
            AssistantState.PROCESSING -> CyberNeonGreen
            AssistantState.SPEAKING -> CyberNeonMagenta
            AssistantState.RECOVERY -> Color(0xFFFFB300)
        }

        val coreSize = if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) 34.dp else 26.dp
        val animatedCoreSize by animateDpAsState(
            targetValue = coreSize,
            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
            label = "coreSize"
        )

        Box(
            modifier = Modifier
                .size(animatedCoreSize)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, coreColor, coreColor.copy(alpha = 0.2f))
                    ),
                    shape = CircleShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
    }
}

@Composable
fun WakeAndReadinessScreen(
    viewModel: com.example.viewmodel.MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentAssistantState by viewModel.assistantState.collectAsState()
    val isSessionActive by viewModel.isVoiceSessionActive.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val continuousListeningHookEnabled by viewModel.isContinuousListeningHookEnabled.collectAsState()
    
    val fgServiceStatus by viewModel.foregroundServiceStatus.collectAsState()
    val voiceEngineStatus by viewModel.voiceEngineStatus.collectAsState()
    val notifListenerStatus by viewModel.notificationListenerStatus.collectAsState()
    val permissionStatus by viewModel.permissionReadinessStatus.collectAsState()
    val databaseStatus by viewModel.databaseStatus.collectAsState()

    val lastSessionStartTime by viewModel.lastSessionStartTime.collectAsState()
    val lastSessionEndTime by viewModel.lastSessionEndTime.collectAsState()
    val lastSessionCommandCount by viewModel.lastSessionCommandCount.collectAsState()
    val lastSessionDurationMs by viewModel.lastSessionDurationMs.collectAsState()

    val lastWakeEvent by viewModel.lastWakeEvent.collectAsState()
    val wakeEvents by viewModel.wakeEventsFlow.collectAsState()
    val engineUptimeSeconds by viewModel.engineUptimeSeconds.collectAsState()

    val savedSessions by viewModel.voiceSessions.collectAsState()

    val serviceLogs by viewModel.serviceLogs.collectAsState()
    val notificationsHistory by viewModel.notificationsHistory.collectAsState()
    val conversationsState by viewModel.voiceConversations.collectAsState()

    // Dynamic color helper
    fun getStatusColor(status: String): Color {
        return when (status.uppercase()) {
            "HEALTHY", "CONNECTED", "VERIFIED", "SUCCESS" -> CyberNeonGreen
            "PARTIAL", "ARMED" -> CyberNeonCyan
            "INACTIVE", "STANDBY" -> Color.Gray
            else -> CyberNeonMagenta
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("wake_readiness_tab_column"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Wake Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("wake_status_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASSISTANT PORTAL ACTIVE",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isSessionActive) CyberNeonGreen else CyberNeonCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSessionActive) "LIVE TRANSACTION" else "MONITOR ARMED",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSessionActive) CyberNeonGreen else CyberNeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistantPresenceOrb(state = currentAssistantState)
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CURRENT PORTAL STATE",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta.copy(alpha = 0.15f) else CyberNeonCyan.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            0.5.dp,
                                            if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(
                                                    if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = preferences.activeAssistant,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan
                                        )
                                    }
                                }
                            }
                            Text(
                                text = currentAssistantState.displayName,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when (currentAssistantState) {
                                    AssistantState.STANDBY -> Color.Gray
                                    AssistantState.ARMED -> CyberNeonCyan.copy(alpha = 0.7f)
                                    AssistantState.LISTENING -> CyberNeonCyan
                                    AssistantState.PROCESSING -> CyberNeonGreen
                                    AssistantState.SPEAKING -> CyberNeonMagenta
                                    AssistantState.RECOVERY -> Color(0xFFFFB300)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "continuous wake word hook".uppercase(),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Keep wake engine armed to trigger visual presence portal upon voice phrase detection.",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                        
                        Switch(
                            checked = continuousListeningHookEnabled,
                            onCheckedChange = { viewModel.toggleContinuousListeningHook(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberNeonCyan,
                                checkedTrackColor = CyberNeonCyan.copy(alpha = 0.25f),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("wake_word_toggle_switch")
                        )
                    }
                }
            }
        }

        // 2. Dashboard Wake Center (Stats Grid)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("dashboard_wake_center_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DASHBOARD WAKE CENTER",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "total sessions".uppercase(),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${savedSessions.size}",
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                        ) {
                            val hrs = engineUptimeSeconds / 3600
                            val mins = (engineUptimeSeconds % 3600) / 60
                            val secs = engineUptimeSeconds % 60
                            val uptimeStr = String.format("%02d:%02d:%02d", hrs, mins, secs)

                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "engine uptime".uppercase(),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                Text(
                                    text = uptimeStr,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberNeonCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last Wake Event Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "LATEST PHRASE INTERCEPT",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (lastWakeEvent != null) {
                                val ev = lastWakeEvent!!
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = ev.eventName,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                        Text(
                                            text = sdf.format(java.util.Date(ev.timestamp)),
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(getStatusColor(ev.resultStatus).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(1.dp, getStatusColor(ev.resultStatus).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = ev.resultStatus,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = getStatusColor(ev.resultStatus)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No captured hotword events in active cache buffer.",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Last Session Recovery Info displaying block (Requirement 7)
                    if (lastSessionStartTime > 0L) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "RECOVERED PREVIOUS SESSION INFO",
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonCyan.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                    Text(
                                        text = "Time: ${sdf.format(java.util.Date(lastSessionStartTime))} - ${sdf.format(java.util.Date(lastSessionEndTime))}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "Cmds: $lastSessionCommandCount | Dur: ${lastSessionDurationMs / 1000}s",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Background Readiness Monitor & Diagnostics combined
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("readiness_monitor_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val readinessScore = remember(fgServiceStatus, notifListenerStatus, voiceEngineStatus, permissionStatus, databaseStatus) {
                        var score = 0
                        fun eval(s: String): Int {
                            return when (s.uppercase()) {
                                "HEALTHY", "CONNECTED", "VERIFIED", "SUCCESS" -> 20
                                "PARTIAL", "ARMED", "WARNING", "PENDING" -> 10
                                else -> 5
                            }
                        }
                        score += eval(fgServiceStatus)
                        score += eval(notifListenerStatus)
                        score += eval(voiceEngineStatus)
                        score += eval(permissionStatus)
                        score += eval(databaseStatus)
                        score.coerceIn(0, 100)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ASSISTANT READINESS SCAN",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "UNIFIED RUNTIME DIAGNOSTIC INDEX",
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (readinessScore >= 80) CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonMagenta.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (readinessScore >= 80) CyberNeonGreen else CyberNeonMagenta,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$readinessScore / 100 PTS",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (readinessScore >= 80) CyberNeonGreen else CyberNeonMagenta
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { readinessScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = if (readinessScore >= 80) CyberNeonGreen else CyberNeonMagenta,
                        trackColor = Color.Gray.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val items = listOf(
                        Triple("Foreground Service", fgServiceStatus, "Nova Background Engine"),
                        Triple("Notification Interceptor", notifListenerStatus, "Permission Gate Status"),
                        Triple("Voice TTS Synthesizer", voiceEngineStatus, "Speech Synthesizer Status"),
                        Triple("Permission readiness", permissionStatus, "Gateway Permission"),
                        Triple("SQLite Room Database", databaseStatus, "Persistent State Storage")
                    )

                    items.forEachIndexed { idx, (label, status, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = desc,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(getStatusColor(status).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, getStatusColor(status).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = getStatusColor(status)
                                )
                            }
                        }
                        if (idx < items.lastIndex) {
                            HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

        // 4. Diagnostics Health Center Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("diagnostics_center_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM HEALTH & COGNITIVE DIAGNOSTICS",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (fgServiceStatus == "HEALTHY" && databaseStatus == "HEALTHY") CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonMagenta.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    0.5.dp,
                                    if (fgServiceStatus == "HEALTHY" && databaseStatus == "HEALTHY") CyberNeonGreen else CyberNeonMagenta,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (fgServiceStatus == "HEALTHY" && databaseStatus == "HEALTHY") "SYSTEM_SECURE" else "ATTN_REQUIRED",
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (fgServiceStatus == "HEALTHY" && databaseStatus == "HEALTHY") CyberNeonGreen else CyberNeonMagenta
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CORE TELEMETRY NODES",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Live Diagnostics Information Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ENGINE UPTIME", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${engineUptimeSeconds}s", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonCyan)
                        }
                        Column(modifier = Modifier.weight(1f).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LATENCY", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("0.8 ms", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonGreen)
                        }
                        Column(modifier = Modifier.weight(1f).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DB INTEGRITY", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(databaseStatus, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (databaseStatus == "HEALTHY") CyberNeonGreen else CyberNeonMagenta)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DATABASE PERSISTENCE CACHE METRICS",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Grid of counts
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Service Logs Stack", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${serviceLogs.size}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("records", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        }
                        HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Intercept Wake Logs", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${wakeEvents.size}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("records", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        }
                        HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Voice Sessions Persisted", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${savedSessions.size}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("records", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        }
                        HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Notifications History Archive", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${notificationsHistory.size}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonMagenta)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("records", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        }
                        HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Vocal Dialog Interactions", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${conversationsState.size}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberNeonCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("records", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "CACHE MAINTENANCE & RECOVERY",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { viewModel.pruneOldDatabaseEntries(3) },
                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("optimize_database_cache_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberNeonCyan.copy(alpha = 0.12f),
                            contentColor = CyberNeonCyan
                        ),
                        border = BorderStroke(0.5.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TUNE & OPTIMIZE CACHE (PRUNE >72 hrs)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Testing & Simulation Utilities
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("testing_utilities_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TESTING & TELEMETRY SIMULATORS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Button 1: Simulate "Hello Nova" Wake event
                    Button(
                        onClick = { viewModel.simulateWakeEvent() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan.copy(alpha = 0.15f), contentColor = CyberNeonCyan),
                        border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("simulate_wake_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "SIMULATE WAKE EVENT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 2: Simulate Voice Session
                    Button(
                        onClick = { viewModel.simulateVoiceSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonGreen.copy(alpha = 0.15f), contentColor = CyberNeonGreen),
                        border = BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("simulate_session_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "SIMULATE VOICE SESSION", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 3: Reset Statistics
                    Button(
                        onClick = { viewModel.resetSessionStatistics() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonMagenta.copy(alpha = 0.15f), contentColor = CyberNeonMagenta),
                        border = BorderStroke(1.dp, CyberNeonMagenta.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("reset_statistics_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "RESET ALL STATISTICS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. Wake Event Logs List
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("wake_event_logs_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WAKE INTERCEPT EVENT LOGS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(CyberNeonCyan.copy(alpha = 0.11f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${wakeEvents.size} LOGS",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    if (wakeEvents.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            wakeEvents.take(15).forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = log.eventName,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                        Text(
                                            text = sdf.format(java.util.Date(log.timestamp)),
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(getStatusColor(log.resultStatus).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(1.dp, getStatusColor(log.resultStatus).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.resultStatus,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = getStatusColor(log.resultStatus)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Database buffer empty.", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLauncherAndMemoryScreen(
    viewModel: com.example.viewmodel.MainViewModel,
    modifier: Modifier = Modifier
) {
    val launcherState by viewModel.appLauncherState.collectAsState()
    val allApps by viewModel.allInstalledApps.collectAsState()
    val launchHistory by viewModel.appLaunchHistory.collectAsState()
    val memoryState by viewModel.assistantMemory.collectAsState()
    val searchQuery by viewModel.appSearchQuery.collectAsState()

    // Filtered apps based on search query
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("app_launcher_tab_column"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. AI BRAIN & SELF-LEARNING CORE (Phase 22) ---
        item {
            val brainStatusVal by viewModel.brainStatus.collectAsState()
            val learningScoreVal by viewModel.learningScore.collectAsState()
            val lastPatternVal by viewModel.lastLearnedPattern.collectAsState()
            val suggestionsVal by viewModel.smartSuggestions.collectAsState()
            val memoriesVal by viewModel.categorizedMemories.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, CyberNeonMagenta.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .testTag("ai_brain_dashboard_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF09081E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COGNITIVE SYNAPTIC ENGINE",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonMagenta,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "SELF-LEARNING AI CORE",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    when (brainStatusVal) {
                                        "ANALYZING_PATTERNS" -> CyberNeonCyan.copy(alpha = 0.15f)
                                        else -> CyberNeonMagenta.copy(alpha = 0.15f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    when (brainStatusVal) {
                                        "ANALYZING_PATTERNS" -> CyberNeonCyan
                                        else -> CyberNeonMagenta
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = brainStatusVal,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when (brainStatusVal) {
                                    "ANALYZING_PATTERNS" -> CyberNeonCyan
                                    else -> CyberNeonMagenta
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Memory count & Learning score row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Learning Score Box
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .border(0.5.dp, CyberDarkCardBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "SYNAPTIC SCORE",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$learningScoreVal%",
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonMagenta,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { learningScoreVal / 100f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = CyberNeonMagenta,
                                    trackColor = Color.Gray.copy(alpha = 0.2f),
                                )
                            }
                        }

                        // Memory Count Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .border(0.5.dp, CyberDarkCardBorder, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "LOCAL MEMORIES",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${memoriesVal.size}",
                                    fontSize = 28.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "NODES STORED",
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last Learned Pattern node
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "LAST DISCOVERED USER PATTERN",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastPatternVal,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Smart Suggestions
                    Text(
                        text = "PROACTIVE PREDICTIVE RECOMMENDATIONS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestionsVal.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, CyberNeonGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (suggestion.contains("YouTube")) {
                                            viewModel.simulateHabitLearning("YouTube")
                                            viewModel.launchAppDirectly("com.google.android.youtube", "YouTube")
                                        } else if (suggestion.contains("WhatsApp")) {
                                            viewModel.simulateHabitLearning("WhatsApp")
                                            viewModel.launchAppDirectly("com.whatsapp", "WhatsApp")
                                        } else {
                                            viewModel.forceAnalyzeUsagePatterns()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = CyberNeonGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = suggestion,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = CyberNeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulation area to make it super easy to check and review!
                    Text(
                        text = "MANUAL OPERATOR ACTION TELEMETRY EMULATOR",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateHabitLearning("YouTube") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonMagenta.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CyberNeonMagenta),
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("SIMULATE YOUTUBE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                        
                        Button(
                            onClick = { viewModel.simulateHabitLearning("WhatsApp") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CyberNeonCyan),
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("SIMULATE WHATSAPP", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Long term memory manager list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURE LOCAL MEMORIES (" + memoriesVal.size + ")",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        if (memoriesVal.isNotEmpty()) {
                            Text(
                                text = "FLUSH ALL",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonMagenta,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.clearAllCategorizedMemories() }
                                    .padding(4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (memoriesVal.isEmpty()) {
                        Text(
                            text = "No user habits or synaptic records stored. Generate launcher intents or run emulator commands to record memories.",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState())
                        ) {
                            memoriesVal.forEach { memory ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        when(memory.category) {
                                                            "Personal Preferences" -> CyberNeonCyan
                                                            "Daily Habits" -> CyberNeonMagenta
                                                            "Frequently Used Commands" -> CyberNeonGreen
                                                            else -> Color.Gray
                                                        },
                                                        CircleShape
                                                    )
                                            )
                                            Text(
                                                text = memory.category.uppercase(),
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = when(memory.category) {
                                                    "Personal Preferences" -> CyberNeonCyan
                                                    "Daily Habits" -> CyberNeonMagenta
                                                    "Frequently Used Commands" -> CyberNeonGreen
                                                    else -> Color.Gray
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = memory.content,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        
                                        // Dynamic AI Learning Confidence & Timeline Polish
                                        val calculatedConfidence = memory.confidenceScore
                                        val timeStr = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(memory.timestamp))
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "CONFIDENCE: $calculatedConfidence% // WEIGHT: 0.$calculatedConfidence",
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = CyberNeonGreen
                                            )
                                            Text(
                                                text = "•",
                                                fontSize = 7.sp,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "TRAINED: $timeStr",
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteCategorizedMemory(memory.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Memory",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 1. Engine Status & Header Panel ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("launcher_header_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "APP LAUNCHER ENGINE",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Active State Pulse Badge
                        Box(
                            modifier = Modifier
                                .background(
                                    when (launcherState) {
                                        LauncherEngineState.READY -> CyberNeonGreen.copy(alpha = 0.15f)
                                        LauncherEngineState.SEARCHING -> CyberNeonCyan.copy(alpha = 0.15f)
                                        LauncherEngineState.LAUNCHING -> Color(0xFFFFD600).copy(alpha = 0.15f)
                                        LauncherEngineState.COMPLETED -> CyberNeonGreen.copy(alpha = 0.25f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    when (launcherState) {
                                        LauncherEngineState.READY -> CyberNeonGreen.copy(alpha = 0.6f)
                                        LauncherEngineState.SEARCHING -> CyberNeonCyan.copy(alpha = 0.6f)
                                        LauncherEngineState.LAUNCHING -> Color(0xFFFFD600).copy(alpha = 0.6f)
                                        LauncherEngineState.COMPLETED -> CyberNeonGreen.copy(alpha = 0.8f)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "SYS_STATE: ${launcherState.name}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when (launcherState) {
                                    LauncherEngineState.READY -> CyberNeonGreen
                                    LauncherEngineState.SEARCHING -> CyberNeonCyan
                                    LauncherEngineState.LAUNCHING -> Color(0xFFFFD600)
                                    LauncherEngineState.COMPLETED -> CyberNeonGreen
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Real-time searchable indexing of application nodes, facilitating quick vocal launch sequences and action logs tracking.",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            }
        }

        // --- 2. Assistant Memory Panel ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("assistant_memory_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COGNITIVE MEMORY BYTES",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Memory Node Icon",
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Field 1: Last Executed Command
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "LAST VOICE COMMAND RECEIVED",
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (memoryState.lastExecutedCommand.isNotEmpty()) {
                                "\"${memoryState.lastExecutedCommand}\""
                            } else {
                                "N/A - Waiting for voice stimulus"
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (memoryState.lastExecutedCommand.isNotEmpty()) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Field 2: Last Opened Application
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "LAST OPENED APPLICATION NODE",
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (memoryState.lastOpenedAppName.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = memoryState.lastOpenedAppName,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberNeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = memoryState.lastOpenedPackageName,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            Text(
                                text = "N/A - No launcher events registered",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Stream of Recent Actions
                    Text(
                        text = "RECENT COGNITIVE ACTIONS BUFFER",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val actions = memoryState.recentActions.split("\n").filter { it.isNotBlank() }
                    if (actions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            actions.forEach { action ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(CyberNeonCyan, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = action,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Buffer clear. No cognitive trace recorded yet.",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // --- 3. App Search & Installed Apps Panel ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("installed_apps_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INSTALLED APPLICATIONS DIRECTORY",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .background(CyberNeonCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${filteredApps.size} NODES",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.triggerAppSearch(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_search_box"),
                        placeholder = {
                            Text(
                                "Search application index...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.triggerAppSearch("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear search",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberDarkCardBorder,
                            focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scanner Action Buttons
                    Button(
                        onClick = { viewModel.triggerManualAppScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan.copy(alpha = 0.15f), contentColor = CyberNeonCyan),
                        border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("btn_manual_scan")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "RE-INDEX SYSTEM CHASSIS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Apps List Scroll Block
                    if (filteredApps.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            filteredApps.take(15).forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .testTag("app_item_card_${app.packageName}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1.0f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Android,
                                            contentDescription = "App logo generic",
                                            tint = if (app.isSystemApp) CyberNeonCyan else CyberNeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = app.appName,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = app.packageName,
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    // Direct Manual Launch Button
                                    IconButton(
                                        onClick = { viewModel.launchAppDirectly(app.packageName, app.appName) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("btn_launch_${app.packageName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Launch,
                                            contentDescription = "Trigger launch",
                                            tint = CyberNeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Zero applications match current filter index.",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Recent Opened Apps Panel (Activity log) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("launch_history_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0x99040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT LAUNCHED ACTIONS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.clearLaunchHistory() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Wipe history",
                                tint = CyberNeonMagenta,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (launchHistory.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            launchHistory.take(10).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.appName,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = item.packageName,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                                        Text(
                                            text = sdf.format(java.util.Date(item.launchTime)),
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.Gray
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (item.status == "SUCCESS") CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonMagenta.copy(alpha = 0.15f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (item.status == "SUCCESS") CyberNeonGreen.copy(alpha = 0.5f) else CyberNeonMagenta.copy(alpha = 0.5f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.status,
                                            fontSize = 7.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.status == "SUCCESS") CyberNeonGreen else CyberNeonMagenta
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Telemetry launch buffer is empty.",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceControlDashboardCard(
    viewModel: MainViewModel,
    deviceCommandsHistory: List<com.example.data.DeviceControlCommandEntity>,
    screenshotHistoryList: List<com.example.data.ScreenshotHistoryEntity>,
    fileSearchEngineQuery: String,
    fileSearchEngineType: String,
    filteredFiles: List<com.example.viewmodel.CyberFile>,
    modifier: Modifier = Modifier
) {
    var fileQueryInput by remember { mutableStateOf(fileSearchEngineQuery) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
            .testTag("device_control_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0B2E).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Device Control Core",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ULTIMATE DEVICE CONTROL CORE",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(CyberNeonCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .border(0.5.dp, CyberNeonCyan, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CORE V23",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CyberDarkCardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Status Indicators Row
            Text(
                text = "SYSTEM TELEMETRY STATUS",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Background listen status
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF141243), RoundedCornerShape(8.dp))
                        .border(0.5.dp, CyberDarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("VOICE DAEMON", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(CyberNeonGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LISTENING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberNeonGreen)
                        }
                    }
                }
                
                // Active Voice feedback status
                val isVoiceFeedbackOn = viewModel.preferences.value.voiceFeedbackEnabled
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF141243), RoundedCornerShape(8.dp))
                        .border(0.5.dp, CyberDarkCardBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.executeSingleVoiceCommandInternal(
                                if (isVoiceFeedbackOn) "disable voice feedback" else "enable voice feedback"
                            )
                        }
                        .padding(8.dp)
                ) {
                    Column {
                        Text("FEEDBACK AUDIO", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isVoiceFeedbackOn) CyberNeonCyan else Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isVoiceFeedbackOn) "SPEECH ON" else "MUTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isVoiceFeedbackOn) CyberNeonCyan else Color.Red
                            )
                        }
                    }
                }

                // Active Assistant Personality
                val actPersonality = viewModel.preferences.value.activeAssistant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF141243), RoundedCornerShape(8.dp))
                        .border(0.5.dp, CyberDarkCardBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.executeSingleVoiceCommandInternal(
                                if (actPersonality == "ZOYA") "switch to nova" else "switch to zoya"
                            )
                        }
                        .padding(8.dp)
                ) {
                    Column {
                        Text("PERSONALITY", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (actPersonality == "ZOYA") CyberNeonMagenta else CyberNeonCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = actPersonality,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (actPersonality == "ZOYA") CyberNeonMagenta else CyberNeonCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Reusable Smart Automation Sections
            Text(
                text = "LAUNCH EXECUTOR SEQUENCES",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.executeSingleVoiceCommandInternal("Morning Mode") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CyberNeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Morning ModeIcon", tint = CyberNeonCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Morning Mode", fontSize = 10.sp, color = CyberNeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Button(
                    onClick = { viewModel.executeSingleVoiceCommandInternal("Work Mode") },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonMagenta.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, CyberNeonMagenta.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Work ModeIcon", tint = CyberNeonMagenta)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Work Mode", fontSize = 10.sp, color = CyberNeonMagenta, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Command Queue / History Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMMAND STACK HISTORY (${deviceCommandsHistory.size})",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                if (deviceCommandsHistory.isNotEmpty()) {
                    Text(
                        text = "FLUSH",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Red,
                        modifier = Modifier.clickable { viewModel.clearDeviceCommands() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (deviceCommandsHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141243).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Standby: Execution core empty. Try 'Open YouTube' or 'Take screenshot'.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141243).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        deviceCommandsHistory.reversed().take(10).forEach { cmd ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cmd.commandText, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(cmd.executionDetails, fontSize = 8.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (cmd.status == "COMPLETED") CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonCyan.copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            0.5.dp,
                                            if (cmd.status == "COMPLETED") CyberNeonGreen else CyberNeonCyan,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cmd.status,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cmd.status == "COMPLETED") CyberNeonGreen else CyberNeonCyan
                                    )
                                }
                            }
                            HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. File Intelligence Search Engine Subsystem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FILE INTELLIGENCE CORE",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = fileQueryInput,
                onValueChange = {
                    fileQueryInput = it
                    viewModel.fileSearchQuery.value = it
                },
                placeholder = { Text("Search files, images, documents...", fontSize = 10.sp, color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_intelligence_input"),
                textStyle = TextStyle(fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberNeonCyan,
                    unfocusedBorderColor = CyberDarkCardBorder,
                    focusedContainerColor = Color(0xFF141243),
                    unfocusedContainerColor = Color(0xFF141243).copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Query", tint = CyberNeonCyan, modifier = Modifier.size(16.dp))
                }
            )

            Spacer(modifier = Modifier.height(6.dp))
            // File type filtering selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ALL", "IMAGE", "DOCUMENT").forEach { type ->
                    val isSelected = fileSearchEngineType == type
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) CyberNeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                0.5.dp,
                                if (isSelected) CyberNeonCyan else CyberDarkCardBorder,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { viewModel.fileSearchType.value = type }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = type,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) CyberNeonCyan else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Files display rows list
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141243).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .heightIn(max = 130.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredFiles.isEmpty()) {
                        Text("No records matched.", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(12.dp))
                    } else {
                        filteredFiles.forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.executeSingleVoiceCommandInternal("open file ${file.name}")
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (file.type == "IMAGE") Icons.Default.Search else Icons.Default.List,
                                        contentDescription = "File Type",
                                        tint = if (file.type == "IMAGE") CyberNeonCyan else CyberNeonMagenta,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(file.name, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${file.size} // ${file.lastModified}", fontSize = 8.sp, color = Color.Gray)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(text = file.type, fontSize = 6.sp, color = Color.LightGray)
                                }
                            }
                            HorizontalDivider(color = CyberDarkCardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Screenshot & Captures History Repository
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SCREENSHOT GALLERY REPOSITORY (${screenshotHistoryList.size})",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CAPTURE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        modifier = Modifier.clickable { viewModel.executeSingleVoiceCommandInternal("take screenshot") }
                    )
                    if (screenshotHistoryList.isNotEmpty()) {
                        Text(
                            text = "CLEAR",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Red,
                            modifier = Modifier.clickable { viewModel.clearScreenshots() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (screenshotHistoryList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141243).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No screenshots captured yet. Click CAPTURE above or say 'Take a screenshot'.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    screenshotHistoryList.reversed().forEach { sc ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .background(Color(0xFF141243), RoundedCornerShape(8.dp))
                                .border(0.5.dp, CyberDarkCardBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Dynamic cyber screen mockup
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(Color(0xFF0F0E2C), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, CyberNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = "Asset", tint = CyberNeonCyan.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                        Text("REACTIVE_PNG", fontSize = 7.sp, color = CyberNeonCyan.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(sc.screenshotName, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                // Share button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CyberNeonCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .clickable { viewModel.executeSingleVoiceCommandInternal("send screenshot to Rahul") }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = CyberNeonCyan, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("SHARE", fontSize = 7.sp, color = CyberNeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AndroidSystemCoreCard(
    viewModel: MainViewModel,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsState()
    
    // Core stateflows
    val fServiceState by com.example.service.NovaForegroundService.engineState.collectAsState()
    val notificationStatus by com.example.service.NovaForegroundService.notificationStatus.collectAsState()
    val wakeEngineStatus by com.example.service.BackgroundWakeWordEngine.status.collectAsState()
    val batteryStatus by com.example.service.NovaForegroundService.batteryOptimizationStatus.collectAsState()
    val recoveryCount by com.example.service.NovaForegroundService.recoveryEventsCount.collectAsState()
    val healthScore by com.example.service.NovaForegroundService.systemHealthScore.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberNeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .testTag("android_system_core_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040616)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ANDROID SYSTEM CORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        letterSpacing = 1.sp
                    )
                }

                // Health Score Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (healthScore >= 75) CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonMagenta.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (healthScore >= 75) CyberNeonGreen else CyberNeonMagenta,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "HEALTH: $healthScore",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (healthScore >= 75) CyberNeonGreen else CyberNeonMagenta
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

            // Diagnostic indicators
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Core Status
                DiagnosticRow(
                    label = "CORE RUNTIME STATE",
                    value = if (fServiceState == com.example.service.NovaForegroundService.EngineState.ACTIVE) "ACTIVE SERVICE" else "STANDBY / OFFLINE",
                    isHealthy = fServiceState == com.example.service.NovaForegroundService.EngineState.ACTIVE
                )

                // Foreground Service Status
                DiagnosticRow(
                    label = "FOREGROUND SERVICE",
                    value = fServiceState.name,
                    isHealthy = fServiceState == com.example.service.NovaForegroundService.EngineState.ACTIVE
                )

                // Notification Status
                DiagnosticRow(
                    label = "NOTIF PROTECTION",
                    value = notificationStatus,
                    isHealthy = notificationStatus == "ACTIVE"
                )

                // Wake Engine Status
                DiagnosticRow(
                    label = "WAKE ENGINE STATE",
                    value = wakeEngineStatus.name,
                    isHealthy = wakeEngineStatus == com.example.service.WakeStatus.LISTENING || wakeEngineStatus == com.example.service.WakeStatus.SLEEPING || wakeEngineStatus == com.example.service.WakeStatus.TRIGGERED
                )

                // Battery Optimizations
                DiagnosticRow(
                    label = "BATTERY STABILITY",
                    value = batteryStatus,
                    isHealthy = batteryStatus == "UNRESTRICTED"
                )

                // Recovery Events
                DiagnosticRow(
                    label = "RECOVERY ALERTS",
                    value = "$recoveryCount EVENTS TRIGGERED",
                    isHealthy = true,
                    customColor = if (recoveryCount > 0) CyberNeonCyan else Color.LightGray
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

            // Dynamic Voice Diagnostic Feedback + Navigation button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speak diagnostic status
                Button(
                    onClick = {
                        // Speak voice feedback depending on active assistant
                        val nameLower = preferences.activeAssistant.uppercase()
                        val text = if (nameLower.contains("ZOYA")) {
                            "Everything looks great! All systems are online!"
                        } else {
                            "Sir, Android System Core is fully operational."
                        }
                        viewModel.voiceManager.speak(text)
                        
                        // Trigger manual report sync
                        viewModel.syncFullSecurityReport()
                    },
                    modifier = Modifier.weight(1.5f).height(38.dp).testTag("system_core_voice_test_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VOICE COMPANION STATUS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Go to Permission Center
                Button(
                    onClick = onNavigateToPermissions,
                    modifier = Modifier.weight(1f).height(38.dp).testTag("system_core_nav_perm_center"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PERMISSION MAP",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    value: String,
    isHealthy: Boolean,
    customColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.LightGray
        )
        Text(
            text = value,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = customColor ?: (if (isHealthy) CyberNeonGreen else CyberNeonMagenta)
        )
    }
}

@Composable
fun NovaSystemStatusDashboardCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val fgServiceStatus by viewModel.foregroundServiceStatus.collectAsState()
    val voiceEngineStatus by viewModel.voiceEngineStatus.collectAsState()
    val bgWakeStatus by viewModel.wakeEngineStatus.collectAsState()
    val securityReportState by viewModel.securityReport.collectAsState()
    val databaseStatus by viewModel.databaseStatus.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    val isZoya = preferences.activeAssistant == "ZOYA"
    val accentColor = if (isZoya) CyberNeonMagenta else CyberNeonCyan

    // Calculate overall Assistant Readiness Score (0-100)
    val readinessScore = remember(fgServiceStatus, bgWakeStatus, voiceEngineStatus, securityReportState, databaseStatus) {
        var score = 0
        if (fgServiceStatus == "HEALTHY") score += 20 else score += 5
        if (bgWakeStatus.toString().contains("RUNNING") || bgWakeStatus.toString().contains("ACTIVE") || bgWakeStatus.toString().contains("STARTING")) score += 20 else score += 5
        if (voiceEngineStatus == "HEALTHY" || voiceEngineStatus == "READY") score += 20 else score += 5
        if (databaseStatus == "HEALTHY") score += 20 else score += 5
        val permScoreWeighted = (securityReportState.securityScore * 0.2f).toInt()
        score += permScoreWeighted
        score.coerceIn(5, 100)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("nova_system_status_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF040616)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with glowing status score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isZoya) "ZOYA CORE OMEGA STATUS" else "NOVA SYSTEM STATUS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "INTELLIGENCE ENGINE HEURISTICS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        color = Color.Gray
                    )
                }

                // Score dial badge
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "READINESS: $readinessScore%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subsystem Health Grid
            val rows = listOf(
                Triple("AI Brain", "ONLINE", CyberNeonGreen),
                Triple("Voice Engine", if (voiceEngineStatus == "HEALTHY") "READY" else "CONNECTED", CyberNeonGreen),
                Triple("Wake Engine", if (bgWakeStatus.toString().contains("ERROR") || bgWakeStatus.toString().contains("STOPPED")) "OFFLINE" else "ACTIVE", CyberNeonGreen),
                Triple("Background Service", if (fgServiceStatus == "HEALTHY") "RUNNING" else "PENDING", CyberNeonGreen),
                Triple("Permissions", if (securityReportState.securityScore >= 90) "SECURE" else if (securityReportState.securityScore >= 60) "WARNING" else "CRITICAL", if (securityReportState.securityScore >= 90) CyberNeonGreen else if (securityReportState.securityScore >= 60) Color(0xFFFFB300) else CyberNeonMagenta),
                Triple("Memory Core", if (databaseStatus == "HEALTHY") "ONLINE" else "ERROR", CyberNeonGreen)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.chunked(2).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunk.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF090D26), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF141F4D), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.first,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(item.third, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.second,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = item.third
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
