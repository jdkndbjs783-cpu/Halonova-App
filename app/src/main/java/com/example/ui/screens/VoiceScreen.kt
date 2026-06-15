package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import com.example.service.AssistantState
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferences.collectAsState()
    val currentAssistantState by viewModel.assistantState.collectAsState()
    val isListening by viewModel.voiceIsListening.collectAsState()
    val recognizedText by viewModel.voiceRecognizedText.collectAsState()
    val scope = rememberCoroutineScope()

    // Interactive selections
    val currentLanguage = preferences.voiceLanguage
    val currentAssistantName = preferences.activeAssistant
    val currentStyle = preferences.personalityMode

    // Animating values
    val infiniteTransition = rememberInfiniteTransition(label = "voice_screen_anims")
    
    // Wave rods height scaling loops
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "wave2"
    )
    val waveScale3 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave3"
    )
    val waveScale4 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.70f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wave4"
    )

    // Breathing scale & alpha (4-second duration mimicking breathing cycle)
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathScale"
    )
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathAlpha"
    )

    // Spinner for processing/thinking states
    val spinRotate by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "spinRotate"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("voice_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Section
        item {
            Text(
                text = "QUANTUM SPEECH SYNAPSE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "DYNAMIC SPEECH PATTERNS • ALWAYS LISTENING",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray.copy(alpha = 0.6f)
            )
        }

        // 1. Interactive Wave & Breathing Visual Area
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x661A1F3D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Constant visual breathing aura
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .graphicsLayer {
                                scaleX = breathScale
                                scaleY = breathScale
                                alpha = breathAlpha
                            }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        if (currentAssistantName == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Core state visualizer orb
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(
                                2.dp,
                                Brush.sweepGradient(
                                    colors = listOf(
                                        if (currentAssistantName == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                CircleShape
                            )
                            .clip(CircleShape)
                            .background(Color(0xFF040616)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentAssistantState == AssistantState.PROCESSING) {
                            // Thinking rotating ring animation
                            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = spinRotate }) {
                                drawCircle(
                                    color = if (currentAssistantName == "ZOYA") CyberNeonMagenta else CyberNeonCyan,
                                    radius = size.minDimension / 2.3f,
                                    style = Stroke(width = 4f)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice active icon",
                            tint = if (isListening) CyberNeonGreen else (if (currentAssistantName == "ZOYA") CyberNeonMagenta else CyberNeonCyan),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Bottom: Streaming transcription or status label
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (currentAssistantState) {
                                AssistantState.LISTENING -> "LISTENING TO TRANSCRIPT FEED..."
                                AssistantState.PROCESSING -> "COGNITIVE MATRIX PROCESSING..."
                                AssistantState.SPEAKING -> "NOVA CORE SYNTHESIZING SPEECH..."
                                else -> "VOICE QUANTUM TRANSLATOR ACTIVE"
                            },
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) CyberNeonGreen else Color.LightGray.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (recognizedText.isNotBlank()) "\"$recognizedText\"" else "\"Always listening for your wake phrase...\"",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }

        // Real-time Voice Wave Animation rods (always moves active pulse lines)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val waveColor = if (currentAssistantName == "ZOYA") CyberNeonMagenta else CyberNeonCyan
                val count = 28
                for (i in 0 until count) {
                    val s = if (i % 4 == 0) waveScale1 else if (i % 4 == 1) waveScale2 else if (i % 4 == 2) waveScale3 else waveScale4
                    val multiplier = if (isListening || currentAssistantState == AssistantState.SPEAKING) 1.0f else 0.15f
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(3.dp)
                            .fillMaxHeight(s * multiplier)
                            .clip(RoundedCornerShape(2.dp))
                            .background(waveColor)
                    )
                }
            }
        }

        // 2. Assistant Personality Customization Switcher Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .testTag("personality_selection_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x661A1F3D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ASSISTANT BIOMETRICS",
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
                        // Nova Choice
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (currentAssistantName == "NOVA") CyberNeonCyan.copy(alpha = 0.15f)
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (currentAssistantName == "NOVA") CyberNeonCyan else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        viewModel.executeSingleVoiceCommandInternal("Switch to Nova")
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NOVA",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (currentAssistantName == "NOVA") CyberNeonCyan else Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "PROFESSIONAL\nRESPECTFUL",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Zoya Choice
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (currentAssistantName == "ZOYA") CyberNeonMagenta.copy(alpha = 0.15f)
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (currentAssistantName == "ZOYA") CyberNeonMagenta else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        viewModel.executeSingleVoiceCommandInternal("Switch to Zoya")
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ZOYA",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (currentAssistantName == "ZOYA") CyberNeonMagenta else Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "FRIENDLY • CUTE\nEMOTIONAL",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Language Matrix Choice Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x661A1F3D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SPEECH TRANSLATION LANGUAGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonMagenta,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // English option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (currentLanguage == "en") CyberNeonCyan.copy(alpha = 0.15f)
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (currentLanguage == "en") CyberNeonCyan else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        val p = preferences
                                        viewModel.updatePersonalizationSettings(
                                            username = p.username,
                                            geminiApiKey = p.geminiApiKey,
                                            darkThemeEnabled = p.darkThemeEnabled,
                                            assistantName = p.assistantName,
                                            voiceLanguage = "en",
                                            speechSpeed = p.speechSpeed,
                                            speechPitch = p.speechPitch,
                                            speechVolume = p.speechVolume,
                                            themeMode = p.themeMode,
                                            startupEngineEnabled = p.startupEngineEnabled,
                                            notificationReadbackEnabled = p.notificationReadbackEnabled,
                                            voiceFeedbackEnabled = p.voiceFeedbackEnabled,
                                            sessionAutoRestoreEnabled = p.sessionAutoRestoreEnabled,
                                            personalityMode = p.personalityMode,
                                            wakePhrase = p.wakePhrase
                                        )
                                        viewModel.writeTerminalLog("LANG_SYNC // Synapse updated target language: [ENGLISH]")
                                    }
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ENGLISH (US)",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (currentLanguage == "en") CyberNeonCyan else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Bangla option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (currentLanguage == "bn") CyberNeonMagenta.copy(alpha = 0.15f)
                                    else Color.Black.copy(alpha = 0.25f)
                                )
                                .border(
                                    1.dp,
                                    if (currentLanguage == "bn") CyberNeonMagenta else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        val p = preferences
                                        viewModel.updatePersonalizationSettings(
                                            username = p.username,
                                            geminiApiKey = p.geminiApiKey,
                                            darkThemeEnabled = p.darkThemeEnabled,
                                            assistantName = p.assistantName,
                                            voiceLanguage = "bn",
                                            speechSpeed = p.speechSpeed,
                                            speechPitch = p.speechPitch,
                                            speechVolume = p.speechVolume,
                                            themeMode = p.themeMode,
                                            startupEngineEnabled = p.startupEngineEnabled,
                                            notificationReadbackEnabled = p.notificationReadbackEnabled,
                                            voiceFeedbackEnabled = p.voiceFeedbackEnabled,
                                            sessionAutoRestoreEnabled = p.sessionAutoRestoreEnabled,
                                            personalityMode = p.personalityMode,
                                            wakePhrase = p.wakePhrase
                                        )
                                        viewModel.writeTerminalLog("LANG_SYNC // Synapse updated target language: [BENGALI]")
                                    }
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "বাংলা (BD)",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (currentLanguage == "bn") CyberNeonMagenta else Color.LightGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Voice Style Selection (Professional, Friendly, Natural, Warm)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0x661A1F3D)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYNAPSE PERSONALIZATION STYLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val styles = listOf("Professional", "Friendly", "Natural", "Warm")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styles.forEach { styleOption ->
                            val isSelected = currentStyle == styleOption
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) CyberNeonGreen.copy(alpha = 0.15f)
                                        else Color.Black.copy(alpha = 0.25f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberNeonGreen else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            val p = preferences
                                            viewModel.updatePersonalizationSettings(
                                                username = p.username,
                                                geminiApiKey = p.geminiApiKey,
                                                darkThemeEnabled = p.darkThemeEnabled,
                                                assistantName = p.assistantName,
                                                voiceLanguage = p.voiceLanguage,
                                                speechSpeed = p.speechSpeed,
                                                speechPitch = p.speechPitch,
                                                speechVolume = p.speechVolume,
                                                themeMode = p.themeMode,
                                                startupEngineEnabled = p.startupEngineEnabled,
                                                notificationReadbackEnabled = p.notificationReadbackEnabled,
                                                voiceFeedbackEnabled = p.voiceFeedbackEnabled,
                                                sessionAutoRestoreEnabled = p.sessionAutoRestoreEnabled,
                                                personalityMode = styleOption,
                                                wakePhrase = p.wakePhrase
                                            )
                                            viewModel.writeTerminalLog("STYLE_SYNC // Applied voice style template: [${styleOption.uppercase()}]")
                                        }
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = styleOption.uppercase(),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) CyberNeonGreen else Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
