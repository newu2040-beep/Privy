package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrivyBlue
import com.example.ui.theme.PrivyGreen
import com.example.viewmodel.PrivyViewModel

@Composable
fun PrivacyScreen(
    viewModel: PrivyViewModel,
    onBack: () -> Unit,
    onNavigateToExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val metadataReport by viewModel.metadataReport.collectAsState()
    val privacySettings by viewModel.privacySettings.collectAsState()
    val isCleaned by viewModel.isMetadataCleaned.collectAsState()

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
                    .testTag("privacy_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Remove hidden information before sharing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Thumbnail Photo Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 8.5f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            if (displayBitmap != null) {
                Image(
                    bitmap = displayBitmap!!.asImageBitmap(),
                    contentDescription = "Photo thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Check Status Pill
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(PrivyGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isCleaned) "Privacy check: Cleaned & ready" else "Privacy check",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isCleaned) "All personal metadata will be stripped upon export" else "Ready to clean ${metadataReport.detectedTagsCount} detected tags",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detected Metadata Summary Card
        if (metadataReport.detectedTagsCount > 0) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrivyBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detected in photo (${metadataReport.detectedTagsCount} items)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    metadataReport.locationString?.let {
                        MetadataTagRow(label = "GPS Location", value = it)
                    }
                    metadataReport.cameraModel?.let {
                        MetadataTagRow(label = "Camera", value = "${metadataReport.cameraMake ?: ""} $it".trim())
                    }
                    metadataReport.lensModel?.let {
                        MetadataTagRow(label = "Lens", value = it)
                    }
                    metadataReport.dateTime?.let {
                        MetadataTagRow(label = "Date & Time", value = it)
                    }
                    metadataReport.software?.let {
                        MetadataTagRow(label = "Software", value = it)
                    }
                    if (metadataReport.fNumber != null || metadataReport.iso != null) {
                        MetadataTagRow(
                            label = "Exposure",
                            value = "${metadataReport.fNumber ?: ""} ISO ${metadataReport.iso ?: ""} ${metadataReport.exposureTime ?: ""}".trim()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Privacy Toggle Items
        PrivacyToggleItem(
            icon = Icons.Default.LocationOn,
            title = "Remove Location",
            description = "Remove GPS coordinates from the photo.",
            checked = privacySettings.removeLocation,
            onCheckedChange = { checked ->
                viewModel.updatePrivacySettings { it.copy(removeLocation = checked) }
            }
        )

        PrivacyToggleItem(
            icon = Icons.Default.Description,
            title = "Strip All Metadata",
            description = "Remove embedded EXIF information.",
            checked = privacySettings.stripAllMetadata,
            onCheckedChange = { checked ->
                viewModel.updatePrivacySettings { it.copy(stripAllMetadata = checked) }
            }
        )

        PrivacyToggleItem(
            icon = Icons.Default.CameraAlt,
            title = "Remove Camera Info",
            description = "Remove camera model, lens and device details.",
            checked = privacySettings.removeCameraInfo,
            onCheckedChange = { checked ->
                viewModel.updatePrivacySettings { it.copy(removeCameraInfo = checked) }
            }
        )

        PrivacyToggleItem(
            icon = Icons.Default.CalendarToday,
            title = "Remove Date & Time",
            description = "Remove embedded capture information.",
            checked = privacySettings.removeDateTime,
            onCheckedChange = { checked ->
                viewModel.updatePrivacySettings { it.copy(removeDateTime = checked) }
            }
        )

        PrivacyToggleItem(
            icon = Icons.Default.Security,
            title = "Secure Filename",
            description = "Generate a clean filename without personal information.",
            checked = privacySettings.secureFilename,
            onCheckedChange = { checked ->
                viewModel.updatePrivacySettings { it.copy(secureFilename = checked) }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action: Remove All Metadata
        Button(
            onClick = {
                viewModel.markMetadataCleaned()
                onNavigateToExport()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("remove_all_metadata_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = if (isCleaned) "Metadata Cleaned ✓ Continue to Export" else "Remove All Metadata",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Your original photo stays untouched.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MetadataTagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PrivacyToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
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
