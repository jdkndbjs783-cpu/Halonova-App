package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AssistantState
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlin.math.sin

@Composable
fun SiriLiveOverlay(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAssistantState by viewModel.assistantState.collectAsState()
    val recognizedText by viewModel.voiceRecognizedText.collectAsState()
    val voiceNovaResponse by viewModel.voiceNovaResponse.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    
    val isListening = currentAssistantState == AssistantState.LISTENING

    // Animate color state representing deep neural phases
    val orbColorState by animateColorAsState(
        targetValue = when (currentAssistantState) {
            AssistantState.LISTENING -> CyberNeonCyan
            AssistantState.PROCESSING -> CyberNeonGreen
            AssistantState.SPEAKING -> CyberNeonMagenta
            AssistantState.RECOVERY -> Color.Yellow
            else -> CyberNeonCyan.copy(alpha = 0.8f)
        },
        animationSpec = tween(600),
        label = "color_morph"
    )

    // Infinite transitions for breathing/pulses
    val infiniteTransition = rememberInfiniteTransition(label = "siri_live_anims")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radial_rotation"
    )

    val pulsingRadius by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val voiceWaveAmplitude by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_amplitude"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF2020412)) // Immersive velvet darkness
            .testTag("siri_live_overlay"),
        contentAlignment = Alignment.Center
    ) {

        // 1. TOP DISMISS / ACTION CONTROLLERS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection identity info
                Column {
                    Text(
                        text = "LIVE QUANTUM ENCRYPTED LINK",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = orbColorState,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "TARGET: ${preferences.activeAssistant} ASSISTANT core".uppercase(),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                // Interactive exit controller
                IconButton(
                    onClick = {
                        viewModel.endManualVoiceSession()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .testTag("siri_live_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect session",
                        tint = Color.White
                    )
                }
            }
        }

        // 2. CENTRAL ANIMATED GEOMETRIC ORB REPRESENTATION
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer(
                    scaleX = if (isListening) pulsingRadius else 1f,
                    scaleY = if (isListening) pulsingRadius else 1f
                ),
            contentAlignment = Alignment.Center
        ) {
            // Nebula Layer (Ambient background glow)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                orbColorState.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Dynamic Orbits
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size / 2f
                val radius = size.minDimension / 2.2f

                // Outer decorative constellation loops
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            orbColorState.copy(alpha = 0.05f),
                            orbColorState,
                            orbColorState.copy(alpha = 0.1f)
                        )
                    ),
                    startAngle = rotationAngle,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Sub-ring orbit 2
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            orbColorState.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    startAngle = -rotationAngle * 1.5f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Real-time voice vector wave analyzer bounds
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                orbColorState.copy(alpha = 0.25f),
                                Color.Black
                              )
                          )
                      )
                      .border(0.5.dp, orbColorState, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // High contrast stable interior icon core
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF040616))
                        .border(1.5.dp, orbColorState.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentAssistantState) {
                            AssistantState.LISTENING -> Icons.Default.Mic
                            else -> Icons.Default.MicOff
                        },
                        contentDescription = "Microphone status",
                        tint = orbColorState,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 3. SOUNDWAVE VISUALLY PULSING MULTI-ROD ENGINE (at bottom half)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Transcription Console
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                // Current speech state
                Text(
                    text = when (currentAssistantState) {
                        AssistantState.LISTENING -> "LISTENING TO TRANSCRIPT..."
                        AssistantState.PROCESSING -> "COGNITIVE RECURSIVE ANALYSIS..."
                        AssistantState.SPEAKING -> "EMITTING SYNTHESIS CHANNELS..."
                        else -> "UPLINK ESTABLISHED"
                    }.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = orbColorState,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Understood transcript preview
                Text(
                    text = if (recognizedText.isNotBlank()) recognizedText else "Awakened. Nova core is listening for instructions...",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                if (voiceNovaResponse.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = voiceNovaResponse,
                        fontSize = 13.sp,
                        color = orbColorState,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag("siri_voice_response_preview")
                    )
                }
            }

            // High Fidelity Animated Soundwave Rods (24 symmetric responsive bars)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barCount = 24
                for (i in 0 until barCount) {
                    val scaleFactor = sin((i.toFloat() / barCount) * Math.PI).toFloat()
                    val dynamicAmplitude = when (currentAssistantState) {
                        AssistantState.LISTENING -> (0.2f + voiceWaveAmplitude * 0.8f) * scaleFactor
                        AssistantState.SPEAKING -> (0.4f + voiceWaveAmplitude * 0.6f) * scaleFactor
                        AssistantState.PROCESSING -> 0.15f * scaleFactor
                        else -> 0.05f
                    }
                    val currentBarHeight = (dynamicAmplitude * 40f).coerceAtLeast(4f).dp

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.5.dp)
                            .width(4.dp)
                            .height(currentBarHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        orbColorState,
                                        orbColorState.copy(alpha = 0.2f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
