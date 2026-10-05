package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlackoutShape
import com.example.model.EditTool
import com.example.model.HideMode
import com.example.model.PointD
import com.example.ui.theme.PrivyBlue
import com.example.ui.theme.PrivyDarkCharcoal
import com.example.viewmodel.PrivyViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: PrivyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val currentFilename by viewModel.currentFilename.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val hideMode by viewModel.hideMode.collectAsState()

    val editMasks by viewModel.editMasks.collectAsState()
    val redoStack by viewModel.redoStack.collectAsState()

    val blurStrength by viewModel.blurStrength.collectAsState()
    val pixelSizeRatio by viewModel.pixelSizeRatio.collectAsState()
    val motionStrength by viewModel.motionStrength.collectAsState()
    val blackoutColor by viewModel.blackoutColor.collectAsState()
    val blackoutShape by viewModel.blackoutShape.collectAsState()
    val brushRadiusRatio by viewModel.brushRadiusRatio.collectAsState()
    val adjustments by viewModel.adjustments.collectAsState()

    // Canvas Zoom & Pan State
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isPanZoomMode by remember { mutableStateOf(false) }

    // Brush cursor indicator state
    var cursorPosition by remember { mutableStateOf<Offset?>(null) }
    var isDrawing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("editor_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = currentFilename,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pan/Zoom toggle
                IconButton(
                    onClick = { isPanZoomMode = !isPanZoomMode },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Pan and Zoom mode",
                        tint = if (isPanZoomMode) PrivyBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Undo
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = editMasks.isNotEmpty(),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (editMasks.isNotEmpty()) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }

                // Redo
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }

                // Reset all edits
                IconButton(
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                        viewModel.clearAllEdits()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset edits",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Center: Interactive Image Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(Color(0xFF090A0C)),
            contentAlignment = Alignment.Center
        ) {
            if (displayBitmap != null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = panOffset.x,
                            translationY = panOffset.y
                        )
                        .pointerInput(isPanZoomMode) {
                            if (isPanZoomMode) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(0.7f, 6.0f)
                                    panOffset += pan
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val containerWidth = with(density) { maxWidth.toPx() }
                    val containerHeight = with(density) { maxHeight.toPx() }
                    val bmp = displayBitmap!!

                    // Compute fitted image bounds inside container
                    val imgAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    val containerAspect = containerWidth / containerHeight

                    val drawnW: Float
                    val drawnH: Float
                    if (imgAspect > containerAspect) {
                        drawnW = containerWidth
                        drawnH = containerWidth / imgAspect
                    } else {
                        drawnW = containerHeight * imgAspect
                        drawnH = containerHeight
                    }

                    val imageLeft = (containerWidth - drawnW) / 2f
                    val imageTop = (containerHeight - drawnH) / 2f

                    val drawnWDp = with(density) { drawnW.toDp() }
                    val drawnHDp = with(density) { drawnH.toDp() }

                    // Display Bitmap
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Photo editing canvas",
                        modifier = Modifier
                            .size(drawnWDp, drawnHDp)
                    )

                    // Touch drawing overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isPanZoomMode, currentTool) {
                                if (!isPanZoomMode && currentTool != EditTool.CROP && currentTool != EditTool.ADJUST) {
                                    val currentStroke = mutableListOf<PointD>()
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            isDrawing = true
                                            cursorPosition = offset
                                            val normX = ((offset.x - imageLeft) / drawnW).coerceIn(0f, 1f)
                                            val normY = ((offset.y - imageTop) / drawnH).coerceIn(0f, 1f)
                                            currentStroke.clear()
                                            currentStroke.add(PointD(normX, normY))
                                            viewModel.updateActivePoints(currentStroke.toList())
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val offset = change.position
                                            cursorPosition = offset
                                            val normX = ((offset.x - imageLeft) / drawnW).coerceIn(0f, 1f)
                                            val normY = ((offset.y - imageTop) / drawnH).coerceIn(0f, 1f)
                                            currentStroke.add(PointD(normX, normY))
                                            viewModel.updateActivePoints(currentStroke.toList())
                                        },
                                        onDragEnd = {
                                            isDrawing = false
                                            cursorPosition = null
                                            viewModel.commitCurrentStroke()
                                            currentStroke.clear()
                                        },
                                        onDragCancel = {
                                            isDrawing = false
                                            cursorPosition = null
                                            viewModel.updateActivePoints(emptyList())
                                            currentStroke.clear()
                                        }
                                    )
                                }
                            }
                    ) {
                        // Draw brush size cursor circle when touching
                        if (isDrawing && cursorPosition != null) {
                            val brushPx = brushRadiusRatio * maxOf(drawnW, drawnH)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = brushPx,
                                center = cursorPosition!!,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.35f),
                                radius = brushPx + 1.dp.toPx(),
                                center = cursorPosition!!,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Quick Fit Button (resets zoom when zoomed in)
            if (zoomScale != 1f || panOffset != Offset.Zero) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clickable {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                ) {
                    Text(
                        text = "Reset Zoom",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Secondary Context-Sensitive Controls
        SecondaryToolControls(
            currentTool = currentTool,
            hideMode = hideMode,
            blurStrength = blurStrength,
            pixelSizeRatio = pixelSizeRatio,
            motionStrength = motionStrength,
            blackoutColor = blackoutColor,
            blackoutShape = blackoutShape,
            brushRadiusRatio = brushRadiusRatio,
            adjustments = adjustments,
            onBlurStrengthChange = { viewModel.setBlurStrength(it) },
            onPixelSizeChange = { viewModel.setPixelSizeRatio(it) },
            onMotionStrengthChange = { viewModel.setMotionStrength(it) },
            onBlackoutColorChange = { viewModel.setBlackoutColor(it) },
            onBlackoutShapeChange = { viewModel.setBlackoutShape(it) },
            onBrushRadiusChange = { viewModel.setBrushRadiusRatio(it) },
            onHideModeChange = { viewModel.setHideMode(it) },
            onRotate90 = { viewModel.rotate90() },
            onFlipH = { viewModel.flipHorizontal() },
            onFlipV = { viewModel.flipVertical() },
            onAdjustmentsChange = { b, c, s -> viewModel.setAdjustments(b, c, s) }
        )

        // Bottom Tool Selector
        BottomToolBar(
            currentTool = currentTool,
            onToolSelect = {
                viewModel.setTool(it)
                isPanZoomMode = false
            }
        )
    }
}

@Composable
fun SecondaryToolControls(
    currentTool: EditTool,
    hideMode: HideMode,
    blurStrength: Float,
    pixelSizeRatio: Float,
    motionStrength: Float,
    blackoutColor: Long,
    blackoutShape: BlackoutShape,
    brushRadiusRatio: Float,
    adjustments: com.example.model.ColorAdjustments,
    onBlurStrengthChange: (Float) -> Unit,
    onPixelSizeChange: (Float) -> Unit,
    onMotionStrengthChange: (Float) -> Unit,
    onBlackoutColorChange: (Long) -> Unit,
    onBlackoutShapeChange: (BlackoutShape) -> Unit,
    onBrushRadiusChange: (Float) -> Unit,
    onHideModeChange: (HideMode) -> Unit,
    onRotate90: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onAdjustmentsChange: (Float, Float, Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            when (currentTool) {
                EditTool.BLUR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Blur strength",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(blurStrength * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Slider(
                        value = blurStrength,
                        onValueChange = onBlurStrengthChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = PrivyBlue
                        )
                    )
                }

                EditTool.MOSAIC -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pixel size",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(pixelSizeRatio * 1000).roundToInt()} px",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Slider(
                        value = pixelSizeRatio,
                        onValueChange = onPixelSizeChange,
                        valueRange = 0.015f..0.075f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = PrivyBlue
                        )
                    )
                }

                EditTool.MOTION -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Motion strength",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(motionStrength * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Slider(
                        value = motionStrength,
                        onValueChange = onMotionStrengthChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onSurface,
                            activeTrackColor = PrivyBlue
                        )
                    )
                }

                EditTool.BLACKOUT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Freehand vs Rectangle
                            FilterChip(
                                selected = blackoutShape == BlackoutShape.FREEHAND,
                                label = "Brush",
                                onClick = { onBlackoutShapeChange(BlackoutShape.FREEHAND) }
                            )
                            FilterChip(
                                selected = blackoutShape == BlackoutShape.RECTANGLE,
                                label = "Rectangle",
                                onClick = { onBlackoutShapeChange(BlackoutShape.RECTANGLE) }
                            )
                        }

                        // Color selection
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorChoiceDot(0xFF000000, blackoutColor == 0xFF000000) { onBlackoutColorChange(0xFF000000) }
                            ColorChoiceDot(0xFF1E293B, blackoutColor == 0xFF1E293B) { onBlackoutColorChange(0xFF1E293B) }
                            ColorChoiceDot(0xFFFFFFFF, blackoutColor == 0xFFFFFFFF) { onBlackoutColorChange(0xFFFFFFFF) }
                            ColorChoiceDot(0xFFDC2626, blackoutColor == 0xFFDC2626) { onBlackoutColorChange(0xFFDC2626) }
                        }
                    }
                }

                EditTool.HIDE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Concealment mode:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = hideMode == HideMode.BLUR,
                                label = "Blur",
                                onClick = { onHideModeChange(HideMode.BLUR) }
                            )
                            FilterChip(
                                selected = hideMode == HideMode.MOSAIC,
                                label = "Mosaic",
                                onClick = { onHideModeChange(HideMode.MOSAIC) }
                            )
                            FilterChip(
                                selected = hideMode == HideMode.BLACKOUT,
                                label = "Blackout",
                                onClick = { onHideModeChange(HideMode.BLACKOUT) }
                            )
                        }
                    }
                }

                EditTool.CROP -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionButton(
                            icon = Icons.Default.CropRotate,
                            label = "Rotate 90°",
                            onClick = onRotate90
                        )
                        ActionButton(
                            icon = Icons.Default.Flip,
                            label = "Flip H",
                            onClick = onFlipH
                        )
                        ActionButton(
                            icon = Icons.Default.Flip,
                            label = "Flip V",
                            onClick = onFlipV
                        )
                    }
                }

                EditTool.ADJUST -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Brightness", style = MaterialTheme.typography.bodySmall)
                            Text("${adjustments.brightness.roundToInt()}", style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = adjustments.brightness,
                            onValueChange = { onAdjustmentsChange(it, adjustments.contrast, adjustments.saturation) },
                            valueRange = -60f..60f,
                            colors = SliderDefaults.colors(activeTrackColor = PrivyBlue)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Contrast", style = MaterialTheme.typography.bodySmall)
                            Text(String.format(java.util.Locale.US, "%.1fx", adjustments.contrast), style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = adjustments.contrast,
                            onValueChange = { onAdjustmentsChange(adjustments.brightness, it, adjustments.saturation) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(activeTrackColor = PrivyBlue)
                        )
                    }
                }
            }

            // Brush size slider for paintable tools
            if (currentTool in listOf(EditTool.BLUR, EditTool.MOSAIC, EditTool.MOTION, EditTool.BLACKOUT, EditTool.HIDE)) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brush size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(brushRadiusRatio * 1000).roundToInt()} pt",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = brushRadiusRatio,
                    onValueChange = onBrushRadiusChange,
                    valueRange = 0.015f..0.12f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun FilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ColorChoiceDot(
    colorHex: Long,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(colorHex))
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) PrivyBlue else Color.Gray.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
fun BottomToolBar(
    currentTool: EditTool,
    onToolSelect: (EditTool) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolBarItem(
            tool = EditTool.BLUR,
            icon = Icons.Default.BlurOn,
            isSelected = currentTool == EditTool.BLUR,
            onClick = { onToolSelect(EditTool.BLUR) }
        )
        ToolBarItem(
            tool = EditTool.MOSAIC,
            icon = Icons.Default.GridOn,
            isSelected = currentTool == EditTool.MOSAIC,
            onClick = { onToolSelect(EditTool.MOSAIC) }
        )
        ToolBarItem(
            tool = EditTool.MOTION,
            icon = Icons.Default.Waves,
            isSelected = currentTool == EditTool.MOTION,
            onClick = { onToolSelect(EditTool.MOTION) }
        )
        ToolBarItem(
            tool = EditTool.BLACKOUT,
            icon = Icons.Default.Lock,
            isSelected = currentTool == EditTool.BLACKOUT,
            onClick = { onToolSelect(EditTool.BLACKOUT) }
        )
        ToolBarItem(
            tool = EditTool.HIDE,
            icon = Icons.Default.VisibilityOff,
            isSelected = currentTool == EditTool.HIDE,
            onClick = { onToolSelect(EditTool.HIDE) }
        )
        ToolBarItem(
            tool = EditTool.CROP,
            icon = Icons.Default.Crop,
            isSelected = currentTool == EditTool.CROP,
            onClick = { onToolSelect(EditTool.CROP) }
        )
        ToolBarItem(
            tool = EditTool.ADJUST,
            icon = Icons.Default.Tune,
            isSelected = currentTool == EditTool.ADJUST,
            onClick = { onToolSelect(EditTool.ADJUST) }
        )
    }
}

@Composable
fun ToolBarItem(
    tool: EditTool,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = tool.label,
                    tint = if (isSelected) PrivyBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
