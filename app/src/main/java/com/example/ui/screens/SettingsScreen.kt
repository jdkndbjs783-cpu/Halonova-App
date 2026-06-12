package com.example.ui.screens

import com.example.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferences.collectAsState()
    val databaseStatus by viewModel.databaseStatus.collectAsState()
    val voiceEngineStatus by viewModel.voiceEngineStatus.collectAsState()

    // 1. Initial State Hooks synced with database/DataStore
    var usernameInput by remember { mutableStateOf("") }
    var keyInput by remember { mutableStateOf("") }
    var darkThemeChecked by remember { mutableStateOf(true) }
    
    var assistantNameInput by remember { mutableStateOf("Hello Nova") }
    var voiceLanguageInput by remember { mutableStateOf("en") }
    var speechSpeedInput by remember { mutableStateOf(1.0f) }
    var speechPitchInput by remember { mutableStateOf(1.0f) }
    var speechVolumeInput by remember { mutableStateOf(1.0f) }
    var themeModeInput by remember { mutableStateOf("cyber") }
    
    var startupEngineChecked by remember { mutableStateOf(false) }
    var notificationReadbackChecked by remember { mutableStateOf(false) }
    var voiceFeedbackChecked by remember { mutableStateOf(true) }
    var sessionAutoRestoreChecked by remember { mutableStateOf(false) }
    
    var personalityModeInput by remember { mutableStateOf("Friendly") }
    var wakePhraseInput by remember { mutableStateOf("Hello Nova") }

    // Synchronize local states when background preferences load
    LaunchedEffect(preferences) {
        usernameInput = preferences.username
        keyInput = preferences.geminiApiKey
        darkThemeChecked = preferences.darkThemeEnabled
        
        assistantNameInput = preferences.assistantName
        voiceLanguageInput = preferences.voiceLanguage
        speechSpeedInput = preferences.speechSpeed
        speechPitchInput = preferences.speechPitch
        speechVolumeInput = preferences.speechVolume
        themeModeInput = preferences.themeMode
        
        startupEngineChecked = preferences.startupEngineEnabled
        notificationReadbackChecked = preferences.notificationReadbackEnabled
        voiceFeedbackChecked = preferences.voiceFeedbackEnabled
        sessionAutoRestoreChecked = preferences.sessionAutoRestoreEnabled
        
        personalityModeInput = preferences.personalityMode
        wakePhraseInput = preferences.wakePhrase
    }

    var showSavedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title: Personalization Portal
        item {
            Text(
                text = "PERSONALIZATION CONSOLE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Card 1: Active Configuration Profile & Terminal Diagnostics
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, CyberNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF040616)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TERMINAL DIAGNOSTICS // ACTIVE PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Divider(color = CyberNeonGreen.copy(alpha = 0.2f), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ASSISTANT CODE NAME:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                        Text(
                            text = "[ ${preferences.assistantName.uppercase()} ]",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonCyan
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PERSONALITY PROTOCOL:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                        Text(
                            text = "${preferences.personalityMode.uppercase()} MODEL",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonMagenta
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ACTIVE WAKE PHRASE:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                        Text(
                            text = "\"${preferences.wakePhrase}\"",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonCyan
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SYNTHETIC VOICE CONFIG:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                        Text(
                            text = "SPEED: ${String.format("%.1f", preferences.speechSpeed)}x | PITCH: ${String.format("%.1f", preferences.speechPitch)}x",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "APP VERSION:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                        Text(
                            text = "v1.12-HARDENED",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DATABASE STATUS STATE:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                        Text(
                            text = databaseStatus.uppercase(),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (databaseStatus == "HEALTHY") CyberNeonGreen else CyberNeonMagenta
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "VOICE ENGINE STATUS:",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                        Text(
                            text = voiceEngineStatus.uppercase(),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (voiceEngineStatus == "HEALTHY") CyberNeonGreen else CyberNeonMagenta
                        )
                    }
                }
            }
        }

        // Card 2: Identity & Cognitive Node Setup
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "CORE SYSTEM IDENTITIES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )

                    // Username Input
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Operator Handle Name", fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyberNeonCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberDarkCardBorder,
                            focusedLabelColor = CyberNeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Gemini Key Input
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Gemini System API Key", fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CyberNeonCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberDarkCardBorder,
                            focusedLabelColor = CyberNeonCyan
                        ),
                        placeholder = { Text("Paste AI API Key...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_apikey_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Card 3: Personality & Assistant Identity Configuration Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "COGNITIVE & CODENAME OVERRIDES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )

                    // Assistant Name override
                    OutlinedTextField(
                        value = assistantNameInput,
                        onValueChange = { assistantNameInput = it },
                        label = { Text("Assistant Core Codename", fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = CyberNeonCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberDarkCardBorder,
                            focusedLabelColor = CyberNeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_assistant_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Custom Wake Phrase override
                    OutlinedTextField(
                        value = wakePhraseInput,
                        onValueChange = { wakePhraseInput = it },
                        label = { Text("Saved Wake Phrase Preference", fontFamily = FontFamily.Monospace) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = CyberNeonCyan) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberDarkCardBorder,
                            focusedLabelColor = CyberNeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_wake_phrase_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Personality Mode Segmented Picker
                    Text(
                        text = "PERSONALITY ALIGNMENT PROTOCOLS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val personalities = listOf("Friendly", "Professional", "Casual")
                        personalities.forEach { style ->
                            val isSelected = personalityModeInput == style
                            val borderCol = if (isSelected) CyberNeonCyan else CyberDarkCardBorder
                            val bgCol = if (isSelected) CyberNeonCyan.copy(alpha = 0.15f) else Color.Transparent
                            val txtCol = if (isSelected) CyberNeonCyan else Color.Gray
                            
                            val modeTag = "settings_personality_${style.lowercase()}"

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(bgCol, RoundedCornerShape(12.dp))
                                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                    .clickable { personalityModeInput = style }
                                    .testTag(modeTag),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = txtCol
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 4: Audio Metrics & TTS Vocal Synthesizer Parameters
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VIRTUAL VOICE SPECS (TTS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )

                    // Language Selection (Segmented Control: English vs Bengali)
                    Text(
                        text = "VOICE ENGINE LANGUAGE SECTOR",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val langs = listOf("en" to "ENGLISH", "bn" to "BENGALI (বাংলা)")
                        langs.forEach { (code, label) ->
                            val isSel = voiceLanguageInput == code
                            val borderCol = if (isSel) CyberNeonCyan else CyberDarkCardBorder
                            val bgCol = if (isSel) CyberNeonCyan.copy(alpha = 0.15f) else Color.Transparent
                            val txtCol = if (isSel) CyberNeonCyan else Color.Gray

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(bgCol, RoundedCornerShape(10.dp))
                                    .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                    .clickable { voiceLanguageInput = code },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = txtCol
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speech Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SPEECH SPEED FACTOR",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )
                            Text(
                                text = "${String.format("%.2f", speechSpeedInput)}x",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = speechSpeedInput,
                            onValueChange = { speechSpeedInput = it },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberNeonCyan,
                                activeTrackColor = CyberNeonCyan,
                                inactiveTrackColor = CyberDarkCardBorder
                            ),
                            modifier = Modifier.testTag("settings_speed_slider")
                        )
                    }

                    // Speech Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SPEECH PITCH LEVEL",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )
                            Text(
                                text = "${String.format("%.2f", speechPitchInput)}x",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = speechPitchInput,
                            onValueChange = { speechPitchInput = it },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberNeonCyan,
                                activeTrackColor = CyberNeonCyan,
                                inactiveTrackColor = CyberDarkCardBorder
                            ),
                            modifier = Modifier.testTag("settings_pitch_slider")
                        )
                    }

                    // Speech Volume Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VOLUME OUTPUT SCALE",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )
                            Text(
                                text = "${Math.round(speechVolumeInput * 100)}%",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = speechVolumeInput,
                            onValueChange = { speechVolumeInput = it },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberNeonCyan,
                                activeTrackColor = CyberNeonCyan,
                                inactiveTrackColor = CyberDarkCardBorder
                            ),
                            modifier = Modifier.testTag("settings_volume_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Live preview button (to review speed + pitch settings)
                    Button(
                        onClick = {
                            val msg = if (voiceLanguageInput == "bn") {
                                "হ্যালো অপারেটর, আমি আপনার গতি এবং পিচ পরীক্ষা করছি।"
                            } else {
                                "Hello Operator! Initializing audio render check with pitch ${String.format("%.1f", speechPitchInput)} and speed ${String.format("%.1f", speechSpeedInput)}."
                            }
                            // Temporarily set on voice manager to let users preview live settings
                            viewModel.voiceManager.setSpeechRate(speechSpeedInput)
                            viewModel.voiceManager.setPitch(speechPitchInput)
                            viewModel.voiceManager.setVolume(speechVolumeInput)
                            viewModel.voiceManager.setLanguage(voiceLanguageInput)
                            viewModel.voiceManager.speak(msg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("settings_preview_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PREVIEW SYNTHETIC VOICE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Card 5: Engine Behavior Integrations & Console Options
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "AUTOMATED INTELLIGENCE TRIGGERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace
                    )

                    // Voice Feedback Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "Acoustic Speech Synthesis",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Synthesize and speak responses aloud",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = voiceFeedbackChecked,
                            onCheckedChange = { voiceFeedbackChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberNeonCyan,
                                checkedTrackColor = CyberNeonCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("settings_voice_feedback_switch")
                        )
                    }

                    // Startup Engine Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "Initiate Engine on Startup",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Boot background system daemon on device boot",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = startupEngineChecked,
                            onCheckedChange = { startupEngineChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberNeonCyan,
                                checkedTrackColor = CyberNeonCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("settings_start_engine_switch")
                        )
                    }

                    // Theme Selection Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.0f)) {
                            Text(
                                text = "Deep Cosmic Theme",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Aesthetic deep space colors (#0A0E29)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = darkThemeChecked,
                            onCheckedChange = { darkThemeChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberNeonCyan,
                                checkedTrackColor = CyberNeonCyan.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("settings_theme_switch")
                        )
                    }
                }
            }
        }

        // Info Node about data persistence sync values
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STATUS // Changes save into persistent DataStore blocks and are auto-restored on deep code boot.",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // UNIFIED COMMIT BUTTON
        item {
            Button(
                onClick = {
                    viewModel.updatePersonalizationSettings(
                        username = usernameInput,
                        geminiApiKey = keyInput,
                        darkThemeEnabled = darkThemeChecked,
                        assistantName = assistantNameInput,
                        voiceLanguage = voiceLanguageInput,
                        speechSpeed = speechSpeedInput,
                        speechPitch = speechPitchInput,
                        speechVolume = speechVolumeInput,
                        themeMode = if (darkThemeChecked) "cyber" else "light",
                        startupEngineEnabled = startupEngineChecked,
                        notificationReadbackEnabled = notificationReadbackChecked,
                        voiceFeedbackEnabled = voiceFeedbackChecked,
                        sessionAutoRestoreEnabled = sessionAutoRestoreChecked,
                        personalityMode = personalityModeInput,
                        wakePhrase = wakePhraseInput
                    )
                    showSavedMessage = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_save_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "COMMIT CONSOLE TO ARCHIVES",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black,
                    fontSize = 12.sp
                )
            }
        }

        // Success dialog/banner popup
        if (showSavedMessage) {
            item {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSavedMessage = false }) {
                            Text("DISMISS", color = CyberNeonCyan, fontFamily = FontFamily.Monospace)
                        }
                    },
                    containerColor = Color(0xFF040616),
                    contentColor = CyberNeonGreen,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "SYS_SYNC: Synchronization vectors completed.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
