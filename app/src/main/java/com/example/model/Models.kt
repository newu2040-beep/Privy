package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EditTool(val label: String) {
    SELECT("Select"),
    BLUR("Blur"),
    MOSAIC("Mosaic"),
    MOTION("Motion"),
    BLACKOUT("Blackout"),
    SCRAMBLE("Scramble"),
    GLITCH("Glitch"),
    STAMP("Stamp"),
    HIDE("AI Hide"),
    CROP("Crop"),
    ADJUST("Grading"),
    PRESETS("Presets")
}

enum class SelectionShape(val label: String) {
    CIRCLE_TAP("Tap Circle"),
    CIRCLE_DRAG("Circle Area"),
    RECTANGLE("Rect Area"),
    FREEHAND("Freehand")
}

enum class PrivacyEffectType(val label: String) {
    BLUR("Blur"),
    MOSAIC("Mosaic"),
    BLACKOUT("Blackout"),
    SCRAMBLE("Scramble"),
    GLITCH("Glitch"),
    MOTION("Motion")
}

enum class HideMode {
    BLUR,
    MOSAIC,
    BLACKOUT,
    SCRAMBLE,
    GLITCH
}

enum class BlackoutShape {
    FREEHAND,
    RECTANGLE,
    OVAL
}

enum class PrivacyStampType(val text: String, val defaultColor: Long) {
    REDACTED("REDACTED", 0xFFDC2626),
    CONFIDENTIAL("CONFIDENTIAL", 0xFFB91C1C),
    TOP_SECRET("TOP SECRET", 0xFF991B1B),
    CENSORED("CENSORED", 0xFF111827),
    PRIVATE("PRIVATE", 0xFF2563EB),
    RESTRICTED("RESTRICTED", 0xFFD97706)
}

enum class ColorGradingPreset(
    val title: String,
    val subtitle: String,
    val adjustments: ColorAdjustments
) {
    ORIGINAL("Natural", "Zero adjustments", ColorAdjustments()),
    VIVID("Vivid Pop", "Punchy colors & high clarity", ColorAdjustments(brightness = 4f, contrast = 1.18f, saturation = 1.35f, vibrance = 25f, highlights = -10f, shadows = 12f, sharpness = 20f)),
    GOLDEN_HOUR("Golden Sunset", "Warm amber radiance", ColorAdjustments(brightness = 5f, contrast = 1.08f, saturation = 1.15f, temperature = 38f, tint = 8f, highlights = 15f, shadows = 8f, vignette = 18f)),
    MOODY_NOIR("Cool Film", "Cinematic cool tone", ColorAdjustments(brightness = -6f, contrast = 1.25f, saturation = 0.85f, temperature = -32f, tint = -12f, highlights = -15f, shadows = -10f, vignette = 30f)),
    VINTAGE_ANALOG("Matte Film", "Faded analog vintage", ColorAdjustments(brightness = 8f, contrast = 0.92f, saturation = 0.82f, sepia = 30f, temperature = 20f, shadows = 25f, vignette = 22f)),
    CYBERPUNK("Cyber Neon", "Futuristic neon pop", ColorAdjustments(brightness = 0f, contrast = 1.30f, saturation = 1.45f, vibrance = 45f, temperature = -25f, tint = 35f, highlights = 20f, sharpness = 35f)),
    MONOCHROME("Dramatic B&W", "Deep rich black & white", ColorAdjustments(contrast = 1.40f, saturation = 0f, brightness = -4f, highlights = 15f, shadows = -15f, sharpness = 40f, vignette = 25f)),
    PASTEL_DREAM("Pastel Dream", "Soft luminous pastel fade", ColorAdjustments(brightness = 14f, contrast = 0.88f, saturation = 0.78f, vibrance = 20f, temperature = 12f, tint = 18f, shadows = 30f, highlights = -20f)),
    EMERALD_MINT("Mint Sage", "Crisp organic botanical greens", ColorAdjustments(brightness = 2f, contrast = 1.12f, saturation = 1.10f, temperature = -15f, tint = -28f, vibrance = 30f, highlights = -8f, shadows = 14f))
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
    data class CircleSpot(
        val centerX: Float,
        val centerY: Float,
        val radiusRatio: Float,
        val effect: PrivacyEffectType = PrivacyEffectType.BLUR,
        val strength: Float = 0.8f,
        val color: Long = 0xFF000000
    ) : PrivacyMask()

    data class RectSpot(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val effect: PrivacyEffectType = PrivacyEffectType.BLUR,
        val strength: Float = 0.8f,
        val color: Long = 0xFF000000
    ) : PrivacyMask()

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

    data class BlackoutOval(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val color: Long = 0xFF000000
    ) : PrivacyMask()

    data class ScrambleStroke(
        val points: List<PointD>,
        val radiusRatio: Float,
        val grainDensity: Float = 0.6f
    ) : PrivacyMask()

    data class GlitchStroke(
        val points: List<PointD>,
        val radiusRatio: Float,
        val intensity: Float = 0.6f
    ) : PrivacyMask()

    data class PrivacyStamp(
        val centerX: Float,
        val centerY: Float,
        val text: String,
        val rotationDeg: Float = -12f,
        val color: Long = 0xFFDC2626,
        val scale: Float = 1.0f
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
    val contrast: Float = 1f,       // 0.5 to 2.5
    val saturation: Float = 1f,     // 0 to 2.5
    val exposure: Float = 0f,       // -100 to 100
    val temperature: Float = 0f,    // -100 (cool) to 100 (warm)
    val tint: Float = 0f,           // -100 (green) to 100 (magenta)
    val highlights: Float = 0f,     // -100 to 100
    val shadows: Float = 0f,        // -100 to 100
    val vibrance: Float = 0f,       // -100 to 100
    val sharpness: Float = 0f,      // 0 to 100
    val vignette: Float = 0f,       // 0 to 100
    val sepia: Float = 0f,          // 0 to 100
    val hueShift: Float = 0f        // -180 to 180
) {
    val isDefault: Boolean
        get() = brightness == 0f &&
                contrast == 1f &&
                saturation == 1f &&
                exposure == 0f &&
                temperature == 0f &&
                tint == 0f &&
                highlights == 0f &&
                shadows == 0f &&
                vibrance == 0f &&
                sharpness == 0f &&
                vignette == 0f &&
                sepia == 0f &&
                hueShift == 0f
}

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
