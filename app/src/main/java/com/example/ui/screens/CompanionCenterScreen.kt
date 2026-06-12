package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionCenterScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferences.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Voice states
    val isSessionActive by viewModel.isVoiceSessionActive.collectAsState()
    val currentAssistantState by viewModel.assistantState.collectAsState()

    // Screen-level loading/changing state to demonstrate dynamic modern transitions
    var isConfiguringByBrain by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "COMPANION COMMAND CENTER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("companion_center_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("companion_center_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Dynamic Mode Toggle Card (Glassmorphism Glass Card Layout)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberNeonCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .testTag("personality_selection_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x661A1F3D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CURRENT ACTIVE SYNAPSE TARGET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // PROFESSIONAL NOVA CARD
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (preferences.activeAssistant == "NOVA") CyberNeonCyan.copy(alpha = 0.15f)
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (preferences.activeAssistant == "NOVA") CyberNeonCyan
                                    else CyberDarkCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        isConfiguringByBrain = true
                                        viewModel.executeSingleVoiceCommandInternal("Switch to Nova")
                                        delay(800)
                                        isConfiguringByBrain = false
                                    }
                                }
                                .padding(12.dp)
                                .testTag("companion_toggle_nova_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NOVA",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (preferences.activeAssistant == "NOVA") CyberNeonCyan else Color.LightGray
                                )
                                Text(
                                    text = "PROFESSIONAL",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                if (preferences.activeAssistant == "NOVA") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = CyberNeonCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // COMPANION ZOYA CARD
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta.copy(alpha = 0.15f)
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta
                                    else CyberDarkCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        isConfiguringByBrain = true
                                        viewModel.executeSingleVoiceCommandInternal("Switch to Zoya")
                                        delay(800)
                                        isConfiguringByBrain = false
                                    }
                                }
                                .padding(12.dp)
                                .testTag("companion_toggle_zoya_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ZOYA",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else Color.LightGray
                                )
                                Text(
                                    text = "COMPANION",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                if (preferences.activeAssistant == "ZOYA") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = CyberNeonMagenta,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Animated Avatar Presence Room (Requirement 2.3)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .border(
                        1.dp,
                        if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta.copy(alpha = 0.3f) else CyberNeonCyan.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("avatar_presence_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Companion Presence Field".uppercase(),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Pulsing Ring Core Engine
                    val isZoya = preferences.activeAssistant == "ZOYA"
                    val coreColor = if (isZoya) CyberNeonMagenta else CyberNeonCyan
                    val stateColor = when (currentAssistantState) {
                        com.example.service.AssistantState.STANDBY -> coreColor.copy(alpha = 0.4f)
                        com.example.service.AssistantState.ARMED -> coreColor
                        com.example.service.AssistantState.LISTENING -> CyberNeonCyan
                        com.example.service.AssistantState.PROCESSING -> CyberNeonGreen
                        com.example.service.AssistantState.SPEAKING -> CyberNeonMagenta
                        com.example.service.AssistantState.RECOVERY -> Color(0xFFFFB300) // Recovery Warm Yellow
                    }

                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "avatarPulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(stateColor.copy(alpha = 0.08f))
                            .border(1.dp, stateColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing glowing rings
                        Canvas(modifier = Modifier.size(90.dp)) {
                            drawCircle(
                                color = stateColor,
                                radius = (size.minDimension / 2) * pulseScale,
                                style = Stroke(width = 2.dp.toPx()),
                                alpha = 0.45f
                            )
                        }

                        // Static Inner Core Icon/Marking
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(stateColor.copy(alpha = 0.4f), Color.Transparent)))
                                .border(2.dp, stateColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Presence Star Core",
                                tint = stateColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isZoya) "ZOYA COMPANION ENGAGED" else "NOVA QUANTUM ENGINE CONNECTED",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = coreColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Status: ${currentAssistantState.name}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 3. Real-Time Voice Activity Visualization (Requirement 2.4)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("voice_visualization_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x33101430)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "REAL-TIME VOICE SIGNATURE SENSORS",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Wave Activity Line Simulator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "bars")
                        
                        val activeCoeff = when (currentAssistantState) {
                            com.example.service.AssistantState.STANDBY -> 0.1f
                            com.example.service.AssistantState.SPEAKING, 
                            com.example.service.AssistantState.LISTENING -> 1.0f
                            else -> 0.4f
                        }

                        for (i in 0..11) {
                            val duration = 300 + (i * 120)
                            val heightFactor by infiniteTransition.animateFloat(
                                initialValue = 5f,
                                targetValue = 45f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(duration, easing = LinearOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar_$i"
                            )

                            val heightDp = (heightFactor * activeCoeff).coerceAtLeast(4f).dp
                            val barBrush = Brush.verticalGradient(
                                colors = listOf(
                                    if (isSessionActive) CyberNeonGreen else CyberNeonCyan,
                                    if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(heightDp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barBrush)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isSessionActive) "STREAMING REAL-TIME AUDIO" else "VOICE CHANNEL INACTIVE // SLEEP MODE",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            }

            // 4. Voice Test Trigger button with the Exact Welcome Phrases (Requirement 5 & 9)
            Button(
                onClick = {
                    val activeName = preferences.activeAssistant
                    val text = if (activeName == "ZOYA") {
                        "Hey! Everything is ready. Let's have some fun!"
                    } else {
                        "Sir, Nova Omega Core is online. All systems operational."
                    }
                    viewModel.voiceManager.speak(text)
                    viewModel.voiceNovaResponse.value = text
                    viewModel.writeTerminalLog("ASSISTANT_WELCOME // $activeName greeting emitted.")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("companion_voice_test_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (preferences.activeAssistant == "ZOYA") CyberNeonMagenta else CyberNeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EMIT IDENTITY SYNAPSE GREETING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
