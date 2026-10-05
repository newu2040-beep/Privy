package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.example.model.ColorAdjustments
import com.example.model.CropTransform
import com.example.model.ExportResolution
import com.example.model.PointD
import com.example.model.PrivacyMask
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object ImageProcessor {

    /**
     * Render the complete pipeline non-destructively:
     * 1. Transform / Crop / Rotate / Flip
     * 2. Privacy Masks (Blur, Mosaic, Motion, Blackout) baked into pixels
     * 3. Color Adjustments (Brightness, Contrast, Saturation)
     */
    fun renderFinalImage(
        source: Bitmap,
        crop: CropTransform,
        masks: List<PrivacyMask>,
        adjustments: ColorAdjustments,
        targetResolution: ExportResolution = ExportResolution.ORIGINAL
    ): Bitmap {
        // Step 1: Apply Crop & Geometric Transform
        val transformed = applyCropTransform(source, crop)

        // If target resolution specifies 4K or 8K or 1080p, scale appropriately
        val scaledSource = if (targetResolution != ExportResolution.ORIGINAL && targetResolution.maxDimension > 0) {
            val maxSide = max(transformed.width, transformed.height)
            val scale = targetResolution.maxDimension.toFloat() / maxSide.toFloat()
            val targetW = (transformed.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (transformed.height * scale).roundToInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(transformed, targetW, targetH, true)
            if (transformed != source && transformed != scaled) {
                transformed.recycle()
            }
            scaled
        } else {
            transformed
        }

        // Step 2: Apply Privacy Masks onto transformed bitmap
        val output = scaledSource.copy(Bitmap.Config.ARGB_8888, true)
        if (scaledSource != source) {
            scaledSource.recycle()
        }

        val canvas = Canvas(output)
        val w = output.width.toFloat()
        val h = output.height.toFloat()

        for (mask in masks) {
            applyMask(output, canvas, mask, w, h)
        }

        // Step 3: Apply Color Adjustments
        if (adjustments.brightness != 0f || adjustments.contrast != 1f || adjustments.saturation != 1f) {
            applyColorAdjustments(output, adjustments)
        }

        return output
    }

    private fun applyCropTransform(src: Bitmap, crop: CropTransform): Bitmap {
        val matrix = Matrix()

        if (crop.flipHorizontal) {
            matrix.postScale(-1f, 1f, src.width / 2f, src.height / 2f)
        }
        if (crop.flipVertical) {
            matrix.postScale(1f, -1f, src.width / 2f, src.height / 2f)
        }
        if (crop.rotationDegrees != 0) {
            matrix.postRotate(crop.rotationDegrees.toFloat(), src.width / 2f, src.height / 2f)
        }

        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)

        // Now apply crop rectangle if sub-region
        val cl = (crop.cropLeft.coerceIn(0f, 1f) * rotated.width).roundToInt()
        val ct = (crop.cropTop.coerceIn(0f, 1f) * rotated.height).roundToInt()
        val cr = (crop.cropRight.coerceIn(0f, 1f) * rotated.width).roundToInt()
        val cb = (crop.cropBottom.coerceIn(0f, 1f) * rotated.height).roundToInt()

        val cropWidth = max(1, cr - cl)
        val cropHeight = max(1, cb - ct)

        return if (cropWidth < rotated.width || cropHeight < rotated.height) {
            val cropped = Bitmap.createBitmap(rotated, cl, ct, min(cropWidth, rotated.width - cl), min(cropHeight, rotated.height - ct))
            if (rotated != src) rotated.recycle()
            cropped
        } else {
            rotated
        }
    }

    private fun applyMask(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask,
        w: Float,
        h: Float
    ) {
        when (mask) {
            is PrivacyMask.BlackoutStroke -> {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mask.color.toInt()
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = max(4f, mask.radiusRatio * max(w, h) * 2f)
                }
                val path = buildPath(mask.points, w, h)
                canvas.drawPath(path, strokePaint)
            }

            is PrivacyMask.BlackoutRect -> {
                val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mask.color.toInt()
                    style = Paint.Style.FILL
                }
                val left = min(mask.left, mask.right) * w
                val top = min(mask.top, mask.bottom) * h
                val right = max(mask.left, mask.right) * w
                val bottom = max(mask.top, mask.bottom) * h
                canvas.drawRect(left, top, right, bottom, rectPaint)
            }

            is PrivacyMask.BlurStroke -> {
                applyBlurStroke(bitmap, canvas, mask, w, h)
            }

            is PrivacyMask.MosaicStroke -> {
                applyMosaicStroke(bitmap, canvas, mask, w, h)
            }

            is PrivacyMask.MotionStroke -> {
                applyMotionStroke(bitmap, canvas, mask, w, h)
            }
        }
    }

    private fun buildPath(points: List<PointD>, w: Float, h: Float): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].x * w, points[0].y * h)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x * w, points[i].y * h)
        }
        return path
    }

    private fun applyBlurStroke(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask.BlurStroke,
        w: Float,
        h: Float
    ) {
        if (mask.points.isEmpty()) return
        val radiusPx = max(4f, mask.radiusRatio * max(w, h))
        val strokeWidth = radiusPx * 2f

        // Create blurred copy of bitmap
        // To achieve high quality and fast blur, scale down by factor depending on strength, blur, and scale back up
        val downsample = (4 + (mask.strength * 12).toInt()).coerceIn(4, 24)
        val smallW = max(8, (bitmap.width / downsample))
        val smallH = max(8, (bitmap.height / downsample))

        val smallBmp = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        val blurredSmall = fastBoxBlur(smallBmp, (mask.strength * 8).toInt().coerceAtLeast(2))
        val blurredFull = Bitmap.createScaledBitmap(blurredSmall, bitmap.width, bitmap.height, true)
        smallBmp.recycle()
        blurredSmall.recycle()

        // Mask the blurred image onto canvas only where the stroke was painted
        val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(maskBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        maskCanvas.drawPath(buildPath(mask.points, w, h), paint)

        // Draw blurred copy masked
        val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = canvas.saveLayer(0f, 0f, w, h, null)
        canvas.drawBitmap(blurredFull, 0f, 0f, compositePaint)
        compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
        canvas.restoreToCount(count)

        blurredFull.recycle()
        maskBitmap.recycle()
    }

    private fun applyMosaicStroke(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask.MosaicStroke,
        w: Float,
        h: Float
    ) {
        if (mask.points.isEmpty()) return
        val strokeWidth = max(6f, mask.radiusRatio * max(w, h) * 2f)
        val pixelBlockSize = max(8, (mask.pixelSizeRatio * max(w, h)).toInt().coerceIn(12, 80))

        val mosaicW = max(4, bitmap.width / pixelBlockSize)
        val mosaicH = max(4, bitmap.height / pixelBlockSize)

        val small = Bitmap.createScaledBitmap(bitmap, mosaicW, mosaicH, false)
        val pixelatedFull = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, false)
        small.recycle()

        val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(maskBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        maskCanvas.drawPath(buildPath(mask.points, w, h), paint)

        val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = canvas.saveLayer(0f, 0f, w, h, null)
        canvas.drawBitmap(pixelatedFull, 0f, 0f, compositePaint)
        compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
        canvas.restoreToCount(count)

        pixelatedFull.recycle()
        maskBitmap.recycle()
    }

    private fun applyMotionStroke(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask.MotionStroke,
        w: Float,
        h: Float
    ) {
        if (mask.points.isEmpty()) return
        val strokeWidth = max(6f, mask.radiusRatio * max(w, h) * 2f)
        val distance = (mask.strength * 40f).coerceIn(8f, 60f)
        val radians = Math.toRadians(mask.angleDegrees.toDouble())
        val dx = (distance * cos(radians)).toFloat()
        val dy = (distance * sin(radians)).toFloat()

        // Create motion smear by multi-sampling offset passes
        val motionBmp = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val motionCanvas = Canvas(motionBmp)
        val smearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val steps = 9
        smearPaint.alpha = (255f / steps * 1.5f).toInt().coerceIn(20, 255)

        for (i in -steps / 2..steps / 2) {
            val stepFrac = i.toFloat() / (steps / 2f)
            motionCanvas.drawBitmap(bitmap, dx * stepFrac, dy * stepFrac, smearPaint)
        }

        val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(maskBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        maskCanvas.drawPath(buildPath(mask.points, w, h), paint)

        val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = canvas.saveLayer(0f, 0f, w, h, null)
        canvas.drawBitmap(motionBmp, 0f, 0f, compositePaint)
        compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
        canvas.restoreToCount(count)

        motionBmp.recycle()
        maskBitmap.recycle()
    }

    private fun applyColorAdjustments(bitmap: Bitmap, adjustments: ColorAdjustments) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix()

        // Saturation
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(adjustments.saturation.coerceIn(0f, 3f))

        // Contrast & Brightness
        val contrast = adjustments.contrast.coerceIn(0.2f, 2.5f)
        val brightness = adjustments.brightness.coerceIn(-100f, 100f)
        val scale = contrast
        val translate = (1f - scale) * 128f + brightness

        val cbMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        cm.postConcat(satMatrix)
        cm.postConcat(cbMatrix)

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    /**
     * Efficient 2-pass box blur for high performance on mobile devices.
     */
    fun fastBoxBlur(src: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 25)
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)

        val newPix = IntArray(w * h)
        val div = 2 * r + 1

        // Horizontal pass
        for (y in 0 until h) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0
            val lineOffset = y * w

            for (i in -r..r) {
                val p = pix[lineOffset + i.coerceIn(0, w - 1)]
                aSum += (p ushr 24) and 0xFF
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
            }

            for (x in 0 until w) {
                newPix[lineOffset + x] = ((aSum / div) shl 24) or
                        ((rSum / div) shl 16) or
                        ((gSum / div) shl 8) or
                        (bSum / div)

                val pLeft = pix[lineOffset + (x - r).coerceIn(0, w - 1)]
                val pRight = pix[lineOffset + (x + r + 1).coerceIn(0, w - 1)]

                aSum += ((pRight ushr 24) and 0xFF) - ((pLeft ushr 24) and 0xFF)
                rSum += ((pRight ushr 16) and 0xFF) - ((pLeft ushr 16) and 0xFF)
                gSum += ((pRight ushr 8) and 0xFF) - ((pLeft ushr 8) and 0xFF)
                bSum += (pRight and 0xFF) - (pLeft and 0xFF)
            }
        }

        // Vertical pass
        val finalPix = IntArray(w * h)
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0

            for (i in -r..r) {
                val p = newPix[i.coerceIn(0, h - 1) * w + x]
                aSum += (p ushr 24) and 0xFF
                rSum += (p ushr 16) and 0xFF
                gSum += (p ushr 8) and 0xFF
                bSum += p and 0xFF
            }

            for (y in 0 until h) {
                finalPix[y * w + x] = ((aSum / div) shl 24) or
                        ((rSum / div) shl 16) or
                        ((gSum / div) shl 8) or
                        (bSum / div)

                val pTop = newPix[(y - r).coerceIn(0, h - 1) * w + x]
                val pBottom = newPix[(y + r + 1).coerceIn(0, h - 1) * w + x]

                aSum += ((pBottom ushr 24) and 0xFF) - ((pTop ushr 24) and 0xFF)
                rSum += ((pBottom ushr 16) and 0xFF) - ((pTop ushr 16) and 0xFF)
                gSum += ((pBottom ushr 8) and 0xFF) - ((pTop ushr 8) and 0xFF)
                bSum += (pBottom and 0xFF) - (pTop and 0xFF)
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(finalPix, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Create downsampled preview bitmap to prevent OutOfMemory issues.
     */
    fun downsampleForDisplay(source: Bitmap, maxDimension: Int = 1600): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDimension && h <= maxDimension) return source

        val ratio = min(maxDimension.toFloat() / w, maxDimension.toFloat() / h)
        val targetW = (w * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (h * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
