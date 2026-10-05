package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HighlightAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlackoutShape
import com.example.model.ColorAdjustments
import com.example.model.ColorGradingPreset
import com.example.model.EditTool
import com.example.model.HideMode
import com.example.model.PointD
import com.example.model.PrivacyEffectType
import com.example.model.PrivacyStampType
import com.example.model.SelectionShape
import com.example.ui.theme.PrivyBlue
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
    val originalBitmap by viewModel.originalDisplayBitmap.collectAsState()
    val currentFilename by viewModel.currentFilename.collectAsState()
    val currentTool by viewModel.currentTool.collectAsState()
    val hideMode by viewModel.hideMode.collectAsState()

    // Custom selection states
    val selectionShape by viewModel.selectionShape.collectAsState()
    val selectedPrivacyEffect by viewModel.selectedPrivacyEffect.collectAsState()
    val selectionRadiusRatio by viewModel.selectionRadiusRatio.collectAsState()

    val editMasks by viewModel.editMasks.collectAsState()
    val brushRadiusRatio by viewModel.brushRadiusRatio.collectAsState()
    val blurStrength by viewModel.blurStrength.collectAsState()
    val pixelSizeRatio by viewModel.pixelSizeRatio.collectAsState()
    val motionStrength by viewModel.motionStrength.collectAsState()
    val motionAngle by viewModel.motionAngle.collectAsState()
    val scrambleDensity by viewModel.scrambleDensity.collectAsState()
    val glitchIntensity by viewModel.glitchIntensity.collectAsState()
    val blackoutColor by viewModel.blackoutColor.collectAsState()
    val blackoutShape by viewModel.blackoutShape.collectAsState()
    val currentStamp by viewModel.currentStamp.collectAsState()
    val stampRotation by viewModel.stampRotation.collectAsState()
    val stampColor by viewModel.stampColor.collectAsState()
    val adjustments by viewModel.adjustments.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()
    val compactModeSetting by viewModel.compactMode.collectAsState()

    // Canvas Zoom & Pan State
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isPanZoomMode by remember { mutableStateOf(false) }
    var isComparingOriginal by remember { mutableStateOf(false) }

    // Brush cursor indicator state
    var cursorPosition by remember { mutableStateOf<Offset?>(null) }
    var isDrawing by remember { mutableStateOf(false) }

    // Color grading sub-category
    var gradingTab by remember { mutableStateOf("Basic") }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val isCompactPhone = screenHeight < 680.dp || screenWidth < 360.dp || compactModeSetting

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompactPhone) 10.dp else 16.dp,
                        vertical = if (isCompactPhone) 4.dp else 8.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(if (isCompactPhone) 38.dp else 44.dp)
                        .testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentFilename,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompactPhone) 14.sp else 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCompactPhone) {
                        Text(
                            text = "Compact View",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Before/After comparison button
                    Surface(
                        shape = CircleShape,
                        color = if (isComparingOriginal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isComparingOriginal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(if (isCompactPhone) 34.dp else 40.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isComparingOriginal = true
                                        tryAwaitRelease()
                                        isComparingOriginal = false
                                    }
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Compare original",
                                modifier = Modifier.size(if (isCompactPhone) 18.dp else 20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = editMasks.isNotEmpty(),
                        modifier = Modifier.size(if (isCompactPhone) 34.dp else 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (editMasks.isNotEmpty()) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(if (isCompactPhone) 18.dp else 20.dp)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier.size(if (isCompactPhone) 34.dp else 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(if (isCompactPhone) 18.dp else 20.dp)
                        )
                    }
                }
            }

            // Interactive Editor Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompactPhone) 8.dp else 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(if (isCompactPhone) 14.dp else 20.dp))
                    .background(Color(0xFF0F1115))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(if (isCompactPhone) 14.dp else 20.dp)
                    )
            ) {
                val activeBitmap = if (isComparingOriginal && originalBitmap != null) originalBitmap else displayBitmap

                if (activeBitmap != null) {
                    val imgAspect = activeBitmap.width.toFloat() / activeBitmap.height.toFloat().coerceAtLeast(1f)

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(imgAspect, matchHeightConstraintsFirst = (maxWidth / maxHeight > imgAspect))
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = panOffset.x,
                                    translationY = panOffset.y
                                )
                                .then(
                                    if (isPanZoomMode) {
                                        Modifier.pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                zoomScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                                panOffset = Offset(
                                                    x = (panOffset.x + pan.x).coerceIn(-1000f, 1000f),
                                                    y = (panOffset.y + pan.y).coerceIn(-1000f, 1000f)
                                                )
                                            }
                                        }
                                    } else {
                                        Modifier.pointerInput(currentTool, selectionShape, selectedPrivacyEffect, selectionRadiusRatio, brushRadiusRatio, currentStamp, stampRotation, stampColor) {
                                            if (currentTool == EditTool.SELECT && selectionShape == SelectionShape.CIRCLE_TAP) {
                                                detectTapGestures(
                                                    onPress = { offset ->
                                                        cursorPosition = offset
                                                        isDrawing = true
                                                        tryAwaitRelease()
                                                        isDrawing = false
                                                        cursorPosition = null
                                                    },
                                                    onTap = { tapOffset ->
                                                        val normX = (tapOffset.x / size.width).coerceIn(0f, 1f)
                                                        val normY = (tapOffset.y / size.height).coerceIn(0f, 1f)
                                                        viewModel.addTapCirclePrivacySpot(normX, normY)
                                                    }
                                                )
                                            } else if (currentTool == EditTool.STAMP) {
                                                detectTapGestures { tapOffset ->
                                                    val normX = (tapOffset.x / size.width).coerceIn(0f, 1f)
                                                    val normY = (tapOffset.y / size.height).coerceIn(0f, 1f)
                                                    viewModel.updateActivePoints(listOf(PointD(normX, normY)))
                                                    viewModel.commitCurrentStroke()
                                                }
                                            } else {
                                                detectDragGestures(
                                                    onDragStart = { startOffset ->
                                                        isDrawing = true
                                                        cursorPosition = startOffset
                                                        val normX = (startOffset.x / size.width).coerceIn(0f, 1f)
                                                        val normY = (startOffset.y / size.height).coerceIn(0f, 1f)
                                                        viewModel.updateActivePoints(listOf(PointD(normX, normY)))
                                                    },
                                                    onDrag = { change, _ ->
                                                        change.consume()
                                                        cursorPosition = change.position
                                                        val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                                                        val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                                                        viewModel.updateActivePoints(
                                                            viewModel.activePoints.value + PointD(normX, normY)
                                                        )
                                                    },
                                                    onDragEnd = {
                                                        isDrawing = false
                                                        cursorPosition = null
                                                        viewModel.commitCurrentStroke()
                                                    },
                                                    onDragCancel = {
                                                        isDrawing = false
                                                        cursorPosition = null
                                                        viewModel.updateActivePoints(emptyList())
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                        ) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = "Active Canvas",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Live stroke / selection preview
                            val activePts by viewModel.activePoints.collectAsState()
                            if ((activePts.isNotEmpty() || cursorPosition != null) && !isPanZoomMode) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // Live Tap Circle Reticle Indicator
                                    if (currentTool == EditTool.SELECT && selectionShape == SelectionShape.CIRCLE_TAP) {
                                        cursorPosition?.let { pos ->
                                            val r = selectionRadiusRatio * maxOf(w, h)
                                            drawCircle(
                                                color = Color(0x663B82F6),
                                                radius = r,
                                                center = pos
                                            )
                                            drawCircle(
                                                color = Color(0xFF60A5FA),
                                                radius = r,
                                                center = pos,
                                                style = Stroke(width = 3f)
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = 4f,
                                                center = pos
                                            )
                                        }
                                    }

                                    if (activePts.isNotEmpty()) {
                                        if (currentTool == EditTool.SELECT) {
                                            when (selectionShape) {
                                                SelectionShape.CIRCLE_DRAG -> {
                                                    if (activePts.size >= 2) {
                                                        val p1 = activePts.first()
                                                        val p2 = activePts.last()
                                                        val cx = (p1.x + p2.x) / 2f * w
                                                        val cy = (p1.y + p2.y) / 2f * h
                                                        val rad = maxOf(Math.abs(p2.x - p1.x) * w, Math.abs(p2.y - p1.y) * h) / 2f
                                                        drawCircle(
                                                            color = Color(0x663B82F6),
                                                            radius = rad,
                                                            center = Offset(cx, cy)
                                                        )
                                                        drawCircle(
                                                            color = Color(0xFF60A5FA),
                                                            radius = rad,
                                                            center = Offset(cx, cy),
                                                            style = Stroke(width = 4f)
                                                        )
                                                    }
                                                }
                                                SelectionShape.RECTANGLE -> {
                                                    if (activePts.size >= 2) {
                                                        val p1 = activePts.first()
                                                        val p2 = activePts.last()
                                                        val l = minOf(p1.x, p2.x) * w
                                                        val t = minOf(p1.y, p2.y) * h
                                                        val r = maxOf(p1.x, p2.x) * w
                                                        val b = maxOf(p1.y, p2.y) * h
                                                        drawRect(
                                                            color = Color(0x663B82F6),
                                                            topLeft = Offset(l, t),
                                                            size = Size(r - l, b - t)
                                                        )
                                                        drawRect(
                                                            color = Color(0xFF60A5FA),
                                                            topLeft = Offset(l, t),
                                                            size = Size(r - l, b - t),
                                                            style = Stroke(width = 4f)
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    val path = androidx.compose.ui.graphics.Path()
                                                    path.moveTo(activePts[0].x * w, activePts[0].y * h)
                                                    for (i in 1 until activePts.size) {
                                                        path.lineTo(activePts[i].x * w, activePts[i].y * h)
                                                    }
                                                    drawPath(
                                                        path = path,
                                                        color = Color(0xFF60A5FA),
                                                        style = Stroke(
                                                            width = maxOf(6f, selectionRadiusRatio * maxOf(w, h) * 2f),
                                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                        )
                                                    )
                                                }
                                            }
                                        } else if (currentTool == EditTool.BLACKOUT && blackoutShape == BlackoutShape.RECTANGLE && activePts.size >= 2) {
                                            val p1 = activePts.first()
                                            val p2 = activePts.last()
                                            val l = minOf(p1.x, p2.x) * w
                                            val t = minOf(p1.y, p2.y) * h
                                            val r = maxOf(p1.x, p2.x) * w
                                            val b = maxOf(p1.y, p2.y) * h
                                            drawRect(
                                                color = Color(blackoutColor),
                                                topLeft = Offset(l, t),
                                                size = Size(r - l, b - t)
                                            )
                                        } else if (currentTool == EditTool.BLACKOUT && blackoutShape == BlackoutShape.OVAL && activePts.size >= 2) {
                                            val p1 = activePts.first()
                                            val p2 = activePts.last()
                                            val l = minOf(p1.x, p2.x) * w
                                            val t = minOf(p1.y, p2.y) * h
                                            val r = maxOf(p1.x, p2.x) * w
                                            val b = maxOf(p1.y, p2.y) * h
                                            drawOval(
                                                color = Color(blackoutColor),
                                                topLeft = Offset(l, t),
                                                size = Size(r - l, b - t)
                                            )
                                        } else {
                                            val path = androidx.compose.ui.graphics.Path()
                                            path.moveTo(activePts[0].x * w, activePts[0].y * h)
                                            for (i in 1 until activePts.size) {
                                                path.lineTo(activePts[i].x * w, activePts[i].y * h)
                                            }
                                            drawPath(
                                                path = path,
                                                color = if (currentTool == EditTool.BLACKOUT) Color(blackoutColor) else Color.White.copy(alpha = 0.6f),
                                                style = Stroke(
                                                    width = maxOf(6f, brushRadiusRatio * maxOf(w, h) * 2f),
                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Comparison Watermark Banner
                if (isComparingOriginal) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = "ORIGINAL UNEDITED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFCD34D),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Custom Selection Prompt Hint
                if (currentTool == EditTool.SELECT) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (selectionShape) {
                                    SelectionShape.CIRCLE_TAP -> "Tap anywhere to apply ${selectedPrivacyEffect.label} circle"
                                    SelectionShape.CIRCLE_DRAG -> "Drag across photo to draw ${selectedPrivacyEffect.label} circle"
                                    SelectionShape.RECTANGLE -> "Drag across photo to draw ${selectedPrivacyEffect.label} box"
                                    SelectionShape.FREEHAND -> "Draw freehand ${selectedPrivacyEffect.label} mask"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                // Floating Action Controls: Pan/Zoom, 1x Reset, Clear All
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (editMasks.isNotEmpty()) {
                        FloatingMiniPill(
                            icon = Icons.Default.Refresh,
                            label = "Clear",
                            onClick = { viewModel.clearAllMasks() }
                        )
                    }

                    if (zoomScale > 1f) {
                        FloatingMiniPill(
                            icon = Icons.Default.RestartAlt,
                            label = "1x",
                            onClick = {
                                zoomScale = 1f
                                panOffset = Offset.Zero
                                isPanZoomMode = false
                            }
                        )
                    }

                    FloatingMiniPill(
                        icon = Icons.Default.PanTool,
                        label = if (isPanZoomMode) "Draw" else "Pan/Zoom",
                        isSelected = isPanZoomMode,
                        onClick = { isPanZoomMode = !isPanZoomMode }
                    )
                }
            }

            // Compact/Adaptive Tool Controls Drawer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompactPhone) 10.dp else 16.dp, vertical = 6.dp)
            ) {
                // Parameter Sliders for the active tool
                when (currentTool) {
                    EditTool.SELECT -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Selection Shape Row (Tap Circle, Circle Area, Rect Area, Freehand)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shape:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                SelectionShape.values().forEach { shape ->
                                    ShapeOptionChip(shape.label, selectionShape == shape) {
                                        viewModel.setSelectionShape(shape)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. Privacy Effect Row (Blur, Mosaic, Blackout, Scramble, Glitch, Motion)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Effect:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PrivacyEffectType.values().forEach { effect ->
                                    EffectOptionChip(effect.label, selectedPrivacyEffect == effect) {
                                        viewModel.setSelectedPrivacyEffect(effect)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Quick Size Presets & Slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Size:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                listOf(
                                    "Small" to 0.035f,
                                    "Medium" to 0.065f,
                                    "Large" to 0.12f,
                                    "XL" to 0.20f
                                ).forEach { (label, ratio) ->
                                    ShapeOptionChip(label, kotlin.math.abs(selectionRadiusRatio - ratio) < 0.015f) {
                                        viewModel.setSelectionRadiusRatio(ratio)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            ToolParamSlider(
                                label = if (selectionShape == SelectionShape.CIRCLE_TAP) "Spot Size" else "Selection Size",
                                value = selectionRadiusRatio,
                                valueRange = 0.015f..0.28f,
                                displayValue = "${(selectionRadiusRatio * 1000).toInt()}px",
                                onValueChange = { viewModel.setSelectionRadiusRatio(it) },
                                isCompact = isCompactPhone
                            )

                            // Effect-specific parameter control
                            when (selectedPrivacyEffect) {
                                PrivacyEffectType.BLUR -> {
                                    ToolParamSlider(
                                        label = "Blur Strength",
                                        value = blurStrength,
                                        valueRange = 0.1f..1.0f,
                                        displayValue = "${(blurStrength * 100).toInt()}%",
                                        onValueChange = { viewModel.setBlurStrength(it) },
                                        isCompact = isCompactPhone
                                    )
                                }
                                PrivacyEffectType.MOSAIC -> {
                                    ToolParamSlider(
                                        label = "Pixel Size",
                                        value = pixelSizeRatio,
                                        valueRange = 0.015f..0.08f,
                                        displayValue = "${(pixelSizeRatio * 1000).toInt()}",
                                        onValueChange = { viewModel.setPixelSizeRatio(it) },
                                        isCompact = isCompactPhone
                                    )
                                }
                                PrivacyEffectType.BLACKOUT -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Color:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        ColorDot(0xFF000000, blackoutColor == 0xFF000000) { viewModel.setBlackoutColor(0xFF000000) }
                                        ColorDot(0xFF1F2937, blackoutColor == 0xFF1F2937) { viewModel.setBlackoutColor(0xFF1F2937) }
                                        ColorDot(0xFFFFFFFF, blackoutColor == 0xFFFFFFFF) { viewModel.setBlackoutColor(0xFFFFFFFF) }
                                        ColorDot(0xFFDC2626, blackoutColor == 0xFFDC2626) { viewModel.setBlackoutColor(0xFFDC2626) }
                                        ColorDot(0xFF2563EB, blackoutColor == 0xFF2563EB) { viewModel.setBlackoutColor(0xFF2563EB) }
                                    }
                                }
                                PrivacyEffectType.SCRAMBLE -> {
                                    ToolParamSlider(
                                        label = "Grain Entropy",
                                        value = scrambleDensity,
                                        valueRange = 0.2f..1.0f,
                                        displayValue = "${(scrambleDensity * 100).toInt()}%",
                                        onValueChange = { viewModel.setScrambleDensity(it) },
                                        isCompact = isCompactPhone
                                    )
                                }
                                PrivacyEffectType.GLITCH -> {
                                    ToolParamSlider(
                                        label = "Scanline Shift",
                                        value = glitchIntensity,
                                        valueRange = 0.2f..1.0f,
                                        displayValue = "${(glitchIntensity * 100).toInt()}%",
                                        onValueChange = { viewModel.setGlitchIntensity(it) },
                                        isCompact = isCompactPhone
                                    )
                                }
                                PrivacyEffectType.MOTION -> {
                                    ToolParamSlider(
                                        label = "Smear Strength",
                                        value = motionStrength,
                                        valueRange = 0.1f..1.0f,
                                        displayValue = "${(motionStrength * 100).toInt()}%",
                                        onValueChange = { viewModel.setMotionStrength(it) },
                                        isCompact = isCompactPhone
                                    )
                                }
                            }
                        }
                    }

                    EditTool.BLUR -> {
                        ToolParamSlider(
                            label = "Blur Strength",
                            value = blurStrength,
                            valueRange = 0.1f..1.0f,
                            displayValue = "${(blurStrength * 100).toInt()}%",
                            onValueChange = { viewModel.setBlurStrength(it) },
                            isCompact = isCompactPhone
                        )
                        ToolParamSlider(
                            label = "Brush Size",
                            value = brushRadiusRatio,
                            valueRange = 0.015f..0.12f,
                            displayValue = "${(brushRadiusRatio * 1000).toInt()}px",
                            onValueChange = { viewModel.setBrushRadiusRatio(it) },
                            isCompact = isCompactPhone
                        )
                    }

                    EditTool.MOSAIC -> {
                        ToolParamSlider(
                            label = "Pixel Size",
                            value = pixelSizeRatio,
                            valueRange = 0.015f..0.08f,
                            displayValue = "${(pixelSizeRatio * 1000).toInt()}",
                            onValueChange = { viewModel.setPixelSizeRatio(it) },
                            isCompact = isCompactPhone
                        )
                        ToolParamSlider(
                            label = "Brush Size",
                            value = brushRadiusRatio,
                            valueRange = 0.015f..0.12f,
                            displayValue = "${(brushRadiusRatio * 1000).toInt()}px",
                            onValueChange = { viewModel.setBrushRadiusRatio(it) },
                            isCompact = isCompactPhone
                        )
                    }

                    EditTool.MOTION -> {
                        ToolParamSlider(
                            label = "Smear Strength",
                            value = motionStrength,
                            valueRange = 0.1f..1.0f,
                            displayValue = "${(motionStrength * 100).toInt()}%",
                            onValueChange = { viewModel.setMotionStrength(it) },
                            isCompact = isCompactPhone
                        )
                        ToolParamSlider(
                            label = "Angle",
                            value = motionAngle,
                            valueRange = 0f..360f,
                            displayValue = "${motionAngle.toInt()}°",
                            onValueChange = { viewModel.setMotionAngle(it) },
                            isCompact = isCompactPhone
                        )
                    }

                    EditTool.SCRAMBLE -> {
                        ToolParamSlider(
                            label = "Grain Entropy",
                            value = scrambleDensity,
                            valueRange = 0.2f..1.0f,
                            displayValue = "${(scrambleDensity * 100).toInt()}%",
                            onValueChange = { viewModel.setScrambleDensity(it) },
                            isCompact = isCompactPhone
                        )
                        ToolParamSlider(
                            label = "Brush Size",
                            value = brushRadiusRatio,
                            valueRange = 0.015f..0.12f,
                            displayValue = "${(brushRadiusRatio * 1000).toInt()}px",
                            onValueChange = { viewModel.setBrushRadiusRatio(it) },
                            isCompact = isCompactPhone
                        )
                    }

                    EditTool.GLITCH -> {
                        ToolParamSlider(
                            label = "Scanline Distortion",
                            value = glitchIntensity,
                            valueRange = 0.2f..1.0f,
                            displayValue = "${(glitchIntensity * 100).toInt()}%",
                            onValueChange = { viewModel.setGlitchIntensity(it) },
                            isCompact = isCompactPhone
                        )
                        ToolParamSlider(
                            label = "Brush Size",
                            value = brushRadiusRatio,
                            valueRange = 0.015f..0.12f,
                            displayValue = "${(brushRadiusRatio * 1000).toInt()}px",
                            onValueChange = { viewModel.setBrushRadiusRatio(it) },
                            isCompact = isCompactPhone
                        )
                    }

                    EditTool.BLACKOUT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ShapeOptionChip("Freehand", blackoutShape == BlackoutShape.FREEHAND) {
                                    viewModel.setBlackoutShape(BlackoutShape.FREEHAND)
                                }
                                ShapeOptionChip("Rect", blackoutShape == BlackoutShape.RECTANGLE) {
                                    viewModel.setBlackoutShape(BlackoutShape.RECTANGLE)
                                }
                                ShapeOptionChip("Oval", blackoutShape == BlackoutShape.OVAL) {
                                    viewModel.setBlackoutShape(BlackoutShape.OVAL)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ColorDot(0xFF000000, blackoutColor == 0xFF000000) { viewModel.setBlackoutColor(0xFF000000) }
                                ColorDot(0xFFFFFFFF, blackoutColor == 0xFFFFFFFF) { viewModel.setBlackoutColor(0xFFFFFFFF) }
                                ColorDot(0xFFDC2626, blackoutColor == 0xFFDC2626) { viewModel.setBlackoutColor(0xFFDC2626) }
                                ColorDot(0xFF2563EB, blackoutColor == 0xFF2563EB) { viewModel.setBlackoutColor(0xFF2563EB) }
                            }
                        }

                        if (blackoutShape == BlackoutShape.FREEHAND) {
                            ToolParamSlider(
                                label = "Brush Size",
                                value = brushRadiusRatio,
                                valueRange = 0.015f..0.12f,
                                displayValue = "${(brushRadiusRatio * 1000).toInt()}px",
                                onValueChange = { viewModel.setBrushRadiusRatio(it) },
                                isCompact = isCompactPhone
                            )
                        }
                    }

                    EditTool.STAMP -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PrivacyStampType.values().forEach { stamp ->
                                    StampChip(
                                        stamp = stamp,
                                        isSelected = currentStamp == stamp,
                                        onClick = { viewModel.setCurrentStamp(stamp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            ToolParamSlider(
                                label = "Stamp Tilt",
                                value = stampRotation,
                                valueRange = -45f..45f,
                                displayValue = "${stampRotation.toInt()}°",
                                onValueChange = { viewModel.setStampRotation(it) },
                                isCompact = isCompactPhone
                            )
                        }
                    }

                    EditTool.HIDE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isCompactPhone) 38.dp else 44.dp)
                                    .clickable { viewModel.autoRedactFaces() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto Redact Faces", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isCompactPhone) 38.dp else 44.dp)
                                    .clickable { viewModel.autoRedactText() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto Mask Text", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    EditTool.CROP -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CropActionBtn(Icons.Default.CropRotate, "Rotate 90°", Modifier.weight(1f)) { viewModel.rotate90() }
                            CropActionBtn(Icons.Default.Flip, "Flip H", Modifier.weight(1f)) { viewModel.flipHorizontal() }
                            CropActionBtn(Icons.Default.Flip, "Flip V", Modifier.weight(1f)) { viewModel.flipVertical() }
                        }
                    }

                    EditTool.ADJUST -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Sub-tabs for detailed grading
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Basic", "Tone", "White Balance", "Effects").forEach { tab ->
                                    SubTabChip(tab, gradingTab == tab) { gradingTab = tab }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { viewModel.resetAdjustments() }
                                        .padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            when (gradingTab) {
                                "Basic" -> {
                                    ToolParamSlider("Exposure", adjustments.exposure, -100f..100f, "${adjustments.exposure.toInt()}", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(exposure = it) }
                                    }
                                    ToolParamSlider("Brightness", adjustments.brightness, -100f..100f, "${adjustments.brightness.toInt()}", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(brightness = it) }
                                    }
                                    ToolParamSlider("Contrast", adjustments.contrast, 0.5f..2.2f, String.format(java.util.Locale.US, "%.2fx", adjustments.contrast), isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(contrast = it) }
                                    }
                                }
                                "Tone" -> {
                                    ToolParamSlider("Highlights", adjustments.highlights, -100f..100f, "${adjustments.highlights.toInt()}", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(highlights = it) }
                                    }
                                    ToolParamSlider("Shadows", adjustments.shadows, -100f..100f, "${adjustments.shadows.toInt()}", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(shadows = it) }
                                    }
                                    ToolParamSlider("Saturation", adjustments.saturation, 0f..2.5f, String.format(java.util.Locale.US, "%.2fx", adjustments.saturation), isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(saturation = it) }
                                    }
                                    ToolParamSlider("Vibrance", adjustments.vibrance, -100f..100f, "${adjustments.vibrance.toInt()}", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(vibrance = it) }
                                    }
                                }
                                "White Balance" -> {
                                    ToolParamSlider("Warmth (Temp)", adjustments.temperature, -100f..100f, "${adjustments.temperature.toInt()}", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(temperature = it) }
                                    }
                                    ToolParamSlider("Tint (G/M)", adjustments.tint, -100f..100f, "${adjustments.tint.toInt()}", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(tint = it) }
                                    }
                                }
                                "Effects" -> {
                                    ToolParamSlider("Vignette", adjustments.vignette, 0f..100f, "${adjustments.vignette.toInt()}%", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(vignette = it) }
                                    }
                                    ToolParamSlider("Sepia Film", adjustments.sepia, 0f..100f, "${adjustments.sepia.toInt()}%", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(sepia = it) }
                                    }
                                    ToolParamSlider("Hue Shift", adjustments.hueShift, -180f..180f, "${adjustments.hueShift.toInt()}°", isCompact = isCompactPhone) {
                                        viewModel.setAdjustments { a -> a.copy(hueShift = it) }
                                    }
                                }
                            }
                        }
                    }

                    EditTool.PRESETS -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ColorGradingPreset.values().forEach { preset ->
                                PresetCard(
                                    preset = preset,
                                    isSelected = activePreset == preset,
                                    onClick = { viewModel.applyPreset(preset) },
                                    isCompact = isCompactPhone
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactPhone) 6.dp else 10.dp))

                // Bottom Main Tool Selector Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompactPhone) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditTool.values().forEach { tool ->
                        ToolButton(
                            tool = tool,
                            isSelected = currentTool == tool,
                            isCompact = isCompactPhone,
                            onClick = { viewModel.setTool(tool) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolParamSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    isCompact: Boolean,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isCompact) 1.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = if (isCompact) 11.sp else 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(if (isCompact) 85.dp else 100.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = displayValue,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (isCompact) 11.sp else 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(if (isCompact) 42.dp else 50.dp)
        )
    }
}

