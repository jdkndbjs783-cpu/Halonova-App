package com.example.ui.screens

import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MainViewModel

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onLogoutPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferences.collectAsState()

    var usernameInput by remember { mutableStateOf("") }
    var selectedAvatarId by remember { mutableStateOf("") }

    // Sync state
    LaunchedEffect(preferences) {
        usernameInput = preferences.username
        selectedAvatarId = preferences.avatarId
    }

    var showProfileUpdatedFeedback by remember { mutableStateOf(false) }

    // Cyber Avatar list definition with core icons
    val avatarOptions = listOf(
        AvatarOption("cyborg", "Cyborg Scout", Icons.Default.Build, "Cyber-synthetic mechanical scout"),
        AvatarOption("companion", "AI Companion", Icons.Default.Face, "Advanced cognitive construct"),
        AvatarOption("hacker", "Net Runner", Icons.Default.Send, "Grid encryption analyst"),
        AvatarOption("ghost", "Phantom Spy", Icons.Default.Lock, "Stealth operations unit"),
        AvatarOption("commander", "Commander", Icons.Default.Star, "Core console administrator")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("profile_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title
        item {
            Text(
                text = "OPERATOR PROFILE",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Active Avatar Display
        item {
            val activeOption = avatarOptions.firstOrNull { it.id == selectedAvatarId } ?: avatarOptions[0]
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
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(CyberNeonCyan.copy(alpha = 0.12f), RoundedCornerShape(35.dp))
                            .border(1.5.dp, CyberNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(35.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeOption.icon,
                            contentDescription = activeOption.name,
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = preferences.username.uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = activeOption.name.uppercase() + " // " + activeOption.desc.uppercase(),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Handle Change textfield
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
                        text = "MANAGE OPERATOR PROTOCOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Display Username", fontFamily = FontFamily.Monospace) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberDarkCardBorder,
                            focusedLabelColor = CyberNeonCyan
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Avatar GRID Choice label
        item {
            Text(
                text = "AVAILABLE AVATAR PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // Custom list item loops using size-indexed loop for failproof compilation
        items(avatarOptions.size) { index ->
            val option = avatarOptions[index]
            val isSelected = option.id == selectedAvatarId
            val borderColor = if (isSelected) CyberNeonCyan.copy(alpha = 0.5f) else CyberDarkCardBorder
            val bgColor = if (isSelected) CyberNeonCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable { selectedAvatarId = option.id }
                    .padding(12.dp)
                    .testTag("avatar_option_${option.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.name,
                    tint = if (isSelected) CyberNeonCyan else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = option.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) CyberNeonCyan else Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = option.desc,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Save Action & Sign out actions
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (usernameInput.trim().isNotEmpty()) {
                        viewModel.updateProfile(usernameInput.trim(), selectedAvatarId)
                        showProfileUpdatedFeedback = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("update_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SYNC OPERATOR DATABASE",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black,
                    fontSize = 12.sp
                )
            }
        }

        // Disconnect logout button
        item {
            OutlinedButton(
                onClick = {
                    viewModel.performLogout()
                    onLogoutPressed()
                },
                border = BorderStroke(1.dp, CyberNeonMagenta),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberNeonMagenta),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "DETACH OPERATOR LINK (LOGOUT)",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        // Update feedback snackbar
        if (showProfileUpdatedFeedback) {
            item {
                Snackbar(
                    action = {
                        TextButton(onClick = { showProfileUpdatedFeedback = false }) {
                            Text("OK", color = CyberNeonCyan, fontFamily = FontFamily.Monospace)
                        }
                    },
                    containerColor = Color(0xFF040616),
                    contentColor = CyberNeonCyan,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "SYS_SYNC: Operator node synced successfully.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private data class AvatarOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val desc: String
)
