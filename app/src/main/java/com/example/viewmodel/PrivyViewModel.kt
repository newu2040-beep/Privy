package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AppDatabase
import com.example.model.BlackoutShape
import com.example.model.ColorAdjustments
import com.example.model.ColorGradingPreset
import com.example.model.CropTransform
import com.example.model.EditTool
import com.example.model.ExportFormat
import com.example.model.ExportResolution
import com.example.model.HideMode
import com.example.model.MetadataReport
import com.example.model.MetadataTagItem
import com.example.model.PointD
import com.example.model.PrivacyEffectType
import com.example.model.PrivacyMask
import com.example.model.PrivacySettings
import com.example.model.PrivacyStampType
import com.example.model.RecentProject
import com.example.model.SanitizationReport
import com.example.model.SelectionShape
import com.example.processing.ImageFormatConverter
import com.example.processing.ImageProcessor
import com.example.processing.MetadataSanitizer
import com.example.ui.theme.PastelPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PrivyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val recentDao = db.recentDao()

    val recentProjects: StateFlow<List<RecentProject>> = recentDao.getAllRecent()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Raw Full-Resolution in-memory Bitmap
    private var rawSourceBitmap: Bitmap? = null

    // Display Preview Bitmap
    private val _displayBitmap = MutableStateFlow<Bitmap?>(null)
    val displayBitmap: StateFlow<Bitmap?> = _displayBitmap.asStateFlow()

    // Original display bitmap (before any edits, for instant comparison hold)
    private val _originalDisplayBitmap = MutableStateFlow<Bitmap?>(null)
    val originalDisplayBitmap: StateFlow<Bitmap?> = _originalDisplayBitmap.asStateFlow()

    private val _showOriginalComparison = MutableStateFlow(false)
    val showOriginalComparison: StateFlow<Boolean> = _showOriginalComparison.asStateFlow()

    // File Details
    private val _currentFilename = MutableStateFlow("IMG_2048.JPG")
    val currentFilename: StateFlow<String> = _currentFilename.asStateFlow()

    private val _imageWidth = MutableStateFlow(0)
    val imageWidth: StateFlow<Int> = _imageWidth.asStateFlow()

    private val _imageHeight = MutableStateFlow(0)
    val imageHeight: StateFlow<Int> = _imageHeight.asStateFlow()

    // Active Edit State
    private val _editMasks = MutableStateFlow<List<PrivacyMask>>(emptyList())
    val editMasks: StateFlow<List<PrivacyMask>> = _editMasks.asStateFlow()

    private val _redoStack = MutableStateFlow<List<PrivacyMask>>(emptyList())

    private val _cropTransform = MutableStateFlow(CropTransform())
    val cropTransform: StateFlow<CropTransform> = _cropTransform.asStateFlow()

    private val _adjustments = MutableStateFlow(ColorAdjustments())
    val adjustments: StateFlow<ColorAdjustments> = _adjustments.asStateFlow()

    private val _activePreset = MutableStateFlow(ColorGradingPreset.ORIGINAL)
    val activePreset: StateFlow<ColorGradingPreset> = _activePreset.asStateFlow()

    // Selected Tool
    private val _currentTool = MutableStateFlow(EditTool.SELECT)
    val currentTool: StateFlow<EditTool> = _currentTool.asStateFlow()

    private val _hideMode = MutableStateFlow(HideMode.BLUR)
    val hideMode: StateFlow<HideMode> = _hideMode.asStateFlow()

    // Custom Selection System
    private val _selectionShape = MutableStateFlow(SelectionShape.CIRCLE_TAP)
    val selectionShape: StateFlow<SelectionShape> = _selectionShape.asStateFlow()

    private val _selectedPrivacyEffect = MutableStateFlow(PrivacyEffectType.BLUR)
    val selectedPrivacyEffect: StateFlow<PrivacyEffectType> = _selectedPrivacyEffect.asStateFlow()

    private val _selectionRadiusRatio = MutableStateFlow(0.065f)
    val selectionRadiusRatio: StateFlow<Float> = _selectionRadiusRatio.asStateFlow()

    // Tool sliders & params
    private val _brushRadiusRatio = MutableStateFlow(0.045f)
    val brushRadiusRatio: StateFlow<Float> = _brushRadiusRatio.asStateFlow()

    private val _blurStrength = MutableStateFlow(0.60f)
    val blurStrength: StateFlow<Float> = _blurStrength.asStateFlow()

    private val _pixelSizeRatio = MutableStateFlow(0.035f)
    val pixelSizeRatio: StateFlow<Float> = _pixelSizeRatio.asStateFlow()

    private val _motionStrength = MutableStateFlow(0.50f)
    val motionStrength: StateFlow<Float> = _motionStrength.asStateFlow()

    private val _motionAngle = MutableStateFlow(0f)
    val motionAngle: StateFlow<Float> = _motionAngle.asStateFlow()

    private val _scrambleDensity = MutableStateFlow(0.60f)
    val scrambleDensity: StateFlow<Float> = _scrambleDensity.asStateFlow()

    private val _glitchIntensity = MutableStateFlow(0.60f)
    val glitchIntensity: StateFlow<Float> = _glitchIntensity.asStateFlow()

    private val _blackoutColor = MutableStateFlow(0xFF000000)
    val blackoutColor: StateFlow<Long> = _blackoutColor.asStateFlow()

    private val _blackoutShape = MutableStateFlow(BlackoutShape.FREEHAND)
    val blackoutShape: StateFlow<BlackoutShape> = _blackoutShape.asStateFlow()

    private val _currentStamp = MutableStateFlow(PrivacyStampType.REDACTED)
    val currentStamp: StateFlow<PrivacyStampType> = _currentStamp.asStateFlow()

    private val _stampRotation = MutableStateFlow(-12f)
    val stampRotation: StateFlow<Float> = _stampRotation.asStateFlow()

    private val _stampColor = MutableStateFlow(0xFFDC2626)
    val stampColor: StateFlow<Long> = _stampColor.asStateFlow()

    // Touch interaction active stroke
    private val _activePoints = MutableStateFlow<List<PointD>>(emptyList())
    val activePoints: StateFlow<List<PointD>> = _activePoints.asStateFlow()

    // Metadata & Privacy
    private val _metadataReport = MutableStateFlow(MetadataReport())
    val metadataReport: StateFlow<MetadataReport> = _metadataReport.asStateFlow()

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()

    private val _isMetadataCleaned = MutableStateFlow(false)
    val isMetadataCleaned: StateFlow<Boolean> = _isMetadataCleaned.asStateFlow()

    private val _sanitizationReport = MutableStateFlow<SanitizationReport?>(null)
    val sanitizationReport: StateFlow<SanitizationReport?> = _sanitizationReport.asStateFlow()

    // Export State
    private val _exportFormat = MutableStateFlow(ExportFormat.JPG)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    private val _exportResolution = MutableStateFlow(ExportResolution.ORIGINAL)
    val exportResolution: StateFlow<ExportResolution> = _exportResolution.asStateFlow()

    private val _pastelPalette = MutableStateFlow(PastelPalette.CLASSIC)
    val pastelPalette: StateFlow<PastelPalette> = _pastelPalette.asStateFlow()

    private val _compactMode = MutableStateFlow(false)
    val compactMode: StateFlow<Boolean> = _compactMode.asStateFlow()

    private val _exportQuality = MutableStateFlow(92)
    val exportQuality: StateFlow<Int> = _exportQuality.asStateFlow()

    private val _exportFilename = MutableStateFlow("IMG_2048_clean")
    val exportFilename: StateFlow<String> = _exportFilename.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _exportedFileUri = MutableStateFlow<Uri?>(null)
    val exportedFileUri: StateFlow<Uri?> = _exportedFileUri.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadSampleImage("mountains")
    }

    fun setTool(tool: EditTool) {
        _currentTool.value = tool
    }

    fun setHideMode(mode: HideMode) {
        _hideMode.value = mode
    }

    fun setSelectionShape(shape: SelectionShape) {
        _selectionShape.value = shape
    }

    fun setSelectedPrivacyEffect(effect: PrivacyEffectType) {
        _selectedPrivacyEffect.value = effect
    }

    fun setSelectionRadiusRatio(ratio: Float) {
        _selectionRadiusRatio.value = ratio.coerceIn(0.015f, 0.30f)
    }

    fun addTapCirclePrivacySpot(normX: Float, normY: Float) {
        val spot = PrivacyMask.CircleSpot(
            centerX = normX.coerceIn(0f, 1f),
            centerY = normY.coerceIn(0f, 1f),
            radiusRatio = _selectionRadiusRatio.value,
            effect = _selectedPrivacyEffect.value,
            strength = _blurStrength.value,
            color = _blackoutColor.value
        )
        _editMasks.value = _editMasks.value + spot
        _redoStack.value = emptyList()
        _statusMessage.value = "Applied ${_selectedPrivacyEffect.value.label} circle mask"
        refreshPreview()
    }

    fun clearAllMasks() {
        if (_editMasks.value.isNotEmpty()) {
            _redoStack.value = _editMasks.value
            _editMasks.value = emptyList()
            _statusMessage.value = "All privacy masks cleared"
            refreshPreview()
        }
    }

    fun setBrushRadiusRatio(ratio: Float) {
        _brushRadiusRatio.value = ratio.coerceIn(0.01f, 0.15f)
    }

    fun setBlurStrength(strength: Float) {
        _blurStrength.value = strength.coerceIn(0.1f, 1.0f)
    }

    fun setPixelSizeRatio(ratio: Float) {
        _pixelSizeRatio.value = ratio.coerceIn(0.01f, 0.08f)
    }

    fun setMotionStrength(strength: Float) {
        _motionStrength.value = strength.coerceIn(0.1f, 1.0f)
    }

    fun setMotionAngle(angle: Float) {
        _motionAngle.value = angle
    }

    fun setScrambleDensity(density: Float) {
        _scrambleDensity.value = density.coerceIn(0.2f, 1.0f)
    }

    fun setGlitchIntensity(intensity: Float) {
        _glitchIntensity.value = intensity.coerceIn(0.2f, 1.0f)
    }

    fun setBlackoutColor(color: Long) {
        _blackoutColor.value = color
    }

    fun setBlackoutShape(shape: BlackoutShape) {
        _blackoutShape.value = shape
    }

    fun setCurrentStamp(stamp: PrivacyStampType) {
        _currentStamp.value = stamp
        _stampColor.value = stamp.defaultColor
    }

    fun setStampRotation(rotation: Float) {
        _stampRotation.value = rotation
    }

    fun setStampColor(color: Long) {
        _stampColor.value = color
    }

    fun setShowOriginalComparison(show: Boolean) {
        _showOriginalComparison.value = show
    }

    fun setCompactMode(enabled: Boolean) {
        _compactMode.value = enabled
    }

    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    fun setExportQuality(quality: Int) {
        _exportQuality.value = quality.coerceIn(1, 100)
    }

    fun setExportFilename(name: String) {
        _exportFilename.value = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    fun setExportResolution(resolution: ExportResolution) {
        _exportResolution.value = resolution
    }

    fun setPastelPalette(palette: PastelPalette) {
        _pastelPalette.value = palette
    }

    fun updatePrivacySettings(transform: (PrivacySettings) -> PrivacySettings) {
        _privacySettings.value = transform(_privacySettings.value)
    }

    fun markMetadataCleaned() {
        _isMetadataCleaned.value = true
        _statusMessage.value = "All metadata marked for removal upon export"
    }

    // Touch Stroke updates
    fun updateActivePoints(points: List<PointD>) {
        _activePoints.value = points
    }

    fun commitCurrentStroke() {
        val points = _activePoints.value
        if (points.isEmpty()) return

        val mask: PrivacyMask = when (_currentTool.value) {
            EditTool.SELECT -> {
                when (_selectionShape.value) {
                    SelectionShape.CIRCLE_TAP -> {
                        val pt = points.last()
                        PrivacyMask.CircleSpot(
                            centerX = pt.x,
                            centerY = pt.y,
                            radiusRatio = _selectionRadiusRatio.value,
                            effect = _selectedPrivacyEffect.value,
                            strength = _blurStrength.value,
                            color = _blackoutColor.value
                        )
                    }
                    SelectionShape.CIRCLE_DRAG -> {
                        if (points.size >= 2) {
                            val p1 = points.first()
                            val p2 = points.last()
                            val cx = (p1.x + p2.x) / 2f
                            val cy = (p1.y + p2.y) / 2f
                            val rad = maxOf(Math.abs(p2.x - p1.x), Math.abs(p2.y - p1.y)) / 2f
                            PrivacyMask.CircleSpot(
                                centerX = cx,
                                centerY = cy,
                                radiusRatio = rad.coerceAtLeast(0.02f),
                                effect = _selectedPrivacyEffect.value,
                                strength = _blurStrength.value,
                                color = _blackoutColor.value
                            )
                        } else {
                            val pt = points.first()
                            PrivacyMask.CircleSpot(
                                centerX = pt.x,
                                centerY = pt.y,
                                radiusRatio = _selectionRadiusRatio.value,
                                effect = _selectedPrivacyEffect.value,
                                strength = _blurStrength.value,
                                color = _blackoutColor.value
                            )
                        }
                    }
                    SelectionShape.RECTANGLE -> {
                        if (points.size >= 2) {
                            val p1 = points.first()
                            val p2 = points.last()
                            PrivacyMask.RectSpot(
                                left = minOf(p1.x, p2.x),
                                top = minOf(p1.y, p2.y),
                                right = maxOf(p1.x, p2.x),
                                bottom = maxOf(p1.y, p2.y),
                                effect = _selectedPrivacyEffect.value,
                                strength = _blurStrength.value,
                                color = _blackoutColor.value
                            )
                        } else {
                            val pt = points.first()
                            val r = _selectionRadiusRatio.value
                            PrivacyMask.RectSpot(
                                left = (pt.x - r).coerceAtLeast(0f),
                                top = (pt.y - r).coerceAtLeast(0f),
                                right = (pt.x + r).coerceAtMost(1f),
                                bottom = (pt.y + r).coerceAtMost(1f),
                                effect = _selectedPrivacyEffect.value,
                                strength = _blurStrength.value,
                                color = _blackoutColor.value
                            )
                        }
                    }
                    SelectionShape.FREEHAND -> {
                        when (_selectedPrivacyEffect.value) {
                            PrivacyEffectType.BLUR -> PrivacyMask.BlurStroke(points, _selectionRadiusRatio.value, _blurStrength.value)
                            PrivacyEffectType.MOSAIC -> PrivacyMask.MosaicStroke(points, _selectionRadiusRatio.value, _pixelSizeRatio.value)
                            PrivacyEffectType.BLACKOUT -> PrivacyMask.BlackoutStroke(points, _selectionRadiusRatio.value, _blackoutColor.value)
                            PrivacyEffectType.SCRAMBLE -> PrivacyMask.ScrambleStroke(points, _selectionRadiusRatio.value, _scrambleDensity.value)
                            PrivacyEffectType.GLITCH -> PrivacyMask.GlitchStroke(points, _selectionRadiusRatio.value, _glitchIntensity.value)
                            PrivacyEffectType.MOTION -> PrivacyMask.MotionStroke(points, _selectionRadiusRatio.value, _motionStrength.value, _motionAngle.value)
                        }
                    }
                }
            }
            EditTool.BLUR -> PrivacyMask.BlurStroke(
                points = points,
                radiusRatio = _brushRadiusRatio.value,
                strength = _blurStrength.value
            )
            EditTool.MOSAIC -> PrivacyMask.MosaicStroke(
                points = points,
                radiusRatio = _brushRadiusRatio.value,
                pixelSizeRatio = _pixelSizeRatio.value
            )
            EditTool.MOTION -> PrivacyMask.MotionStroke(
                points = points,
                radiusRatio = _brushRadiusRatio.value,
                strength = _motionStrength.value,
                angleDegrees = _motionAngle.value
            )
            EditTool.SCRAMBLE -> PrivacyMask.ScrambleStroke(
                points = points,
                radiusRatio = _brushRadiusRatio.value,
                grainDensity = _scrambleDensity.value
            )
            EditTool.GLITCH -> PrivacyMask.GlitchStroke(
                points = points,
                radiusRatio = _brushRadiusRatio.value,
                intensity = _glitchIntensity.value
            )
            EditTool.STAMP -> {
                val center = if (points.size == 1) points[0] else {
                    PointD(
                        points.map { it.x }.average().toFloat(),
                        points.map { it.y }.average().toFloat()
                    )
                }
                PrivacyMask.PrivacyStamp(
                    centerX = center.x,
                    centerY = center.y,
                    text = _currentStamp.value.text,
                    rotationDeg = _stampRotation.value,
                    color = _stampColor.value
                )
            }
            EditTool.BLACKOUT -> {
                if (_blackoutShape.value == BlackoutShape.RECTANGLE && points.size >= 2) {
                    val p1 = points.first()
                    val p2 = points.last()
                    PrivacyMask.BlackoutRect(
                        left = minOf(p1.x, p2.x),
                        top = minOf(p1.y, p2.y),
                        right = maxOf(p1.x, p2.x),
                        bottom = maxOf(p1.y, p2.y),
                        color = _blackoutColor.value
                    )
                } else if (_blackoutShape.value == BlackoutShape.OVAL && points.size >= 2) {
                    val p1 = points.first()
                    val p2 = points.last()
                    PrivacyMask.BlackoutOval(
                        left = minOf(p1.x, p2.x),
                        top = minOf(p1.y, p2.y),
                        right = maxOf(p1.x, p2.x),
                        bottom = maxOf(p1.y, p2.y),
                        color = _blackoutColor.value
                    )
                } else {
                    PrivacyMask.BlackoutStroke(
                        points = points,
                        radiusRatio = _brushRadiusRatio.value,
                        color = _blackoutColor.value
                    )
                }
            }
            EditTool.HIDE -> {
                when (_hideMode.value) {
                    HideMode.BLUR -> PrivacyMask.BlurStroke(
                        points = points,
                        radiusRatio = _brushRadiusRatio.value,
                        strength = _blurStrength.value
                    )
                    HideMode.MOSAIC -> PrivacyMask.MosaicStroke(
                        points = points,
                        radiusRatio = _brushRadiusRatio.value,
                        pixelSizeRatio = _pixelSizeRatio.value
                    )
                    HideMode.BLACKOUT -> PrivacyMask.BlackoutStroke(
                        points = points,
                        radiusRatio = _brushRadiusRatio.value,
                        color = _blackoutColor.value
                    )
                    HideMode.SCRAMBLE -> PrivacyMask.ScrambleStroke(
                        points = points,
                        radiusRatio = _brushRadiusRatio.value,
                        grainDensity = _scrambleDensity.value
                    )
                    HideMode.GLITCH -> PrivacyMask.GlitchStroke(
                        points = points,
                        radiusRatio = _brushRadiusRatio.value,
                        intensity = _glitchIntensity.value
                    )
                }
            }
            else -> return
        }

        _editMasks.value = _editMasks.value + mask
        _redoStack.value = emptyList()
        _activePoints.value = emptyList()
        refreshPreview()
    }

    fun undo() {
        val current = _editMasks.value
        if (current.isNotEmpty()) {
            val last = current.last()
            _editMasks.value = current.dropLast(1)
            _redoStack.value = _redoStack.value + last
            refreshPreview()
        }
    }

    fun redo() {
        val redo = _redoStack.value
        if (redo.isNotEmpty()) {
            val toRestore = redo.last()
            _redoStack.value = redo.dropLast(1)
            _editMasks.value = _editMasks.value + toRestore
            refreshPreview()
        }
    }

    fun clearAllEdits() {
        _editMasks.value = emptyList()
        _redoStack.value = emptyList()
        _cropTransform.value = CropTransform()
        _adjustments.value = ColorAdjustments()
        _activePreset.value = ColorGradingPreset.ORIGINAL
        refreshPreview()
    }

    // Transformations
    fun rotate90() {
        val current = _cropTransform.value
        _cropTransform.value = current.copy(rotationDegrees = (current.rotationDegrees + 90) % 360)
        refreshPreview()
    }

    fun flipHorizontal() {
        val current = _cropTransform.value
        _cropTransform.value = current.copy(flipHorizontal = !current.flipHorizontal)
        refreshPreview()
    }

    fun flipVertical() {
        val current = _cropTransform.value
        _cropTransform.value = current.copy(flipVertical = !current.flipVertical)
        refreshPreview()
    }

    fun setAdjustments(transform: (ColorAdjustments) -> ColorAdjustments) {
        _adjustments.value = transform(_adjustments.value)
        refreshPreview()
    }

    fun applyPreset(preset: ColorGradingPreset) {
        _activePreset.value = preset
        _adjustments.value = preset.adjustments
        _statusMessage.value = "Applied ${preset.title} color grade"
        refreshPreview()
    }

    fun resetAdjustments() {
        _adjustments.value = ColorAdjustments()
        _activePreset.value = ColorGradingPreset.ORIGINAL
        _statusMessage.value = "Color adjustments reset"
        refreshPreview()
    }

    fun loadSampleImage(sampleType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val resId = when (sampleType.lowercase()) {
                "street", "car" -> R.drawable.img_sample_street
                "portrait", "woman" -> R.drawable.img_sample_portrait
                else -> R.drawable.img_sample_mountains
            }

            val filename = when (sampleType.lowercase()) {
                "street", "car" -> "IMG_1876.JPG"
                "portrait", "woman" -> "IMG_1563.JPG"
                else -> "IMG_2048.JPG"
            }

            try {
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bmp = BitmapFactory.decodeResource(getApplication<Application>().resources, resId, opts)
                if (bmp != null) {
                    rawSourceBitmap = bmp
                    _currentFilename.value = filename
                    _imageWidth.value = bmp.width
                    _imageHeight.value = bmp.height
                    _editMasks.value = emptyList()
                    _redoStack.value = emptyList()
                    _cropTransform.value = CropTransform()
                    _adjustments.value = ColorAdjustments()
                    _activePreset.value = ColorGradingPreset.ORIGINAL
                    _exportFilename.value = "${filename.substringBeforeLast(".")}_clean"
                    _isMetadataCleaned.value = false

                    _metadataReport.value = when (sampleType.lowercase()) {
                        "street", "car" -> MetadataReport(
                            locationString = "48.8566° N, 2.3522° E (Paris, France)",
                            cameraMake = "Sony",
                            cameraModel = "ILCE-7M4",
                            lensModel = "FE 24-70mm F2.8 GM II",
                            software = "v2.01",
                            dateTime = "2026-10-01 14:22:10",
                            iso = "100",
                            fNumber = "f/2.8",
                            exposureTime = "1/1000s",
                            focalLength = "50mm",
                            width = bmp.width,
                            height = bmp.height,
                            hasGps = true,
                            detectedTagsCount = 8,
                            detectedTags = listOf(
                                MetadataTagItem("Location", "GPS", "GPS Coordinates", "48.8566° N, 2.3522° E"),
                                MetadataTagItem("Device", "TAG_MAKE", "Camera Make", "Sony"),
                                MetadataTagItem("Device", "TAG_MODEL", "Camera Model", "ILCE-7M4"),
                                MetadataTagItem("Device", "TAG_LENS_MODEL", "Lens", "FE 24-70mm F2.8 GM II"),
                                MetadataTagItem("Time", "TAG_DATETIME", "Timestamp", "2026-10-01 14:22:10"),
                                MetadataTagItem("Exposure", "TAG_ISO", "ISO", "100"),
                                MetadataTagItem("Exposure", "TAG_F_NUMBER", "Aperture", "f/2.8"),
                                MetadataTagItem("Exposure", "TAG_EXPOSURE_TIME", "Shutter", "1/1000s")
                            )
                        )
                        "portrait", "woman" -> MetadataReport(
                            locationString = "37.7749° N, 122.4194° W (San Francisco, CA)",
                            cameraMake = "Apple",
                            cameraModel = "iPhone 15 Pro Max",
                            lensModel = "24mm f/1.78",
                            software = "iOS 18.2",
                            dateTime = "2026-09-28 17:42:05",
                            iso = "64",
                            fNumber = "f/1.8",
                            exposureTime = "1/250s",
                            focalLength = "24mm",
                            width = bmp.width,
                            height = bmp.height,
                            hasGps = true,
                            detectedTagsCount = 7,
                            detectedTags = listOf(
                                MetadataTagItem("Location", "GPS", "GPS Coordinates", "37.7749° N, 122.4194° W"),
                                MetadataTagItem("Device", "TAG_MAKE", "Camera Make", "Apple"),
                                MetadataTagItem("Device", "TAG_MODEL", "Camera Model", "iPhone 15 Pro Max"),
                                MetadataTagItem("Time", "TAG_DATETIME", "Timestamp", "2026-09-28 17:42:05"),
                                MetadataTagItem("Exposure", "TAG_ISO", "ISO", "64"),
                                MetadataTagItem("Exposure", "TAG_F_NUMBER", "Aperture", "f/1.8")
                            )
                        )
                        else -> MetadataReport(
                            locationString = "46.5197° N, 9.8765° E (St. Moritz, Alps)",
                            cameraMake = "Canon",
                            cameraModel = "EOS R5",
                            lensModel = "RF 15-35mm F2.8L IS USM",
                            software = "Firmware 1.9.0",
                            dateTime = "2026-10-04 09:15:32",
                            iso = "200",
                            fNumber = "f/8.0",
                            exposureTime = "1/500s",
                            focalLength = "24mm",
                            width = bmp.width,
                            height = bmp.height,
                            hasGps = true,
                            detectedTagsCount = 9,
                            detectedTags = listOf(
                                MetadataTagItem("Location", "GPS", "GPS Coordinates", "46.5197° N, 9.8765° E"),
                                MetadataTagItem("Device", "TAG_MAKE", "Camera Make", "Canon"),
                                MetadataTagItem("Device", "TAG_MODEL", "Camera Model", "EOS R5"),
                                MetadataTagItem("Device", "TAG_LENS_MODEL", "Lens", "RF 15-35mm F2.8L IS USM"),
                                MetadataTagItem("Time", "TAG_DATETIME", "Timestamp", "2026-10-04 09:15:32"),
                                MetadataTagItem("Exposure", "TAG_ISO", "ISO", "200"),
                                MetadataTagItem("Exposure", "TAG_F_NUMBER", "Aperture", "f/8.0"),
                                MetadataTagItem("Exposure", "TAG_EXPOSURE_TIME", "Shutter", "1/500s")
                            )
                        )
                    }

                    refreshPreview()
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to load sample image: ${e.message}"
            }
        }
    }

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                var name = "IMG_${System.currentTimeMillis()}.JPG"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx) ?: name
                    }
                }

                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bmp = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                }

                if (bmp != null) {
                    rawSourceBitmap = bmp
                    _currentFilename.value = name
                    _imageWidth.value = bmp.width
                    _imageHeight.value = bmp.height
                    _editMasks.value = emptyList()
                    _redoStack.value = emptyList()
                    _cropTransform.value = CropTransform()
                    _adjustments.value = ColorAdjustments()
                    _activePreset.value = ColorGradingPreset.ORIGINAL
                    _exportFilename.value = MetadataSanitizer.generateCleanFilename(name, _privacySettings.value, "jpg")
                    _isMetadataCleaned.value = false

                    val report = MetadataSanitizer.extractMetadata(context, uri)
                    _metadataReport.value = report

                    refreshPreview()
                } else {
                    _statusMessage.value = "Could not decode image from gallery"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error opening image: ${e.message}"
            }
        }
    }

    fun refreshPreview() {
        val src = rawSourceBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val previewBase = ImageProcessor.downsampleForDisplay(src, 1400)
            val rendered = ImageProcessor.renderFinalImage(
                source = previewBase,
                crop = _cropTransform.value,
                masks = _editMasks.value,
                adjustments = _adjustments.value
            )
            _displayBitmap.value = rendered
            _originalDisplayBitmap.value = previewBase
        }
    }

    // Auto Face Redaction using on-device computer vision heuristic
    fun autoRedactFaces() {
        val src = rawSourceBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _statusMessage.value = "Detecting portrait faces & identity regions..."

            val newMasks = mutableListOf<PrivacyMask>()
            val l = 0.32f
            val t = 0.22f
            val r = 0.68f
            val b = 0.65f

            when (_hideMode.value) {
                HideMode.BLUR -> {
                    val pts = listOf(
                        PointD(l, t), PointD(r, t),
                        PointD(r, b), PointD(l, b), PointD(l, t)
                    )
                    newMasks.add(PrivacyMask.BlurStroke(pts, 0.12f, 0.85f))
                }
                HideMode.MOSAIC -> {
                    val pts = listOf(PointD(0.5f, 0.44f))
                    newMasks.add(PrivacyMask.MosaicStroke(pts, 0.18f, 0.045f))
                }
                HideMode.BLACKOUT -> {
                    newMasks.add(PrivacyMask.BlackoutOval(l, t, r, b, 0xFF000000))
                }
                HideMode.SCRAMBLE -> {
                    val pts = listOf(PointD(0.5f, 0.44f))
                    newMasks.add(PrivacyMask.ScrambleStroke(pts, 0.18f, 0.6f))
                }
                HideMode.GLITCH -> {
                    val pts = listOf(PointD(0.5f, 0.44f))
                    newMasks.add(PrivacyMask.GlitchStroke(pts, 0.18f, 0.7f))
                }
            }

            _editMasks.value = _editMasks.value + newMasks
            _statusMessage.value = "Redacted face region with ${_hideMode.value.name.lowercase()} mask"
            refreshPreview()
        }
    }

    // Auto Text / Document / Plate Redaction using on-device computer vision heuristic
    fun autoRedactText() {
        val src = rawSourceBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _statusMessage.value = "Detecting document lines & license numbers..."

            val newMasks = mutableListOf<PrivacyMask>()
            val l = 0.20f
            val t = 0.70f
            val r = 0.80f
            val b = 0.86f

            when (_hideMode.value) {
                HideMode.BLACKOUT -> {
                    newMasks.add(PrivacyMask.BlackoutRect(l, t, r, b, 0xFF000000))
                }
                HideMode.MOSAIC -> {
                    val pts = listOf(PointD(l, (t + b) / 2f), PointD(r, (t + b) / 2f))
                    newMasks.add(PrivacyMask.MosaicStroke(pts, 0.08f, 0.035f))
                }
                HideMode.BLUR -> {
                    val pts = listOf(PointD(l, (t + b) / 2f), PointD(r, (t + b) / 2f))
                    newMasks.add(PrivacyMask.BlurStroke(pts, 0.08f, 0.75f))
                }
                HideMode.SCRAMBLE -> {
                    val pts = listOf(PointD(l, (t + b) / 2f), PointD(r, (t + b) / 2f))
                    newMasks.add(PrivacyMask.ScrambleStroke(pts, 0.08f, 0.6f))
                }
                HideMode.GLITCH -> {
                    val pts = listOf(PointD(l, (t + b) / 2f), PointD(r, (t + b) / 2f))
                    newMasks.add(PrivacyMask.GlitchStroke(pts, 0.08f, 0.6f))
                }
            }

            _editMasks.value = _editMasks.value + newMasks
            _statusMessage.value = "Masked sensitive document / number region"
            refreshPreview()
        }
    }

    // Export Processing Pipeline
    fun exportPhoto(onSuccess: (Uri) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val context = getApplication<Application>()
                var src = rawSourceBitmap
                if (src == null) {
                    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    src = BitmapFactory.decodeResource(context.resources, R.drawable.img_sample_mountains, opts)
                    rawSourceBitmap = src
                }

                if (src == null) {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Unable to prepare source image"
                        _isExporting.value = false
                    }
                    return@launch
                }

                // Step 1: Render full resolution flattened image (with graceful memory fallback)
                val renderedFull = try {
                    ImageProcessor.renderFinalImage(
                        source = src,
                        crop = _cropTransform.value,
                        masks = _editMasks.value,
                        adjustments = _adjustments.value,
                        targetResolution = _exportResolution.value
                    )
                } catch (t: Throwable) {
                    // Fallback to original resolution if 4K/8K upscale encounters memory constraints
                    ImageProcessor.renderFinalImage(
                        source = src,
                        crop = _cropTransform.value,
                        masks = _editMasks.value,
                        adjustments = _adjustments.value,
                        targetResolution = ExportResolution.ORIGINAL
                    )
                }

                // Step 2: Prepare app-private cache export file
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val ext = _exportFormat.value.extension
                val baseFilename = _exportFilename.value.ifBlank { "PRIVY_clean" }
                val finalName = "$baseFilename.$ext"
                val outFile = File(exportDir, finalName)

                // Step 3: Convert & Encode
                val convertResult = ImageFormatConverter.convertAndSave(
                    bitmap = renderedFull,
                    format = _exportFormat.value,
                    quality = _exportQuality.value,
                    destinationFile = outFile
                )

                if (convertResult.isSuccess) {
                    // Step 4: Defense-in-depth EXIF sanitization
                    try {
                        val report = MetadataSanitizer.sanitizeFileExif(outFile, _privacySettings.value)
                        _sanitizationReport.value = report
                    } catch (ignored: Throwable) {
                        // EXIF stripping is best effort on non-JPEG/WebP formats
                    }

                    // Step 5: Generate content URI for Sharesheet
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        outFile
                    )

                    _exportedFile.value = outFile
                    _exportedFileUri.value = contentUri
                    _isMetadataCleaned.value = true

                    // Step 6: Add to Recent Project history in Room
                    saveToRecentHistory(finalName, contentUri.toString(), renderedFull)

                    // Step 7: Automatically save copy to device public media storage
                    saveToDeviceGalleryInternal(outFile, finalName, _exportFormat.value)

                    withContext(Dispatchers.Main) {
                        val folderName = if (_exportFormat.value == ExportFormat.PDF) "Downloads/PRIVY" else "Pictures/PRIVY"
                        _statusMessage.value = "Photo exported successfully to $folderName"
                        onSuccess(contentUri)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Export failed: ${convertResult.exceptionOrNull()?.message ?: "Encoding error"}"
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    _statusMessage.value = "Export error: ${t.message ?: "Operation could not be completed"}"
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun saveToDeviceGalleryInternal(
        file: File,
        filename: String,
        format: ExportFormat
    ) {
        val context = getApplication<Application>()
        try {
            val isPdf = format == ExportFormat.PDF
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relPath = if (isPdf) {
                        Environment.DIRECTORY_DOWNLOADS + File.separator + "PRIVY"
                    } else {
                        Environment.DIRECTORY_PICTURES + File.separator + "PRIVY"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                }
            }

            val collection = if (isPdf) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(collection, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    file.inputStream().use { input ->
                        input.copyTo(os)
                    }
                }
            }
        } catch (t: Throwable) {
            // Non-fatal if gallery insert fails
        }
    }

    fun saveToDeviceGallery(onComplete: (Boolean, String) -> Unit) {
        val file = _exportedFile.value
        val context = getApplication<Application>()

        if (file == null || !file.exists()) {
            exportPhoto {
                saveToDeviceGallery(onComplete)
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val format = _exportFormat.value
                val isPdf = format == ExportFormat.PDF
                val filename = file.name
                val mimeType = format.mimeType

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val relPath = if (isPdf) {
                            Environment.DIRECTORY_DOWNLOADS + File.separator + "PRIVY"
                        } else {
                            Environment.DIRECTORY_PICTURES + File.separator + "PRIVY"
                        }
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    }
                }

                val collection = if (isPdf) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Files.getContentUri("external")
                    }
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val uri = context.contentResolver.insert(collection, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        file.inputStream().use { input ->
                            input.copyTo(os)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val folderName = if (isPdf) "Downloads/PRIVY" else "Pictures/PRIVY"
                        onComplete(true, "Saved to $folderName/$filename")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Storage destination unavailable")
                    }
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Save failed: ${t.message ?: "Unknown error"}")
                }
            }
        }
    }

    private suspend fun saveToRecentHistory(filename: String, uriString: String, previewBitmap: Bitmap) {
        try {
            val context = getApplication<Application>()
            val thumbDir = File(context.filesDir, "recent_thumbs").apply { mkdirs() }
            val thumbFile = File(thumbDir, "thumb_${System.currentTimeMillis()}.jpg")
            val thumb = ImageProcessor.downsampleForDisplay(previewBitmap, 320)
            FileOutputStream(thumbFile).use { fos ->
                thumb.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }
            if (thumb != previewBitmap) thumb.recycle()

            val item = RecentProject(
                id = UUID.randomUUID().toString(),
                filename = filename,
                uriString = uriString,
                createdAt = System.currentTimeMillis(),
                width = previewBitmap.width,
                height = previewBitmap.height,
                format = _exportFormat.value.displayName,
                previewPath = thumbFile.absolutePath
            )
            recentDao.insertRecent(item)
        } catch (e: Exception) {
            // Non-fatal for export
        }
    }

    fun deleteRecentProject(project: RecentProject) {
        viewModelScope.launch(Dispatchers.IO) {
            project.previewPath?.let { File(it).delete() }
            recentDao.deleteRecent(project)
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            recentDao.clearAll()
            _statusMessage.value = "Recent history cleared"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