@Composable
fun ToolButton(
    tool: EditTool,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val icon = when (tool) {
        EditTool.SELECT -> Icons.Default.HighlightAlt
        EditTool.BLUR -> Icons.Default.BlurOn
        EditTool.MOSAIC -> Icons.Default.GridOn
        EditTool.MOTION -> Icons.Default.Waves
        EditTool.BLACKOUT -> Icons.Default.VisibilityOff
        EditTool.SCRAMBLE -> Icons.Default.Grain
        EditTool.GLITCH -> Icons.Default.AutoAwesome
        EditTool.STAMP -> Icons.Default.Security
        EditTool.HIDE -> Icons.Default.Lock
        EditTool.CROP -> Icons.Default.Crop
        EditTool.ADJUST -> Icons.Default.Tune
        EditTool.PRESETS -> Icons.Default.Palette
    }

    Surface(
        shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("tool_${tool.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 10.dp else 14.dp,
                vertical = if (isCompact) 8.dp else 10.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tool.label,
                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tool.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 12.sp else 13.sp
                )
            )
        }
    }
}

@Composable
fun EffectOptionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PresetCard(
    preset: ColorGradingPreset,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .width(if (isCompact) 95.dp else 110.dp)
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = preset.subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FloatingMiniPill(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun ShapeOptionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun StampChip(stamp: PrivacyStampType, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(stamp.defaultColor) else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = stamp.text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SubTabChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ColorDot(colorHex: Long, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(colorHex))
            .border(
                2.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun CropActionBtn(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}
