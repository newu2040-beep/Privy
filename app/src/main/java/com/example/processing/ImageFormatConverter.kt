package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import com.example.model.ExportFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong

object ImageFormatConverter {

    fun convertAndSave(
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        destinationFile: File
    ): Result<File> {
        return try {
            destinationFile.parentFile?.mkdirs()
            FileOutputStream(destinationFile).use { fos ->
                writeToStream(bitmap, format, quality, fos)
            }
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun writeToStream(
        bitmap: Bitmap,
        format: ExportFormat,
        quality: Int,
        outputStream: OutputStream
    ) {
        val q = quality.coerceIn(1, 100)

        when (format) {
            ExportFormat.JPG -> {
                // Ensure opaque background (no black alpha artifacts)
                val rgbBitmap = ensureOpaqueRgb(bitmap)
                rgbBitmap.compress(Bitmap.CompressFormat.JPEG, q, outputStream)
                if (rgbBitmap != bitmap) rgbBitmap.recycle()
            }

            ExportFormat.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            ExportFormat.WEBP -> {
                var compressed = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val compressFormat = if (q >= 100) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                    compressed = bitmap.compress(compressFormat, q, outputStream)
                } else {
                    @Suppress("DEPRECATION")
                    compressed = bitmap.compress(Bitmap.CompressFormat.WEBP, q, outputStream)
                }
                if (!compressed) {
                    // Fallback to PNG if native WEBP encoder is absent in environment
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }

            ExportFormat.BMP -> {
                writeBmp(bitmap, outputStream)
            }

            ExportFormat.PDF -> {
                writePdf(bitmap, outputStream)
            }

            ExportFormat.HEIC -> {
                var compressed = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val formatEnum = runCatching { Bitmap.CompressFormat.valueOf("HEIF") }
                            .getOrElse { runCatching { Bitmap.CompressFormat.valueOf("HEIC") }.getOrNull() }
                        if (formatEnum != null) {
                            compressed = bitmap.compress(formatEnum, q, outputStream)
                        }
                    } catch (e: Throwable) {
                        compressed = false
                    }
                }
                if (!compressed) {
                    // Fallback to high quality JPEG if HEIC encoder hardware missing
                    val rgb = ensureOpaqueRgb(bitmap)
                    rgb.compress(Bitmap.CompressFormat.JPEG, q, outputStream)
                    if (rgb != bitmap) rgb.recycle()
                }
            }

            ExportFormat.TIFF -> {
                writeTiff(bitmap, outputStream)
            }
        }
    }

    private fun ensureOpaqueRgb(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(opaque)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return opaque
    }

    private fun writePdf(bitmap: Bitmap, outputStream: OutputStream) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = document.startPage(pageInfo)

