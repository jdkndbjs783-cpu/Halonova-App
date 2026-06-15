package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCenterScreen(
    viewModel: MainViewModel,
    isTabMode: Boolean = true,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val report by viewModel.securityReport.collectAsState()
    val logs by viewModel.serviceLogs.collectAsState()

    // Periodically refresh the report while this screen is active to keep status indicators real-time
    LaunchedEffect(Unit) {
        viewModel.syncFullSecurityReport()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SECURITY & PRIVACY HUB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    if (!isTabMode) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("perm_center_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            viewModel.syncFullSecurityReport()
                            viewModel.writeTerminalLog("PRIVACY_SYNC // Manual health scan completed.")
                        },
                        modifier = Modifier.testTag("perm_center_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Report",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("privacy_dashboard_screen"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Core Hygiene Score Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (report.securityScore >= 80) CyberNeonGreen.copy(alpha = 0.5f) else CyberNeonMagenta.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("privacy_hygiene_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF040616)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Dynamic Score Circle
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.Black, CircleShape)
                                .border(
                                    3.dp,
                                    if (report.securityScore >= 80) CyberNeonGreen else CyberNeonMagenta,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${report.securityScore}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (report.securityScore >= 80) CyberNeonGreen else CyberNeonMagenta,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "SAFETY",
                                    fontSize = 8.sp,
                                    color = Color.LightGray,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Status Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CORE HYGIENE: ${report.healthStatus}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (report.securityScore >= 80) CyberNeonGreen else CyberNeonMagenta,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Missing permissions: ${report.missingPermissionsCount} // Active sensors: ${report.activePermissionsCount}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (report.emergencyProtectionMode) {
                                    "🚨 EMERGENCY STABILIZER ACTIVE // LIMITED API"
                                } else {
                                    "✓ Privacy walls secure and fully operational."
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (report.emergencyProtectionMode) CyberNeonMagenta else CyberNeonCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 1.5 One Tap Fix Button
            if (report.missingPermissionsCount > 0) {
                item {
                    Button(
                        onClick = {
                            if (!report.microphoneGranted) viewModel.triggerPermissionRecovery("MICROPHONE")
                            else if (!report.cameraGranted) viewModel.triggerPermissionRecovery("CAMERA")
                            else if (!report.notificationsGranted) viewModel.triggerPermissionRecovery("NOTIFICATIONS")
                            else if (!report.contactsGranted) viewModel.triggerPermissionRecovery("CONTACTS")
                            else if (!report.phoneGranted) viewModel.triggerPermissionRecovery("PHONE")
                            else if (!report.smsGranted) viewModel.triggerPermissionRecovery("SMS")
                            else if (!report.overlayGranted) viewModel.triggerPermissionRecovery("OVERLAY")
                            else if (!report.accessibilityEnabled) viewModel.triggerPermissionRecovery("ACCESSIBILITY")
                            else if (!report.bluetoothGranted) viewModel.triggerPermissionRecovery("BLUETOOTH")
                            else if (!report.batteryOptimizationsUnrestricted) viewModel.triggerPermissionRecovery("BATTERY")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("one_tap_fix_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberNeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "One Tap Fix",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ONE TAP SECURITY RECOVERY (${report.missingPermissionsCount} ISSUES)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. Quick Revoke & Control Area (Requirement 7.3)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberNeonMagenta.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .testTag("quick_revoke_controls_panel"),
                    colors = CardDefaults.cardColors(containerColor = Color(0x661A1F3D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "RAPID EXTRUSION & SIM PRIVACY CONTROLS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberNeonMagenta,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Instantly trigger sandbox overrides to isolate audio, contact matrix, or overlay routines during runtime simulations.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.logPermissionStatus("ALL_CHANNELS", false)
                                    viewModel.writeTerminalLog("PRIVACY_BLOCK // Emergency override triggered. Isolating all sensors.")
                                    viewModel.voiceManager.speak("Isolating all systems. Sandboxing active!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonMagenta.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, CyberNeonMagenta),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f).height(38.dp).testTag("quick_revoke_all_btn")
                            ) {
                                Text("REVOKE ALL ACCESS", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.logPermissionStatus("ALL_CHANNELS", true)
                                    viewModel.writeTerminalLog("PRIVACY_RESTORE // Authorizing core privilege sets.")
                                    viewModel.voiceManager.speak("Re-authorizing core platform. All systems nominal.")
                                    viewModel.syncFullSecurityReport()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonGreen.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, CyberNeonGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("quick_grant_all_btn")
                            ) {
                                Text("NOMINAL RE-ENGAGE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 3. Permissions List Section Header
            item {
                Text(
                    text = "CRITICAL PRIVILEGE MATRIX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Cards for permissions
            val permissionsList = listOf(
                PermissionModel(
                    key = "MICROPHONE",
                    name = "Microphone Stream",
                    desc = "Arm background microphone loops for continuous voice activations.",
                    icon = Icons.Default.PlayArrow,
                    granted = report.microphoneGranted,
                    tag = "perm_microphone_card"
                ),
                PermissionModel(
                    key = "CAMERA",
                    name = "Camera Access",
                    desc = "Enable spatial and visual recognition overlays for advanced AI context.",
                    icon = Icons.Default.CameraAlt,
                    granted = report.cameraGranted,
                    tag = "perm_camera_card"
                ),
                PermissionModel(
                    key = "NOTIFICATIONS",
                    name = "System Notifications",
                    desc = "Maintain foreground daemon notification binding and background stability.",
                    icon = Icons.Default.Notifications,
                    granted = report.notificationsGranted,
                    tag = "perm_notifications_card"
                ),
                PermissionModel(
                    key = "ACCESSIBILITY",
                    name = "Accessibility Service",
                    desc = "Bind advanced virtual gesture layers and overlay reading routines.",
                    icon = Icons.Default.Info,
                    granted = report.accessibilityEnabled,
                    tag = "perm_accessibility_card"
                ),
                PermissionModel(
                    key = "OVERLAY",
                    name = "Overlay Permission",
                    desc = "Render voice avatars and quick trigger panels on top of active screens.",
                    icon = Icons.Default.Star,
                    granted = report.overlayGranted,
                    tag = "perm_overlay_card"
                ),
                PermissionModel(
                    key = "CONTACTS",
                    name = "Contacts Matrix",
                    desc = "Resolve caller handles into beautiful named notifications on active calls.",
                    icon = Icons.Default.Person,
                    granted = report.contactsGranted,
                    tag = "perm_contacts_card"
                ),
                PermissionModel(
                    key = "PHONE",
                    name = "Phone Telemetry",
                    desc = "Monitor ring statuses real-time to alert operators during voice sessions.",
                    icon = Icons.Default.Call,
                    granted = report.phoneGranted,
                    tag = "perm_phone_card"
                ),
                PermissionModel(
                    key = "SMS",
                    name = "SMS Reader Channel",
                    desc = "Inspect income broadcasts so assist agents can read them back customly.",
                    icon = Icons.Default.Email,
                    granted = report.smsGranted,
                    tag = "perm_sms_card"
                ),
                PermissionModel(
                    key = "BLUETOOTH",
                    name = "Bluetooth Channel",
                    desc = "Maintain wireless connection channels to hands-free headset nodes.",
                    icon = Icons.Default.Bluetooth,
                    granted = report.bluetoothGranted,
                    tag = "perm_bluetooth_card"
                ),
                PermissionModel(
                    key = "BATTERY",
                    name = "Battery Optimization",
                    desc = "Ensure the assistant background engines are unrestricted and run reliably.",
                    icon = Icons.Default.Settings,
                    granted = report.batteryOptimizationsUnrestricted,
                    tag = "perm_battery_card"
                )
            )

            items(permissionsList) { perm ->
                PermissionItemCard(
                    perm = perm,
                    onRequestValue = {
                        viewModel.triggerPermissionRecovery(perm.key)
                    }
                )
            }

            // 4. Permission Audit History (Requirement 7.4)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberDarkCardBorder, RoundedCornerShape(12.dp))
                        .testTag("privacy_audit_history_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33000000)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PRIVACY MONITOR AUDIT TIMELINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonCyan,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "PURGE LOGGER",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberNeonMagenta,
                                modifier = Modifier
                                    .clickable { viewModel.clearServiceLogs() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredLogs = logs.filter { 
                            it.eventType.contains("Permission", ignoreCase = true) || 
                            it.message.contains("Perm", ignoreCase = true) ||
                            it.eventType.contains("PRIVACY", ignoreCase = true)
                        }

                        if (filteredLogs.isEmpty()) {
                            Text(
                                text = "Audit buffer empty. Recheck permission configurations to trigger historical telemetry logs.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredLogs.take(15)) { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = log.eventType.uppercase(),
                                                    fontSize = 7.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (log.message.contains("GRANTED") || log.message.contains("RESTORE")) CyberNeonGreen else CyberNeonMagenta,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = log.message,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White
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
    }
}

data class PermissionModel(
    val key: String,
    val name: String,
    val desc: String,
    val icon: ImageVector,
    val granted: Boolean,
    val tag: String,
    val isCustomUi: Boolean = false,
    val customStatusText: String = ""
)

@Composable
fun PermissionItemCard(
    perm: PermissionModel,
    onRequestValue: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRequestValue() }
            .testTag(perm.tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (perm.granted) CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonMagenta.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = perm.icon,
                    contentDescription = perm.name,
                    tint = if (perm.granted) CyberNeonGreen else CyberNeonMagenta,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = perm.name.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    
                    val statusText = if (perm.isCustomUi) perm.customStatusText else if (perm.granted) "GRANTED" else "DISCONNECTED"
                    val statusColor = if (perm.granted) CyberNeonCyan else CyberNeonMagenta

                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = perm.desc,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
