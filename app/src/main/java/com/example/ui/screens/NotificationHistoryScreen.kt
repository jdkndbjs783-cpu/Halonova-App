package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import com.example.data.NotificationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notificationsHistory.collectAsState()

    var showSimDialog by remember { mutableStateOf(false) }
    var simSender by remember { mutableStateOf("Slack") }
    var simTitle by remember { mutableStateOf("Urgent Deployment Request") }
    var simMessage by remember { mutableStateOf("Core systems require operational approval for Phase 5 release.") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xA0A0E29)) // Central cyber background
            .padding(16.dp)
            .testTag("notification_history_screen")
    ) {
        // HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NOTIFICATION INTERCEPT ARCHIVE",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x00E5FF), // Neon Cyan
                    letterSpacing = 1.sp
                )
                Text(
                    text = "SEC_SYS // Operational notification flow captures",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Trigger Simulated alert
                IconButton(
                    onClick = { showSimDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x00E5FF).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x00E5FF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .testTag("notif_simulate_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Simulate mock alert",
                        tint = Color(0x00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete Archive Button
                IconButton(
                    onClick = { viewModel.clearNotifications() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF007F).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF007F).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .testTag("notif_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Purge Archive",
                        tint = Color(0xFFFF007F), // Neon Magenta
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RECENT NOTIFICATIONS LIST
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "NO INBOUND SECTOR ALERTS DETECTED",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use the simulator trigger top-right, or send system messages from other applications to see notification packages populate here.",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    NotificationCard(item = item)
                }
            }
        }
    }

    // SIMULATED ALERT DIALOG
    if (showSimDialog) {
        AlertDialog(
            onDismissRequest = { showSimDialog = false },
            title = {
                Text(
                    text = "INJECT MOCK ALERT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0x00E5FF)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = simSender,
                        onValueChange = { simSender = it },
                        label = { Text("App Source", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = simTitle,
                        onValueChange = { simTitle = it },
                        label = { Text("Notification Title", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = simMessage,
                        onValueChange = { simMessage = it },
                        label = { Text("Alert Text content", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.simulateIncomingNotification(simSender, simTitle, simMessage)
                        showSimDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x00E5FF))
                ) {
                    Text("INJECT", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSimDialog = false }) {
                    Text("CANCEL", color = Color.Gray, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = Color(0xFF101432),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun NotificationCard(item: NotificationEntity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss // yyyy-MM-dd", Locale.getDefault()) }
    val timeStr = remember(item.timestamp) { formatter.format(Date(item.timestamp)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Application source badge
                Box(
                    modifier = Modifier
                        .background(Color(0x00E5FF).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0x00E5FF).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.appName.uppercase(),
                        color = Color(0x00E5FF),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = timeStr,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.message,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray
            )
        }
    }
}