            val canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            document.finishPage(page)
            document.writeTo(outputStream)
            document.close()
        } catch (e: Throwable) {
            writeFallbackPdf(bitmap, outputStream)
        }
    }

    private fun writeFallbackPdf(bitmap: Bitmap, outputStream: OutputStream) {
        val baos = ByteArrayOutputStream()
        val rgb = ensureOpaqueRgb(bitmap)
        rgb.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        if (rgb != bitmap) rgb.recycle()
        val jpegBytes = baos.toByteArray()

        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
        sb.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n")
        sb.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 ${bitmap.width} ${bitmap.height}] /Contents 4 0 R /Resources << /XObject << /Im1 5 0 R >> >> >> endobj\n")
        val streamContent = "q ${bitmap.width} 0 0 ${bitmap.height} 0 0 cm /Im1 Do Q"
        sb.append("4 0 obj << /Length ${streamContent.length} >> stream\n$streamContent\nendstream\nendobj\n")
        sb.append("5 0 obj << /Type /XObject /Subtype /Image /Width ${bitmap.width} /Height ${bitmap.height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.size} >> stream\n")

        outputStream.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        outputStream.write(jpegBytes)
        val tail = "\nendstream\nendobj\nxref\n0 6\n0000000000 65535 f \ntrailer << /Size 6 /Root 1 0 R >>\nstartxref\n%%EOF\n"
        outputStream.write(tail.toByteArray(Charsets.ISO_8859_1))
    }

    /**
     * Standard 24-bit uncompressed Windows BMP writer.
     */
    private fun writeBmp(bitmap: Bitmap, outputStream: OutputStream) {
        val width = bitmap.width
        val height = bitmap.height
        val rowSize = (width * 3 + 3) and 3.inv() // Rows padded to 4-byte boundary
        val imageSize = rowSize * height
        val fileSize = 54 + imageSize

        val header = ByteBuffer.allocate(54).order(ByteOrder.LITTLE_ENDIAN)
        // BMP Header (14 bytes)
        header.put('B'.code.toByte())
        header.put('M'.code.toByte())
        header.putInt(fileSize)
        header.putShort(0) // reserved 1
        header.putShort(0) // reserved 2
        header.putInt(54) // offset to pixel data

        // DIB Header (BITMAPINFOHEADER - 40 bytes)
        header.putInt(40) // header size
        header.putInt(width)
        header.putInt(height) // positive = bottom-up
        header.putShort(1) // color planes
        header.putShort(24) // bits per pixel
        header.putInt(0) // BI_RGB (uncompressed)
        header.putInt(imageSize)
        header.putInt(2835) // 72 DPI horizontal (pixels/meter)
        header.putInt(2835) // 72 DPI vertical
        header.putInt(0) // palette colors
        header.putInt(0) // important colors

        outputStream.write(header.array())

        val pixels = IntArray(width)
        val rowBuffer = ByteArray(rowSize)

        // Write bottom-up
        for (y in height - 1 downTo 0) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            var colIdx = 0
            for (x in 0 until width) {
                val p = pixels[x]
                rowBuffer[colIdx++] = (p and 0xFF).toByte()         // Blue
                rowBuffer[colIdx++] = ((p ushr 8) and 0xFF).toByte()  // Green
                rowBuffer[colIdx++] = ((p ushr 16) and 0xFF).toByte() // Red
            }
            while (colIdx < rowSize) {
                rowBuffer[colIdx++] = 0
            }
            outputStream.write(rowBuffer)
        }
    }

    /**
     * Standard baseline 24-bit uncompressed RGB TIFF writer.
     */
    private fun writeTiff(bitmap: Bitmap, outputStream: OutputStream) {
        val width = bitmap.width
        val height = bitmap.height
        val stripBytes = width * height * 3
        val ifdOffset = 8 + stripBytes

        // Header (8 bytes): Little Endian "II", 42, IFD offset
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        header.put('I'.code.toByte())
        header.put('I'.code.toByte())
        header.putShort(42)
        header.putInt(ifdOffset)
        outputStream.write(header.array())

        // Pixel data (strip)
        val pixels = IntArray(width)
        val rowBuffer = ByteArray(width * 3)
        for (y in 0 until height) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            var idx = 0
            for (x in 0 until width) {
                val p = pixels[x]
                rowBuffer[idx++] = ((p ushr 16) and 0xFF).toByte() // R
                rowBuffer[idx++] = ((p ushr 8) and 0xFF).toByte()  // G
                rowBuffer[idx++] = (p and 0xFF).toByte()          // B
            }
            outputStream.write(rowBuffer)
        }

        // IFD: 10 tags
        val numTags: Short = 10
        val ifdSize = 2 + numTags * 12 + 4 + 6 // includes 6 bytes extra for BitsPerSample
        val ifd = ByteBuffer.allocate(ifdSize).order(ByteOrder.LITTLE_ENDIAN)
        val bitsPerSampleOffset = ifdOffset + 2 + numTags * 12 + 4

        ifd.putShort(numTags)
        fun putTag(tag: Short, type: Short, count: Int, value: Int) {
            ifd.putShort(tag)
            ifd.putShort(type)
            ifd.putInt(count)
            ifd.putInt(value)
        }

        putTag(256.toShort(), 3.toShort(), 1, width)            // ImageWidth
        putTag(257.toShort(), 3.toShort(), 1, height)           // ImageLength
        putTag(258.toShort(), 3.toShort(), 3, bitsPerSampleOffset) // BitsPerSample
        putTag(259.toShort(), 3.toShort(), 1, 1)                // Compression (1 = uncompressed)
        putTag(262.toShort(), 3.toShort(), 1, 2)                // PhotometricInterpretation (2 = RGB)
        putTag(273.toShort(), 4.toShort(), 1, 8)                // StripOffsets
        putTag(277.toShort(), 3.toShort(), 1, 3)                // SamplesPerPixel
        putTag(278.toShort(), 3.toShort(), 1, height)           // RowsPerStrip
        putTag(279.toShort(), 4.toShort(), 1, stripBytes)       // StripByteCounts
        putTag(284.toShort(), 3.toShort(), 1, 1)                // PlanarConfiguration (1 = chunky)

        ifd.putInt(0) // Next IFD offset
        // BitsPerSample values (8, 8, 8)
        ifd.putShort(8)
        ifd.putShort(8)
        ifd.putShort(8)

        outputStream.write(ifd.array())
    }

    fun estimateFileSize(
        width: Int,
        height: Int,
        format: ExportFormat,
        quality: Int,
        resolution: com.example.model.ExportResolution = com.example.model.ExportResolution.ORIGINAL
    ): String {
        val (effW, effH) = if (resolution != com.example.model.ExportResolution.ORIGINAL && resolution.maxDimension > 0) {
            val maxSide = maxOf(width, height)
            val scale = resolution.maxDimension.toFloat() / maxSide.toFloat()
            (width * scale).toLong() to (height * scale).toLong()
        } else {
            width.toLong() to height.toLong()
        }
        val pixels = effW * effH
        val bytes = when (format) {
            ExportFormat.JPG -> {
                val factor = (quality / 100f) * 0.35f + 0.05f
                (pixels * factor).roundToLong()
            }
            ExportFormat.PNG -> (pixels * 1.8f).roundToLong()
            ExportFormat.WEBP -> {
                val factor = (quality / 100f) * 0.22f + 0.04f
                (pixels * factor).roundToLong()
            }
            ExportFormat.BMP -> (pixels * 3) + 54
            ExportFormat.TIFF -> (pixels * 3) + 200
            ExportFormat.PDF -> {
                val factor = (quality / 100f) * 0.38f + 0.06f
                (pixels * factor).roundToLong() + 4096
            }
            ExportFormat.HEIC -> {
                val factor = (quality / 100f) * 0.18f + 0.03f
                (pixels * factor).roundToLong()
            }
        }

        return formatBytes(bytes)
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }
}
