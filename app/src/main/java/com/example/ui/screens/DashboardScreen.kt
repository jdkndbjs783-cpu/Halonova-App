package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.AssistantState
import com.example.service.NovaForegroundService
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToPermissions: () -> Unit,
    onNavigateToCompanion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Core parameters from Model
    val isSessionActive by viewModel.isVoiceSessionActive.collectAsState()
    val currentAssistantState by viewModel.assistantState.collectAsState()
    val voiceRecognizedText by viewModel.voiceRecognizedText.collectAsState()
    val voiceHistory by viewModel.voiceConversations.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    
    // Health and state counts
    val continuousListeningEnabled by viewModel.isContinuousListeningHookEnabled.collectAsState()
    val fgServiceStatus by viewModel.foregroundServiceStatus.collectAsState()
    val voiceEngineStatus by viewModel.voiceEngineStatus.collectAsState()
    val categorizedMemories by viewModel.categorizedMemories.collectAsState()

    var hasMicrophonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicrophonePermission = granted
        viewModel.logPermissionStatus("RECORD_AUDIO", granted)
    }

    LaunchedEffect(Unit) {
        if (!hasMicrophonePermission) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Flashlight Toggle logic
    var flashlightEnabled by remember { mutableStateOf(false) }
    
    fun toggleFlashlight() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.getOrNull(0)
            if (cameraId != null) {
                flashlightEnabled = !flashlightEnabled
                cameraManager.setTorchMode(cameraId, flashlightEnabled)
                viewModel.writeTerminalLog("FLASHLIGHT_TRIGGER // State: $flashlightEnabled")
            }
        } catch (e: Exception) {
            Log.e("NovaHome", "Failed to access flashlight daemon", e)
        }
    }

    // Infinite rotations/pulses for halo
    val infiniteTransition = rememberInfiniteTransition(label = "nova_core_halo")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radial_rotation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "calm_breathing"
    )

    val neonColorState by animateColorAsState(
        targetValue = when (currentAssistantState) {
            AssistantState.LISTENING -> CyberNeonCyan
            AssistantState.SPEAKING -> CyberNeonMagenta
            AssistantState.PROCESSING -> CyberNeonGreen
            AssistantState.RECOVERY -> Color.Yellow
            else -> CyberNeonCyan.copy(alpha = 0.7f)
        },
        animationSpec = tween(500),
        label = "state_color_morph"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Spacer(modifier = Modifier.height(16.dp))

        // 1. HEADER SECTION & DIAGNOSTIC CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0x33101430)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "HELLO NOVA",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = if (isSessionActive) "COGNITIVE SYNAPSE UPLINK ACTIVE" else "QUANTUM INTELLIGENCE TERMINAL STANDBY",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSessionActive) CyberNeonGreen else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Small pulsing LED indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isSessionActive) CyberNeonGreen else Color.Gray)
                        .border(1.dp, (if (isSessionActive) CyberNeonGreen else Color.Gray).copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        // 2. LARGE ANIMATED NOVA AVATAR
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer(
                    scaleX = if (isSessionActive) breathingScale else 1f,
                    scaleY = if (isSessionActive) breathingScale else 1f
                ),
            contentAlignment = Alignment.Center
        ) {
            // Rotating outer cyber halos
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size / 2f
                val radius = size.minDimension / 2.3f

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            neonColorState.copy(alpha = 0.05f),
                            neonColorState,
                            neonColorState.copy(alpha = 0.1f)
                        )
                    ),
                    startAngle = rotationAngle,
                    sweepAngle = 290f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx())
                )

                drawCircle(
                    color = neonColorState.copy(alpha = 0.08f),
                    radius = radius * 0.8f,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Central Pulsating Radial Glow
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                neonColorState.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Interior metallic plate
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    neonColorState.copy(alpha = 0.25f),
                                    Color.Black
                                )
                            )
                        )
                        .border(1.5.dp, neonColorState, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentAssistantState) {
                            AssistantState.LISTENING -> Icons.Default.Hearing
                            AssistantState.SPEAKING -> Icons.Default.VolumeUp
                            AssistantState.PROCESSING -> Icons.Default.HourglassEmpty
                            AssistantState.RECOVERY -> Icons.Default.Warning
                            else -> Icons.Default.SupportAgent
                        },
                        contentDescription = "Active face Mode",
                        tint = neonColorState,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Live status transcription overview
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (voiceRecognizedText.isNotBlank()) "\"$voiceRecognizedText\"" else "Awaiting voice instructions...",
                fontSize = 12.sp,
                color = if (isSessionActive) Color.White else Color.Gray,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )
        }

        // 3. TACTILE MIC TRIGGER CONTROLLER
        Button(
            onClick = {
                if (isSessionActive) {
                    viewModel.endManualVoiceSession()
                } else {
                    viewModel.startManualVoiceSession()
                }
            },
            modifier = Modifier
                .size(64.dp)
                .testTag("radar_microphone_button"),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSessionActive) CyberNeonMagenta else CyberNeonCyan
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (isSessionActive) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Trigger Speech Sequence",
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = if (isSessionActive) "TAP TO TERMINATE VOICE UPLINK" else "TAP TO INITIATE VOICE SYNAPSE",
            fontSize = 9.sp,
            color = Color.LightGray.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        // 4. ★ QUICK ACTIONS SECTION
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "★ QUICK ACTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberNeonCyan,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Action: Camera
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.executeSingleVoiceCommandInternal("open Camera") },
                    colors = CardDefaults.cardColors(containerColor = Color(0x22111536)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = CyberNeonCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("CAMERA", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // Action: YouTube
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.executeSingleVoiceCommandInternal("play background music on YouTube") },
                    colors = CardDefaults.cardColors(containerColor = Color(0x22111536)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = "YouTube", tint = CyberNeonMagenta, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("YOUTUBE", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Action: WhatsApp
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.executeSingleVoiceCommandInternal("open WhatsApp") },
                    colors = CardDefaults.cardColors(containerColor = Color(0x22111536)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Comment, contentDescription = "WhatsApp", tint = CyberNeonGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("WHATSAPP", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }

                // Action: Flashlight
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .border(1.dp, if (flashlightEnabled) CyberNeonCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .clickable { toggleFlashlight() },
                    colors = CardDefaults.cardColors(containerColor = Color(0x22111536)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (flashlightEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (flashlightEnabled) CyberNeonCyan else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("FLASHLIGHT", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. ★ NOVA SYSTEM HEALTH SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .testTag("system_health_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0x44020412)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "★ NOVA SYSTEM HEALTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberNeonGreen,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                // 1. AI Brain
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = CyberNeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Brain Cognition", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    }
                    Text("ONLINE // SECURE", fontSize = 11.sp, color = CyberNeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // 2. Microphone
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = if (hasMicrophonePermission) CyberNeonCyan else Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Physical Microphone", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        text = if (hasMicrophonePermission) "ARMED" else "DENIED",
                        fontSize = 11.sp,
                        color = if (hasMicrophonePermission) CyberNeonCyan else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 3. Wake Engine
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Hearing, contentDescription = null, tint = CyberNeonMagenta, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wake Phrase Detector", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        text = if (continuousListeningEnabled) "WATCHING" else "STANDBY",
                        fontSize = 11.sp,
                        color = if (continuousListeningEnabled) CyberNeonMagenta else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 4. Background Service
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dvr, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Background Service Daemon", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        text = fgServiceStatus.uppercase(),
                        fontSize = 11.sp,
                        color = if (fgServiceStatus.contains("running", ignoreCase = true)) CyberNeonGreen else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // 5. Permissions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Permissions Center", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = onNavigateToPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow.copy(alpha = 0.15f), contentColor = Color.Yellow),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("MANAGE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. ★ MEMORY CENTER SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .testTag("memory_center_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0x33101430)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★ MEMORY CENTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonMagenta,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${categorizedMemories.size} Neural Patterns Recorded",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "Hello Nova logs key behaviors and recurring intent maps offline to optimize local responses.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = {
                        // We will allow users to jump or see dialogs
                        onNavigateToCompanion() // Jump to configuration or keep simple click
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonMagenta.copy(alpha = 0.15f), contentColor = CyberNeonMagenta),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ACCESS COGNITIVE REPOSITORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // 7. ★ COMPANION CENTER SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .testTag("companion_center_card"),
            colors = CardDefaults.cardColors(containerColor = Color(0x33101430)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★ COMPANION CENTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "MODULE ACTIVE: ${preferences.activeAssistant}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (preferences.activeAssistant == "ZOYA") {
                        "Zoya friendly persona engaged. Casual emotional reactions and casual speech template deployed."
                    } else {
                        "Nova professional engine engaged. Formal assistance voice template active with respectful response criteria."
                    },
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = onNavigateToCompanion,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan.copy(alpha = 0.15f), contentColor = CyberNeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CONFIGURE PERSONALITY ROUTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
