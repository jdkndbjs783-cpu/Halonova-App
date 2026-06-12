package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as HelloNovaApplication).repository,
            application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by viewModel.preferences.collectAsState()

            // Connect dynamic theme selector to user choice stored in persistent Room sector
            MyApplicationTheme(darkTheme = preferences.darkThemeEnabled) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("splash") {
                        SplashScreen(viewModel = viewModel) { isLoggedIn ->
                            if (isLoggedIn) {
                                navController.navigate("main") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                    }

                    composable("login") {
                        LoginScreen(viewModel = viewModel) {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }

                    composable("main") {
                        MainContainer(
                            viewModel = viewModel,
                            onSignOutPressed = {
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    viewModel: MainViewModel,
    onSignOutPressed: () -> Unit
) {
    var activeTab by remember { mutableStateOf("home") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "HELLO NOVA CORE // v1.07",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column {
                // Subtle divider at the top of bottom bar to mimic border-t border-white/5
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                NavigationBar(
                    containerColor = com.example.ui.theme.CyberBottomNavBg,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("app_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = activeTab == "home",
                        onClick = { activeTab = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home tab", modifier = Modifier.size(24.dp)) },
                        label = { Text("Home", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.ui.theme.CyberNeonCyan,
                            selectedTextColor = com.example.ui.theme.CyberNeonCyan,
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f),
                            indicatorColor = com.example.ui.theme.CyberNeonCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_home_tab")
                    )

                    NavigationBarItem(
                        selected = activeTab == "settings",
                        onClick = { activeTab = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings tab", modifier = Modifier.size(24.dp)) },
                        label = { Text("Settings", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.ui.theme.CyberNeonCyan,
                            selectedTextColor = com.example.ui.theme.CyberNeonCyan,
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f),
                            indicatorColor = com.example.ui.theme.CyberNeonCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_settings_tab")
                    )

                    NavigationBarItem(
                        selected = activeTab == "profile",
                        onClick = { activeTab = "profile" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile tab", modifier = Modifier.size(24.dp)) },
                        label = { Text("Profile", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = com.example.ui.theme.CyberNeonCyan,
                            selectedTextColor = com.example.ui.theme.CyberNeonCyan,
                            unselectedIconColor = Color.White.copy(alpha = 0.4f),
                            unselectedTextColor = Color.White.copy(alpha = 0.4f),
                            indicatorColor = com.example.ui.theme.CyberNeonCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_profile_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_fade",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                "home" -> DashboardScreen(
                    viewModel = viewModel, 
                    onNavigateToPermissions = { activeTab = "permissions" },
                    onNavigateToCompanion = { activeTab = "companion" }
                )
                "settings" -> SettingsScreen(viewModel = viewModel)
                "profile" -> ProfileScreen(viewModel = viewModel, onLogoutPressed = onSignOutPressed)
                "permissions" -> PermissionCenterScreen(viewModel = viewModel, onBack = { activeTab = "home" })
                "companion" -> CompanionCenterScreen(viewModel = viewModel, onBack = { activeTab = "home" })
            }
        }
    }
}
