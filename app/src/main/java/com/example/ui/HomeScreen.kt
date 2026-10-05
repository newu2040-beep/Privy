package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.model.EditTool
import com.example.model.RecentProject
import com.example.ui.theme.PrivyBlue
import com.example.ui.theme.PrivyDarkCharcoal
import com.example.viewmodel.PrivyViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PrivyViewModel,
    onNavigateToEdit: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val currentFilename by viewModel.currentFilename.collectAsState()
    val recentProjects by viewModel.recentProjects.collectAsState()

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var hasGalleryPermission by remember {
        mutableStateOf(
            requiredPermissions.any { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromUri(uri)
            onNavigateToEdit()
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasGalleryPermission = results.values.any { it }
        // Launch photo picker immediately after granting
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "P R I V Y",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Hero Typography
        Text(
            text = "Photos, made private.",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Edit, protect and export your photos without the clutter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Hero Photo Preview Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9.5f)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onNavigateToEdit() }
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap!!.asImageBitmap(),
                        contentDescription = "Active photo preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_sample_mountains),
                        contentDescription = "Sample Mountains",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Active filename pill badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentFilename,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Action: Import Photo Button
        Button(
            onClick = {
                if (!hasGalleryPermission) {
                    permissionsLauncher.launch(requiredPermissions)
                } else {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("import_photo_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Import Photo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Full Gallery Access Status Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    permissionsLauncher.launch(requiredPermissions)
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasGalleryPermission) Icons.Default.CheckCircle else Icons.Default.FolderShared,
                        contentDescription = null,
                        tint = if (hasGalleryPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasGalleryPermission) "Full gallery access active" else "Request full gallery access",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!hasGalleryPermission) {
                    Text(
                        text = "Grant",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Quick Tools Section
        Text(
            text = "Quick tools",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickToolCard(
                title = "Blur",
                icon = Icons.Default.BlurOn,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.setTool(EditTool.BLUR)
                    onNavigateToEdit()
                }
            )
            QuickToolCard(
                title = "Mosaic",
                icon = Icons.Default.GridOn,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.setTool(EditTool.MOSAIC)
                    onNavigateToEdit()
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickToolCard(
                title = "Blackout",
                icon = Icons.Default.Lock,
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.setTool(EditTool.BLACKOUT)
                    onNavigateToEdit()
                }
            )
            QuickToolCard(
                title = "Metadata",
                icon = Icons.Default.Description,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToPrivacy
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sample Privacy Photos Row (Quick test without uploading)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Try sample photos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tap to load",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                SamplePhotoCard(
                    title = "Landscape",
                    filename = "IMG_2048.JPG",
                    drawableId = R.drawable.img_sample_mountains,
                    onClick = {
                        viewModel.loadSampleImage("mountains")
                        onNavigateToEdit()
                    }
                )
            }
            item {
                SamplePhotoCard(
                    title = "Car Plate",
                    filename = "IMG_1876.JPG",
                    drawableId = R.drawable.img_sample_street,
                    onClick = {
                        viewModel.loadSampleImage("street")
                        onNavigateToEdit()
                    }
                )
            }
            item {
                SamplePhotoCard(
                    title = "Portrait Face",
                    filename = "IMG_1563.JPG",
                    drawableId = R.drawable.img_sample_portrait,
                    onClick = {
                        viewModel.loadSampleImage("portrait")
                        onNavigateToEdit()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Recent Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (recentProjects.isNotEmpty()) {
                Text(
                    text = "Clear all",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { viewModel.clearRecentHistory() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recentProjects.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nothing here yet.",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Import a photo to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentProjects, key = { it.id }) { project ->
                    RecentProjectCard(
                        project = project,
                        onDelete = { viewModel.deleteRecentProject(project) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun QuickToolCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SamplePhotoCard(
    title: String,
    filename: String,
    drawableId: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(135.dp)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Column {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = filename,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentProjectCard(
    project: RecentProject,
    onDelete: () -> Unit
) {
    val dateStr = remember(project.createdAt) {
        val diff = System.currentTimeMillis() - project.createdAt
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> SimpleDateFormat("MMM d", Locale.US).format(Date(project.createdAt))
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .width(135.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
            ) {
                if (project.previewPath != null && File(project.previewPath).exists()) {
                    AsyncImage(
                        model = File(project.previewPath),
                        contentDescription = project.filename,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.padding(3.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = project.filename,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateStr • ${project.format}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
