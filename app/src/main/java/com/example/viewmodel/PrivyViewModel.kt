package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
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
import com.example.model.CropTransform
import com.example.model.EditTool
import com.example.model.ExportFormat
import com.example.model.HideMode
import com.example.model.MetadataReport
import com.example.model.PointD
import com.example.model.PrivacyMask
import com.example.model.PrivacySettings
import com.example.model.RecentProject
import com.example.processing.ImageFormatConverter
import com.example.processing.ImageProcessor
import com.example.processing.MetadataSanitizer
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
import java.io.InputStream
import java.util.UUID

class PrivyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val recentDao = db.recentDao()

    val recentProjects: StateFlow<List<RecentProject>> = recentDao.getAllRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Core Image State
    private var rawSourceBitmap: Bitmap? = null
    private val _displayBitmap = MutableStateFlow<Bitmap?>(null)
    val displayBitmap: StateFlow<Bitmap?> = _displayBitmap.asStateFlow()

    private val _currentFilename = MutableStateFlow("IMG_2048.JPG")
    val currentFilename: StateFlow<String> = _currentFilename.asStateFlow()

    private val _imageWidth = MutableStateFlow(4032)
    val imageWidth: StateFlow<Int> = _imageWidth.asStateFlow()

    private val _imageHeight = MutableStateFlow(3024)
    val imageHeight: StateFlow<Int> = _imageHeight.asStateFlow()

    // Non-destructive Edit State
    private val _editMasks = MutableStateFlow<List<PrivacyMask>>(emptyList())
    val editMasks: StateFlow<List<PrivacyMask>> = _editMasks.asStateFlow()

    private val _redoStack = MutableStateFlow<List<PrivacyMask>>(emptyList())
    val redoStack: StateFlow<List<PrivacyMask>> = _redoStack.asStateFlow()

    private val _cropTransform = MutableStateFlow(CropTransform())
    val cropTransform: StateFlow<CropTransform> = _cropTransform.asStateFlow()

    private val _adjustments = MutableStateFlow(ColorAdjustments())
    val adjustments: StateFlow<ColorAdjustments> = _adjustments.asStateFlow()

    // Active tool state
    private val _currentTool = MutableStateFlow(EditTool.BLUR)
    val currentTool: StateFlow<EditTool> = _currentTool.asStateFlow()

    private val _hideMode = MutableStateFlow(HideMode.BLUR)
    val hideMode: StateFlow<HideMode> = _hideMode.asStateFlow()

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

    private val _blackoutColor = MutableStateFlow(0xFF000000)
    val blackoutColor: StateFlow<Long> = _blackoutColor.asStateFlow()

    private val _blackoutShape = MutableStateFlow(BlackoutShape.FREEHAND)
    val blackoutShape: StateFlow<BlackoutShape> = _blackoutShape.asStateFlow()

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

    private val _sanitizationReport = MutableStateFlow<com.example.model.SanitizationReport?>(null)
    val sanitizationReport: StateFlow<com.example.model.SanitizationReport?> = _sanitizationReport.asStateFlow()

    // Export State
    private val _exportFormat = MutableStateFlow(ExportFormat.JPG)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    private val _exportResolution = MutableStateFlow(com.example.model.ExportResolution.ORIGINAL)
    val exportResolution: StateFlow<com.example.model.ExportResolution> = _exportResolution.asStateFlow()

    private val _pastelPalette = MutableStateFlow(com.example.ui.theme.PastelPalette.CLASSIC)
    val pastelPalette: StateFlow<com.example.ui.theme.PastelPalette> = _pastelPalette.asStateFlow()

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
        // Automatically preload the signature mountain sample image on startup so user sees a rich photo right away
        loadSampleImage("mountains")
    }

    fun setTool(tool: EditTool) {
        _currentTool.value = tool
    }

    fun setHideMode(mode: HideMode) {
        _hideMode.value = mode
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

    fun setBlackoutColor(color: Long) {
        _blackoutColor.value = color
    }

    fun setBlackoutShape(shape: BlackoutShape) {
        _blackoutShape.value = shape
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

    fun setExportResolution(resolution: com.example.model.ExportResolution) {
        _exportResolution.value = resolution
    }

    fun setPastelPalette(palette: com.example.ui.theme.PastelPalette) {
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

    fun setAdjustments(brightness: Float, contrast: Float, saturation: Float) {
        _adjustments.value = ColorAdjustments(brightness, contrast, saturation)
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
                    _exportFilename.value = "${filename.substringBeforeLast(".")}_clean"
                    _isMetadataCleaned.value = false

                    // Generate rich simulated metadata for sample photos so user can test the privacy inspector
                    _metadataReport.value = when (sampleType.lowercase()) {
                        "street", "car" -> MetadataReport(
                            locationString = "48.8566° N, 2.3522° E (Paris, France)",
                            cameraMake = "Sony",
                            cameraModel = "ILCE-7M4 (A7 IV)",
                            lensModel = "FE 24-70mm F2.8 GM II",
                            software = "v2.01",
                            dateTime = "2026-10-02 14:28:10",
                            iso = "100",
                            fNumber = "f/2.8",
                            exposureTime = "1/1000s",
                            focalLength = "50mm",
                            width = bmp.width,
                            height = bmp.height,
                            hasGps = true,
                            detectedTagsCount = 8
                        )
                        "portrait", "woman" -> MetadataReport(
                            locationString = "40.7128° N, 74.0060° W (New York, NY)",
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
                            detectedTagsCount = 7
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
                            detectedTagsCount = 9
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
                // Determine file name
                var name = "IMG_${System.currentTimeMillis()}.JPG"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIdx) ?: name
                    }
                }

                // Decode bounds first
                var stream = context.contentResolver.openInputStream(uri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, bounds)
                stream?.close()

                // Calculate downsample to prevent OOM on massive photos
                var inSampleSize = 1
                val maxDim = 3840
                if (bounds.outHeight > maxDim || bounds.outWidth > maxDim) {
                    val halfHeight = bounds.outHeight / 2
                    val halfWidth = bounds.outWidth / 2
                    while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                        inSampleSize *= 2
                    }
                }

                stream = context.contentResolver.openInputStream(uri)
                val decodeOpts = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = BitmapFactory.decodeStream(stream, null, decodeOpts)
                stream?.close()

                if (bmp != null) {
                    rawSourceBitmap = bmp
                    _currentFilename.value = name
                    _imageWidth.value = bounds.outWidth.takeIf { it > 0 } ?: bmp.width
                    _imageHeight.value = bounds.outHeight.takeIf { it > 0 } ?: bmp.height
                    _editMasks.value = emptyList()
                    _redoStack.value = emptyList()
                    _cropTransform.value = CropTransform()
                    _adjustments.value = ColorAdjustments()
                    _exportFilename.value = "${name.substringBeforeLast(".")}_clean"
                    _isMetadataCleaned.value = false

                    // Extract real EXIF metadata from imported photo!
                    _metadataReport.value = MetadataSanitizer.extractMetadata(context, uri)

                    refreshPreview()
                    _statusMessage.value = "Imported $name successfully"
                } else {
                    _statusMessage.value = "This image format isn't supported."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Couldn't load image: ${e.message}"
            }
        }
    }

    private fun refreshPreview() {
        val src = rawSourceBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val downsampled = ImageProcessor.downsampleForDisplay(src, 1400)
            val rendered = ImageProcessor.renderFinalImage(
                source = downsampled,
                crop = _cropTransform.value,
                masks = _editMasks.value,
                adjustments = _adjustments.value
            )
            _displayBitmap.value = rendered
        }
    }

    fun exportPhoto(onSuccess: (Uri) -> Unit) {
        val src = rawSourceBitmap
        if (src == null) {
            _statusMessage.value = "No photo loaded to export"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                // Step 1: Render full resolution flattened image (up to 4K / 8K if requested)
                val renderedFull = ImageProcessor.renderFinalImage(
                    source = src,
                    crop = _cropTransform.value,
                    masks = _editMasks.value,
                    adjustments = _adjustments.value,
                    targetResolution = _exportResolution.value
                )

                // Step 2: Prepare app-private cache export file
                val context = getApplication<Application>()
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
                    val report = MetadataSanitizer.sanitizeFileExif(outFile, _privacySettings.value)
                    _sanitizationReport.value = report

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

                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Exported $finalName cleanly!"
                        onSuccess(contentUri)
                    }
                } else {
                    _statusMessage.value = "Export failed: ${convertResult.exceptionOrNull()?.message}"
                }

                if (renderedFull != src) {
                    renderedFull.recycle()
                }
            } catch (e: Exception) {
                _statusMessage.value = "Couldn't export this photo: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun saveToDeviceGallery(onComplete: (Boolean, String) -> Unit) {
        val file = _exportedFile.value
        val bitmap = _displayBitmap.value
        val context = getApplication<Application>()

        if (file == null || !file.exists()) {
            // If user hasn't explicitly tapped export yet, trigger export first
            exportPhoto {
                saveToDeviceGallery(onComplete)
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val format = _exportFormat.value
                val filename = file.name
                val mimeType = format.mimeType

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "PRIVY")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, contentValues, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        onComplete(true, "Saved to Pictures/PRIVY")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Storage unavailable")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Save failed: ${e.message}")
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
