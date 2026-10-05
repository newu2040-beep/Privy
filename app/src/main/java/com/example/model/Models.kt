package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EditTool(val label: String) {
    BLUR("Blur"),
    MOSAIC("Mosaic"),
    MOTION("Motion"),
    BLACKOUT("Blackout"),
    HIDE("Hide"),
    CROP("Crop"),
    ADJUST("Adjust")
}

enum class HideMode {
    BLUR,
    MOSAIC,
    BLACKOUT
}

enum class BlackoutShape {
    FREEHAND,
    RECTANGLE
}

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    JPG("jpg", "image/jpeg", "JPG"),
    PNG("png", "image/png", "PNG"),
    WEBP("webp", "image/webp", "WEBP"),
    HEIC("heic", "image/heif", "HEIC"),
    TIFF("tiff", "image/tiff", "TIFF"),
    BMP("bmp", "image/bmp", "BMP"),
    PDF("pdf", "application/pdf", "PDF")
}

enum class ExportResolution(val label: String, val maxDimension: Int, val description: String) {
    ORIGINAL("Original", 0, "Native resolution"),
    FHD("1080p FHD", 1920, "1920 × 1080"),
    UHD_4K("4K UHD", 3840, "3840 × 2160"),
    UHD_8K("8K Master", 7680, "7680 × 4320")
}

data class PointD(val x: Float, val y: Float)

sealed class PrivacyMask {
    data class BlurStroke(
        val points: List<PointD>,
        val radiusRatio: Float,
        val strength: Float
    ) : PrivacyMask()

    data class MosaicStroke(
        val points: List<PointD>,
        val radiusRatio: Float,
        val pixelSizeRatio: Float
    ) : PrivacyMask()

    data class MotionStroke(
        val points: List<PointD>,
        val radiusRatio: Float,
        val strength: Float,
        val angleDegrees: Float
    ) : PrivacyMask()

    data class BlackoutStroke(
        val points: List<PointD>,
        val radiusRatio: Float,
        val color: Long = 0xFF000000
    ) : PrivacyMask()

    data class BlackoutRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val color: Long = 0xFF000000
    ) : PrivacyMask()
}

data class CropTransform(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
)

data class ColorAdjustments(
    val brightness: Float = 0f,    // -100 to 100
    val contrast: Float = 1f,       // 0.5 to 2.0
    val saturation: Float = 1f      // 0 to 2.0
)

data class MetadataTagItem(
    val category: String,
    val tagKey: String,
    val displayName: String,
    val displayValue: String
)

data class SanitizationReport(
    val isClean: Boolean = true,
    val locationRemoved: Boolean = true,
    val cameraInfoRemoved: Boolean = true,
    val dateTimeRemoved: Boolean = true,
    val exifStripped: Boolean = true,
    val remainingTagsCount: Int = 0
)

data class MetadataReport(
    val locationString: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val software: String? = null,
    val dateTime: String? = null,
    val iso: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val hasGps: Boolean = false,
    val detectedTagsCount: Int = 0,
    val detectedTags: List<MetadataTagItem> = emptyList()
)

data class PrivacySettings(
    val removeLocation: Boolean = true,
    val stripAllMetadata: Boolean = true,
    val removeCameraInfo: Boolean = true,
    val removeDateTime: Boolean = true,
    val secureFilename: Boolean = true
)

@Entity(tableName = "recent_projects")
data class RecentProject(
    @PrimaryKey val id: String,
    val filename: String,
    val uriString: String,
    val createdAt: Long,
    val width: Int,
    val height: Int,
    val format: String,
    val previewPath: String? = null
)
