package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.model.ColorAdjustments
import com.example.model.CropTransform
import com.example.model.ExportFormat
import com.example.model.PointD
import com.example.model.PrivacyMask
import com.example.model.PrivacySettings
import com.example.processing.ImageFormatConverter
import com.example.processing.ImageProcessor
import com.example.processing.MetadataSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PRIVY", appName)
    }

    @Test
    fun testGenerateCleanFilename() {
        val settings = PrivacySettings(secureFilename = true)
        val filename = MetadataSanitizer.generateCleanFilename("IMG_2048.JPG", settings, "jpg")
        assertTrue("Filename should start with PRIVY_", filename.startsWith("PRIVY_"))

        val rawSettings = PrivacySettings(secureFilename = false)
        val regularName = MetadataSanitizer.generateCleanFilename("IMG_2048.JPG", rawSettings, "jpg")
        assertEquals("IMG_2048_clean", regularName)
    }

    @Test
    fun testEstimateFileSize() {
        val estimate = ImageFormatConverter.estimateFileSize(4032, 3024, ExportFormat.JPG, 92)
        assertNotNull(estimate)
        assertTrue(estimate.endsWith("MB") || estimate.endsWith("KB"))
    }

    @Test
    fun testImageRenderingPipeline() {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val masks = listOf(
            PrivacyMask.BlackoutRect(0.1f, 0.1f, 0.5f, 0.5f, 0xFF000000)
        )
        val rendered = ImageProcessor.renderFinalImage(
            source = bmp,
            crop = CropTransform(),
            masks = masks,
            adjustments = ColorAdjustments()
        )
        assertNotNull(rendered)
        assertEquals(100, rendered.width)
        assertEquals(100, rendered.height)
    }

    @Test
    fun test4KAnd8KResolutionScaling() {
        val bmp = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
        val rendered4K = ImageProcessor.renderFinalImage(
            source = bmp,
            crop = CropTransform(),
            masks = emptyList(),
            adjustments = ColorAdjustments(),
            targetResolution = com.example.model.ExportResolution.UHD_4K
        )
        assertEquals(3840, rendered4K.width)
        assertEquals(2880, rendered4K.height)

        val estimate4K = ImageFormatConverter.estimateFileSize(400, 300, ExportFormat.JPG, 90, com.example.model.ExportResolution.UHD_4K)
        assertNotNull(estimate4K)
        assertTrue(estimate4K.endsWith("MB") || estimate4K.endsWith("KB"))
    }

    @Test
    fun testPastelColorSchemes() {
        for (palette in com.example.ui.theme.PastelPalette.values()) {
            val light = com.example.ui.theme.getPastelColorScheme(palette, false)
            val dark = com.example.ui.theme.getPastelColorScheme(palette, true)
            assertNotNull(light)
            assertNotNull(dark)
        }
    }

    @Test
    fun testMetadataInspectionAndSanitization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File(context.cacheDir, "test_exif.jpg")
        val bmp = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        FileOutputStream(tempFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }

        // Add EXIF tags via ExifInterface
        val exifWriter = ExifInterface(tempFile.absolutePath)
        exifWriter.setAttribute(ExifInterface.TAG_MAKE, "TestMaker")
        exifWriter.setAttribute(ExifInterface.TAG_MODEL, "CameraPro")
        exifWriter.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "37/1,46/1,30/1")
        exifWriter.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
        exifWriter.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "122/1,25/1,6/1")
        exifWriter.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
        exifWriter.saveAttributes()

        // 1. Inspect using MetadataSanitizer wrapper
        val inspected = MetadataSanitizer.extractMetadata(tempFile)
        assertEquals("TestMaker", inspected.cameraMake)
        assertEquals("CameraPro", inspected.cameraModel)
        assertTrue("Detected tags should be >= 2", inspected.detectedTagsCount >= 2)

        // 2. Sanitize using MetadataSanitizer
        val report = MetadataSanitizer.sanitizeFileExif(tempFile, PrivacySettings(stripAllMetadata = true))
        assertTrue("File should be clean after sanitization", report.isClean)
        assertTrue("Location should be marked removed", report.locationRemoved)

        // 3. Verify with ExifInterface directly that tags are stripped
        val verifyExif = ExifInterface(tempFile.absolutePath)
        assertNull(verifyExif.getAttribute(ExifInterface.TAG_MAKE))
        assertNull(verifyExif.getAttribute(ExifInterface.TAG_MODEL))
        assertNull(verifyExif.latLong)

        tempFile.delete()
    }
}
