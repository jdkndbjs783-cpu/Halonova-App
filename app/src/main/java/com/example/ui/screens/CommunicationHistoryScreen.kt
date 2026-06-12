package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel
import com.example.data.ContactEntity
import com.example.data.ContactSearchHistoryEntity
import com.example.data.CommunicationEventEntity
import com.example.service.ContactEngineState
import com.example.ui.theme.CyberDarkCardBorder
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.CyberNeonMagenta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommunicationHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.allContacts.collectAsState()
    val searchHistory by viewModel.contactSearchHistory.collectAsState()
    val rawEvents by viewModel.communicationEventsHistory.collectAsState()
    val engineState by viewModel.contactEngineState.collectAsState()
    val assistantMemory by viewModel.assistantMemory.collectAsState()
    val contactQuery by viewModel.contactDirectoryQuery.collectAsState()

    var showSimCallDialog by remember { mutableStateOf(false) }

    // Filter contacts dynamic indexing
    val filteredContacts = remember(contacts, contactQuery) {
        if (contactQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.displayName.contains(contactQuery, ignoreCase = true) ||
                it.phoneNumber.contains(contactQuery, ignoreCase = true) ||
                it.email.contains(contactQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040616))
            .padding(horizontal = 16.dp)
            .testTag("communication_history_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. Communication AI Dashboard (Phase 21.3) ---
        item {
            val activeCallStateVal by viewModel.activeCallState.collectAsState()
            val activeCallerNameVal by viewModel.activeCallerName.collectAsState()
            val latestSmsSenderVal by viewModel.latestSmsSender.collectAsState()
            val latestSmsContentVal by viewModel.latestSmsContent.collectAsState()
            val latestWhatsAppSenderVal by viewModel.latestWhatsAppSender.collectAsState()
            val latestWhatsAppContentVal by viewModel.latestWhatsAppContent.collectAsState()

            val missedCallsCount = rawEvents.count { it.type == "CALL" && it.title == "MISSED_CALL" }
            val totalCallsCount = rawEvents.count { it.type == "CALL" }
            val totalSmsCount = rawEvents.count { it.type == "SMS" }
            val totalNotifCount = rawEvents.count { it.type == "NOTIFICATION" }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("comms_dashboard_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B24)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COMMUNICATION MONITOR CORE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Connections & Missed Calls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Uplink Block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "UPLINK STATE",
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isRingOrActive = activeCallStateVal == "RINGING" || activeCallStateVal == "ACTIVE"
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (isRingOrActive) CyberNeonMagenta else Color.Gray,
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = activeCallStateVal,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isRingOrActive) CyberNeonMagenta else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (activeCallerNameVal != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "NAME: $activeCallerNameVal",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberNeonCyan,
                                        lineHeight = 10.sp
                                    )
                                }
                            }
                        }

                        // Missed Calls Block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "MISSED CALLS BUFFER",
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneMissed,
                                        contentDescription = null,
                                        tint = if (missedCallsCount > 0) CyberNeonMagenta else Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$missedCallsCount MISSED",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (missedCallsCount > 0) CyberNeonMagenta else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SMS and WhatsApp Live Logs
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Live SMS Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "LIVE SMS INTERCEPTOR",
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberNeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(CyberNeonCyan.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "RUNNING",
                                            fontSize = 6.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberNeonCyan
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (latestSmsSenderVal != null) {
                                    Text(
                                        text = "Sir, $latestSmsSenderVal: \"$latestSmsContentVal\"",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        lineHeight = 12.sp
                                    )
                                } else {
                                    Text(
                                        text = "Listening for incoming encrypted telemetries...",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        // WhatsApp Live Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "WHATSAPP INTELLIGENCE INTERCEPTOR",
                                        fontSize = 7.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberNeonGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(CyberNeonGreen.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontSize = 6.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CyberNeonGreen
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (latestWhatsAppSenderVal != null) {
                                    Text(
                                        text = "Sir, $latestWhatsAppSenderVal: \"$latestWhatsAppContentVal\"",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        lineHeight = 12.sp
                                    )
                                } else {
                                    Text(
                                        text = "Listening for incoming message hooks...",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Communication Statistics segmental pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("TOTAL CALLS", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Text("$totalCallsCount", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(CyberDarkCardBorder.copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("TOTAL SMS", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Text("$totalSmsCount", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(CyberDarkCardBorder.copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("TOTAL NOTIF", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Text("$totalNotifCount", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
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
                    .testTag("contact_engine_status_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0x990A0E29)),
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
                                text = "CONTACT DIRECTORY ENGINE",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SYS_STATE: ${engineState.name}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }

                        // State Badges mapping (READY, SEARCHING_CONTACTS, CONTACT_FOUND, COMPLETED)
                        val statusColor = when (engineState) {
                            ContactEngineState.READY -> CyberNeonGreen
                            ContactEngineState.SEARCHING_CONTACTS -> CyberNeonCyan
                            ContactEngineState.CONTACT_FOUND -> Color(0xFF00FFCC)
                            ContactEngineState.COMPLETED -> CyberNeonMagenta
                        }

                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = engineState.name,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Real-time query processing nodes. Fast local search with zero automated tracking, OTP requirements, or hidden permissions.",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // --- 2. Contact Search Box ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("contact_search_box_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0x770A0E29)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SEARCH ADDRESS DATABASE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberNeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = contactQuery,
                        onValueChange = { viewModel.triggerContactDirectorySearch(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_search_box"),
                        placeholder = {
                            Text(
                                "Search name, phone, or email...",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Contacts",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (contactQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.triggerContactDirectorySearch("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear input",
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
                            focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.25f)
                        )
                    )
                }
            }
        }

        // --- 3. Contact Directory Panel ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("contact_directory_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0x990A0E29)),
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
                                imageVector = Icons.Default.ContactPage,
                                contentDescription = null,
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CONTACT DIRECTORY INDEX",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(CyberNeonCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${filteredContacts.size} NODES",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Re-sync catalog option
                    Button(
                        onClick = { viewModel.triggerManualContactScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberNeonCyan.copy(alpha = 0.12f),
                            contentColor = CyberNeonCyan
                        ),
                        border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("btn_contact_resync")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RE-SYNC CENTRAL DIRECTORY",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (filteredContacts.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            filteredContacts.take(15).forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .testTag("contact_item_${contact.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    if (contact.isStarred) CyberNeonGreen.copy(alpha = 0.1f) else CyberNeonCyan.copy(alpha = 0.1f),
                                                    CircleShape
                                                )
                                                .border(
                                                    1.dp,
                                                    if (contact.isStarred) CyberNeonGreen.copy(alpha = 0.4f) else CyberNeonCyan.copy(alpha = 0.3f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (contact.isStarred) Icons.Default.Star else Icons.Default.Person,
                                                contentDescription = "Contact Avatar",
                                                tint = if (contact.isStarred) CyberNeonGreen else CyberNeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = contact.displayName,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = contact.phoneNumber.ifEmpty { "No phone" },
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray
                                                )
                                                if (contact.email.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "• ${contact.email}",
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color.Gray.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Quick search stimulus trigger
                                    IconButton(
                                        onClick = { viewModel.searchContactDirectly(contact.displayName) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("btn_trigger_query_${contact.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.YoutubeSearchedFor,
                                            contentDescription = "Trigger Quick Inquiry",
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
                                text = "Zero matches found in active contact index.",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Recent Contacts Panel (Memory cache panel) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("recent_contacts_memory_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0x990A0E29)),
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
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECENT CONTACTS CACHE",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(CyberNeonMagenta.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MEM_ACTIVE",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonMagenta,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Saved Last Searched Contact Metadata
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "LAST SEARCHED CONTACT IN COGNITION",
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (assistantMemory.lastSearchedContact.isNotEmpty()) {
                                assistantMemory.lastSearchedContact
                            } else {
                                "N/A - Zero contact inquiries mapped"
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (assistantMemory.lastSearchedContact.isNotEmpty()) CyberNeonGreen else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Recent Searches Buffer
                    Text(
                        text = "RECENT SEARCH RECIPIENTS BUFFER",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val searchesList = assistantMemory.recentContactSearches
                        .split("\n")
                        .filter { it.isNotBlank() }

                    if (searchesList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            searchesList.forEach { searchName ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(CyberNeonCyan, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = searchName,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Memory buffer clear. No contact targets indexed.",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // --- 5. Communication Hub Panel ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(16.dp))
                    .testTag("communication_hub_history_panel"),
                colors = CardDefaults.cardColors(containerColor = Color(0x990A0E29)),
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
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COMMUNICATION HISTORY LOG",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Simulator trigger
                            IconButton(
                                onClick = { showSimCallDialog = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(CyberNeonGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, CyberNeonGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .testTag("btn_trigger_simulation")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Simulate incoming caller",
                                    tint = CyberNeonGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            // Erase communication search lists
                            IconButton(
                                onClick = { viewModel.clearContactSearchHistory() },
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(CyberNeonMagenta.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, CyberNeonMagenta.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .testTag("btn_erase_comms_history")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Wipe history",
                                    tint = CyberNeonMagenta,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (searchHistory.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchHistory.take(10).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                        .testTag("search_history_row_${item.id}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.contactName,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            Text(
                                                text = sdf.format(Date(item.searchTime)),
                                                fontSize = 7.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.Gray
                                            )
                                            if (item.matchedPhoneNumber != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• NUM: ${item.matchedPhoneNumber}",
                                                    fontSize = 7.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.Gray.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
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
                                text = "Telemetry communication search buffer is empty.",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }

                    // --- Legacy Comms Logger items merged beneath cleanly to keep backwards functionality active ---
                    if (rawEvents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "LEGACY CALLER TELEMETRIES",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rawEvents.take(5).forEach { ev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, CyberDarkCardBorder.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCell,
                                        contentDescription = null,
                                        tint = CyberNeonCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${ev.senderOrApp}: ${ev.title}",
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
    }

    // Interactive SIM Dialog
    if (showSimCallDialog) {
        AlertDialog(
            onDismissRequest = { showSimCallDialog = false },
            title = {
                Text(
                    text = "TRIGGER TEST INCOMING CALL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CyberNeonGreen
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Tony Stark", "Bruce Wayne", "Steve Rogers", "Peter Parker").forEach { sender ->
                        Button(
                            onClick = {
                                viewModel.simulateIncomingCall(sender)
                                showSimCallDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyberNeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Incoming: $sender",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSimCallDialog = false }) {
                    Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            },
            containerColor = Color(0xFF040616),
            shape = RoundedCornerShape(16.dp)
        )
    }
}
