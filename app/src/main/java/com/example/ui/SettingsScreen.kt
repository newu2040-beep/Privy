package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrivyBlue
import com.example.viewmodel.PrivyViewModel
import java.io.File

enum class AppThemeSetting {
    SYSTEM, LIGHT, DARK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PrivyViewModel,
    currentThemeSetting: AppThemeSetting,
    onThemeChange: (AppThemeSetting) -> Unit,
    currentPastelPalette: com.example.ui.theme.PastelPalette,
    onPastelPaletteChange: (com.example.ui.theme.PastelPalette) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val privacySettings by viewModel.privacySettings.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Appearance
        SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionChip(
                    label = "System",
                    selected = currentThemeSetting == AppThemeSetting.SYSTEM,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeChange(AppThemeSetting.SYSTEM) }
                )
                ThemeOptionChip(
                    label = "Light",
                    selected = currentThemeSetting == AppThemeSetting.LIGHT,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeChange(AppThemeSetting.LIGHT) }
                )
                ThemeOptionChip(
                    label = "Dark",
                    selected = currentThemeSetting == AppThemeSetting.DARK,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeChange(AppThemeSetting.DARK) }
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Pastel Theme Palettes
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Pastel Color Themes",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.ui.theme.PastelPalette.values().take(3).forEach { palette ->
                        PastelPaletteChip(
                            palette = palette,
                            selected = currentPastelPalette == palette,
                            modifier = Modifier.weight(1f),
                            onClick = { onPastelPaletteChange(palette) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.ui.theme.PastelPalette.values().drop(3).forEach { palette ->
                        PastelPaletteChip(
                            palette = palette,
                            selected = currentPastelPalette == palette,
                            modifier = Modifier.weight(1f),
                            onClick = { onPastelPaletteChange(palette) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy Defaults
        SettingsSectionHeader(title = "Privacy Defaults", icon = Icons.Default.Security)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingsToggleRow(
                    title = "Strip all EXIF by default",
                    subtitle = "Always remove metadata on export",
                    checked = privacySettings.stripAllMetadata,
                    onCheckedChange = { checked ->
                        viewModel.updatePrivacySettings { it.copy(stripAllMetadata = checked) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsToggleRow(
                    title = "Remove location by default",
                    subtitle = "Never leak GPS coordinates",
                    checked = privacySettings.removeLocation,
                    onCheckedChange = { checked ->
                        viewModel.updatePrivacySettings { it.copy(removeLocation = checked) }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsToggleRow(
                    title = "Secure filename by default",
                    subtitle = "Strip camera and user tokens from filenames",
                    checked = privacySettings.secureFilename,
                    onCheckedChange = { checked ->
                        viewModel.updatePrivacySettings { it.copy(secureFilename = checked) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Storage & Cache
        SettingsSectionHeader(title = "Storage & Cache", icon = Icons.Default.Storage)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Clear temporary files",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Delete intermediate export files from app cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            context.cacheDir.deleteRecursively()
                            File(context.cacheDir, "exports").mkdirs()
                            Toast.makeText(context, "Temporary files cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Clear recent history",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Removes entries from Home list (keeps your photos)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearRecentHistory()
                            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About PRIVY
        SettingsSectionHeader(title = "About PRIVY", icon = Icons.Default.Info)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "P R I V Y",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "v1.0.0 • On-Device Privacy Utility",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "«Edit it. Clean it. Share it privately.»",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PrivyBlue
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "All image processing, metadata stripping and conversion happens 100% locally on your device. No cloud storage, no tracking, and no external network calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Made with love by Rahul Shah",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrivyBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ThemeOptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrivyBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun PastelPaletteChip(
    palette: com.example.ui.theme.PastelPalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(palette.previewColor, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = palette.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
