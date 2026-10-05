package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrivyBlue
import com.example.ui.theme.PrivyTheme
import com.example.viewmodel.PrivyViewModel

enum class Screen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    EDIT("Edit", Icons.Default.Edit),
    PRIVACY("Privacy", Icons.Default.Security),
    EXPORT("Export", Icons.Default.IosShare),
    SETTINGS("Settings", Icons.Default.Home)
}

@Composable
fun PrivyApp(
    viewModel: PrivyViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var previousScreen by remember { mutableStateOf(Screen.HOME) }
    var themeSetting by remember { mutableStateOf(AppThemeSetting.SYSTEM) }

    val isDark = when (themeSetting) {
        AppThemeSetting.SYSTEM -> isSystemInDarkTheme()
        AppThemeSetting.LIGHT -> false
        AppThemeSetting.DARK -> true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()
    val pastelPalette by viewModel.pastelPalette.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            previousScreen = currentScreen
            currentScreen = screen
        }
    }

    // Hardware / gesture back handling
    if (currentScreen != Screen.HOME) {
        BackHandler {
            currentScreen = if (currentScreen == Screen.SETTINGS) {
                previousScreen
            } else {
                Screen.HOME
            }
        }
    }

    PrivyTheme(darkTheme = isDark, pastelPalette = pastelPalette) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Show bottom navigation bar only for top-level screens
                if (currentScreen != Screen.SETTINGS) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        listOf(Screen.HOME, Screen.EDIT, Screen.PRIVACY, Screen.EXPORT).forEach { screen ->
                            NavigationBarItem(
                                selected = currentScreen == screen,
                                onClick = { navigateTo(screen) },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.label,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrivyBlue,
                                    selectedTextColor = PrivyBlue,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToEdit = { navigateTo(Screen.EDIT) },
                        onNavigateToPrivacy = { navigateTo(Screen.PRIVACY) },
                        onNavigateToSettings = { navigateTo(Screen.SETTINGS) }
                    )

                    Screen.EDIT -> EditorScreen(
                        viewModel = viewModel,
                        onBack = { navigateTo(Screen.HOME) }
                    )

                    Screen.PRIVACY -> PrivacyScreen(
                        viewModel = viewModel,
                        onBack = { navigateTo(Screen.HOME) },
                        onNavigateToExport = { navigateTo(Screen.EXPORT) }
                    )

                    Screen.EXPORT -> ExportScreen(
                        viewModel = viewModel,
                        onBack = { navigateTo(Screen.HOME) }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        currentThemeSetting = themeSetting,
                        onThemeChange = { themeSetting = it },
                        currentPastelPalette = pastelPalette,
                        onPastelPaletteChange = { viewModel.setPastelPalette(it) },
                        onBack = { navigateTo(previousScreen) }
                    )
                }
            }
        }
    }
}
