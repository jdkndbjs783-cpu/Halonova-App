package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategorizedMemoryEntity
import com.example.data.VoiceConversationEntity
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.voiceConversations.collectAsState()
    val categorizedMemories by viewModel.categorizedMemories.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }

    // Filtered lists
    val filteredConversations = remember(conversations, searchQuery) {
        if (searchQuery.isBlank()) {
            conversations
        } else {
            conversations.filter {
                it.userSpeech.contains(searchQuery, ignoreCase = true) ||
                it.novaResponse.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredMemories = remember(categorizedMemories, searchQuery) {
        if (searchQuery.isBlank()) {
            categorizedMemories
        } else {
            categorizedMemories.filter {
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.patternName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Standard glassmorphic layout
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("memory_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Header
        item {
            Text(
                text = "COGNITIVE MEMORY SYSTEM",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "SYNAPTIC STORAGE NODES • CORE RECOVERY",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray.copy(alpha = 0.6f)
            )
        }

        // Search Interface
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("memory_search_input"),
                placeholder = {
                    Text(
                        "Search dialogue node or pattern...",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = CyberNeonCyan
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberNeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF040616),
                    unfocusedContainerColor = Color(0xFF040616)
                )
            )
        }

        // Wipe Button Header Control
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEMORIES IN STORAGE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberNeonMagenta
                )

                Button(
                    onClick = {
                        viewModel.clearAllCategorizedMemories()
                        viewModel.clearVoiceHistory()
                    },
                    modifier = Modifier.testTag("purge_all_memories_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberNeonMagenta.copy(alpha = 0.2f),
                        contentColor = CyberNeonMagenta
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete all",
                        modifier = Modifier.size(14.dp),
                        tint = CyberNeonMagenta
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PURGE ALL",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section A: Learned Commands & Patterns
        item {
            Text(
                text = "// LEARNED HABITS & AI PATTERNS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberNeonCyan,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredMemories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .background(Color(0xFF040616))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No neural pattern records registered.\n Nova learns commands and launches dynamically.",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredMemories) { memory ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberNeonCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .testTag("memory_item_card_${memory.id}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0x331A1F3D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Pattern Star",
                                    tint = CyberNeonCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = memory.patternName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = memory.content,
                                fontSize = 12.sp,
                                color = Color.LightGray,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Confidence: ${memory.confidenceScore}%",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(CyberNeonCyan)
                                )
                                Text(
                                    text = "Hits: ${memory.frequencyCount}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteCategorizedMemory(memory.id) },
                            modifier = Modifier.testTag("delete_memory_btn_${memory.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Memory node",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section B: Recent Conversations
        item {
            Text(
                text = "// RECENT CONVERSATIONS HISTORY",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = CyberNeonMagenta,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (filteredConversations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .background(Color(0xFF040616))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "History logs empty.\nSpeak to Nova or Zoya to begin neural record sync.",
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredConversations) { conv ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .testTag("conv_item_card_${conv.id}"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF040616)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "USER // INPUT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberNeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = conv.language.uppercase(),
                                fontSize = 8.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = conv.userSpeech,
                            fontSize = 13.sp,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )

                        Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                        Text(
                            text = "ASSISTANT // RESPONSE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonMagenta,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = conv.novaResponse,
                            fontSize = 13.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}
