package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.ColorAdjustments
import com.example.model.CropTransform
import com.example.model.ExportResolution
import com.example.model.PointD
import com.example.model.PrivacyEffectType
import com.example.model.PrivacyMask
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object ImageProcessor {

    /**
     * Render the complete pipeline non-destructively:
     * 1. Transform / Crop / Rotate / Flip
     * 2. Privacy Masks (Blur, Mosaic, Motion, Blackout, Scramble, Glitch, Stamp)
     * 3. Color Grading & Adjustments
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

        // Step 3: Apply Color Grading & Adjustments
        if (!adjustments.isDefault) {
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

            is PrivacyMask.BlackoutOval -> {
                val ovalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mask.color.toInt()
                    style = Paint.Style.FILL
                }
                val left = min(mask.left, mask.right) * w
                val top = min(mask.top, mask.bottom) * h
                val right = max(mask.left, mask.right) * w
                val bottom = max(mask.top, mask.bottom) * h
                canvas.drawOval(RectF(left, top, right, bottom), ovalPaint)
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

            is PrivacyMask.ScrambleStroke -> {
                applyScrambleStroke(bitmap, canvas, mask, w, h)
            }

            is PrivacyMask.GlitchStroke -> {
                applyGlitchStroke(bitmap, canvas, mask, w, h)
            }

            is PrivacyMask.PrivacyStamp -> {
                applyPrivacyStamp(canvas, mask, w, h)
            }

            is PrivacyMask.CircleSpot -> {
                applyCircleSpot(bitmap, canvas, mask, w, h)
            }

            is PrivacyMask.RectSpot -> {
                applyRectSpot(bitmap, canvas, mask, w, h)
            }
        }
    }

    private fun applyCircleSpot(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask.CircleSpot,
        w: Float,
        h: Float
    ) {
        val cx = mask.centerX * w
        val cy = mask.centerY * h
        val radius = max(6f, mask.radiusRatio * max(w, h))

        when (mask.effect) {
            PrivacyEffectType.BLACKOUT -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mask.color.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, radius, paint)
            }
            PrivacyEffectType.BLUR -> {
                val downsample = 12
                val smallW = max(8, bitmap.width / downsample)
                val smallH = max(8, bitmap.height / downsample)
                val smallBmp = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
                val blurredSmall = fastBoxBlur(smallBmp, (mask.strength * 10).toInt().coerceAtLeast(3))
                val blurredFull = Bitmap.createScaledBitmap(blurredSmall, bitmap.width, bitmap.height, true)
                smallBmp.recycle()
                blurredSmall.recycle()

                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawCircle(cx, cy, radius, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(blurredFull, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                blurredFull.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.MOSAIC -> {
                val pixelBlock = max(8, (max(w, h) * 0.035f).toInt())
                val mosaicW = max(4, bitmap.width / pixelBlock)
                val mosaicH = max(4, bitmap.height / pixelBlock)
                val small = Bitmap.createScaledBitmap(bitmap, mosaicW, mosaicH, false)
                val pixelatedFull = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, false)
                small.recycle()

                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawCircle(cx, cy, radius, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(pixelatedFull, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                pixelatedFull.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.SCRAMBLE -> {
                val noiseW = max(8, bitmap.width / 6)
                val noiseH = max(8, bitmap.height / 6)
                val noiseSmall = Bitmap.createBitmap(noiseW, noiseH, Bitmap.Config.ARGB_8888)
                val rand = Random(42)
                val pixels = IntArray(noiseW * noiseH)
                for (i in pixels.indices) {
                    val v = rand.nextInt(256)
                    pixels[i] = Color.argb(255, v, v, v)
                }
                noiseSmall.setPixels(pixels, 0, noiseW, 0, 0, noiseW, noiseH)
                val noiseFull = Bitmap.createScaledBitmap(noiseSmall, bitmap.width, bitmap.height, false)
                noiseSmall.recycle()

                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawCircle(cx, cy, radius, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(noiseFull, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                noiseFull.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.GLITCH -> {
                val glitchBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val glitchCanvas = Canvas(glitchBmp)
                val rand = Random(1337)
                val sliceH = (bitmap.height / 30).coerceAtLeast(6)
                val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (y in 0 until bitmap.height step sliceH) {
                    val offset = (rand.nextInt(40) - 20) * mask.strength
                    val srcRect = Rect(0, y, bitmap.width, min(bitmap.height, y + sliceH))
                    val dstRect = Rect(offset.toInt(), y, bitmap.width + offset.toInt(), min(bitmap.height, y + sliceH))
                    glitchCanvas.drawBitmap(bitmap, srcRect, dstRect, slicePaint)
                }
                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawCircle(cx, cy, radius, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(glitchBmp, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                glitchBmp.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.MOTION -> {
                val motionBmp = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val motionCanvas = Canvas(motionBmp)
                val smearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 70 }
                for (i in -4..4) {
                    motionCanvas.drawBitmap(bitmap, i * 6f, 0f, smearPaint)
                }
                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawCircle(cx, cy, radius, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(motionBmp, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                motionBmp.recycle()
                maskBitmap.recycle()
            }
        }
    }

    private fun applyRectSpot(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask.RectSpot,
        w: Float,
        h: Float
    ) {
        val l = min(mask.left, mask.right) * w
        val t = min(mask.top, mask.bottom) * h
        val r = max(mask.left, mask.right) * w
        val b = max(mask.top, mask.bottom) * h

        when (mask.effect) {
            PrivacyEffectType.BLACKOUT -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mask.color.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawRect(l, t, r, b, paint)
            }
            PrivacyEffectType.BLUR -> {
                val downsample = 12
                val smallW = max(8, bitmap.width / downsample)
                val smallH = max(8, bitmap.height / downsample)
                val smallBmp = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
                val blurredSmall = fastBoxBlur(smallBmp, (mask.strength * 10).toInt().coerceAtLeast(3))
                val blurredFull = Bitmap.createScaledBitmap(blurredSmall, bitmap.width, bitmap.height, true)
                smallBmp.recycle()
                blurredSmall.recycle()

                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawRect(l, t, r, b, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(blurredFull, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                blurredFull.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.MOSAIC -> {
                val pixelBlock = max(8, (max(w, h) * 0.035f).toInt())
                val mosaicW = max(4, bitmap.width / pixelBlock)
                val mosaicH = max(4, bitmap.height / pixelBlock)
                val small = Bitmap.createScaledBitmap(bitmap, mosaicW, mosaicH, false)
                val pixelatedFull = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, false)
                small.recycle()

                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawRect(l, t, r, b, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(pixelatedFull, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                pixelatedFull.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.SCRAMBLE -> {
                val noiseW = max(8, bitmap.width / 6)
                val noiseH = max(8, bitmap.height / 6)
                val noiseSmall = Bitmap.createBitmap(noiseW, noiseH, Bitmap.Config.ARGB_8888)
                val rand = Random(42)
                val pixels = IntArray(noiseW * noiseH)
                for (i in pixels.indices) {
                    val v = rand.nextInt(256)
                    pixels[i] = Color.argb(255, v, v, v)
                }
                noiseSmall.setPixels(pixels, 0, noiseW, 0, 0, noiseW, noiseH)
                val noiseFull = Bitmap.createScaledBitmap(noiseSmall, bitmap.width, bitmap.height, false)
                noiseSmall.recycle()

                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawRect(l, t, r, b, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(noiseFull, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                noiseFull.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.GLITCH -> {
                val glitchBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val glitchCanvas = Canvas(glitchBmp)
                val rand = Random(1337)
                val sliceH = (bitmap.height / 30).coerceAtLeast(6)
                val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (y in 0 until bitmap.height step sliceH) {
                    val offset = (rand.nextInt(40) - 20) * mask.strength
                    val srcRect = Rect(0, y, bitmap.width, min(bitmap.height, y + sliceH))
                    val dstRect = Rect(offset.toInt(), y, bitmap.width + offset.toInt(), min(bitmap.height, y + sliceH))
                    glitchCanvas.drawBitmap(bitmap, srcRect, dstRect, slicePaint)
                }
                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawRect(l, t, r, b, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(glitchBmp, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                glitchBmp.recycle()
                maskBitmap.recycle()
            }
            PrivacyEffectType.MOTION -> {
                val motionBmp = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val motionCanvas = Canvas(motionBmp)
                val smearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 70 }
                for (i in -4..4) {
                    motionCanvas.drawBitmap(bitmap, i * 6f, 0f, smearPaint)
                }
                val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
                val maskCanvas = Canvas(maskBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                maskCanvas.drawRect(l, t, r, b, paint)

                val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                val count = canvas.saveLayer(0f, 0f, w, h, null)
                canvas.drawBitmap(motionBmp, 0f, 0f, compositePaint)
                compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
                canvas.restoreToCount(count)

                motionBmp.recycle()
                maskBitmap.recycle()
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

        val downsample = (4 + (mask.strength * 12).toInt()).coerceIn(4, 24)
        val smallW = max(8, (bitmap.width / downsample))
        val smallH = max(8, (bitmap.height / downsample))

        val smallBmp = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        val blurredSmall = fastBoxBlur(smallBmp, (mask.strength * 8).toInt().coerceAtLeast(2))
        val blurredFull = Bitmap.createScaledBitmap(blurredSmall, bitmap.width, bitmap.height, true)
        smallBmp.recycle()
        blurredSmall.recycle()

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

    private fun applyScrambleStroke(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask.ScrambleStroke,
        w: Float,
        h: Float
    ) {
        if (mask.points.isEmpty()) return
        val strokeWidth = max(6f, mask.radiusRatio * max(w, h) * 2f)

        // Create cryptographic noise / scramble pattern
        val blockSize = max(4, (mask.grainDensity * 12f).toInt())
        val noiseW = max(8, bitmap.width / blockSize)
        val noiseH = max(8, bitmap.height / blockSize)

        val noiseSmall = Bitmap.createBitmap(noiseW, noiseH, Bitmap.Config.ARGB_8888)
        val rand = Random(42)
        val pixels = IntArray(noiseW * noiseH)
        for (i in pixels.indices) {
            val v = rand.nextInt(256)
            pixels[i] = Color.argb(255, v, v, v)
        }
        noiseSmall.setPixels(pixels, 0, noiseW, 0, 0, noiseW, noiseH)

        val noiseFull = Bitmap.createScaledBitmap(noiseSmall, bitmap.width, bitmap.height, false)
        noiseSmall.recycle()

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
        canvas.drawBitmap(noiseFull, 0f, 0f, compositePaint)
        compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
        canvas.restoreToCount(count)

        noiseFull.recycle()
        maskBitmap.recycle()
    }

    private fun applyGlitchStroke(
        bitmap: Bitmap,
        canvas: Canvas,
        mask: PrivacyMask.GlitchStroke,
        w: Float,
        h: Float
    ) {
        if (mask.points.isEmpty()) return
        val strokeWidth = max(6f, mask.radiusRatio * max(w, h) * 2f)

        // Generate glitch slice offsets
        val glitchBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val glitchCanvas = Canvas(glitchBmp)
        val rand = Random(1337)
        val sliceH = (bitmap.height / 35).coerceAtLeast(6)
        val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (y in 0 until bitmap.height step sliceH) {
            val offset = (rand.nextInt(40) - 20) * mask.intensity
            val srcRect = Rect(0, y, bitmap.width, min(bitmap.height, y + sliceH))
            val dstRect = Rect(offset.toInt(), y, bitmap.width + offset.toInt(), min(bitmap.height, y + sliceH))
            glitchCanvas.drawBitmap(bitmap, srcRect, dstRect, slicePaint)
        }

        // Draw digital scanlines
        val scanlinePaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            this.strokeWidth = 2f
        }
        for (y in 0 until bitmap.height step 4) {
            glitchCanvas.drawLine(0f, y.toFloat(), bitmap.width.toFloat(), y.toFloat(), scanlinePaint)
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
        canvas.drawBitmap(glitchBmp, 0f, 0f, compositePaint)
        compositePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, compositePaint)
        canvas.restoreToCount(count)

        glitchBmp.recycle()
        maskBitmap.recycle()
    }

    private fun applyPrivacyStamp(
        canvas: Canvas,
        stamp: PrivacyMask.PrivacyStamp,
        w: Float,
        h: Float
    ) {
        val cx = stamp.centerX * w
        val cy = stamp.centerY * h

        canvas.save()
        canvas.rotate(stamp.rotationDeg, cx, cy)

        val stampText = "[ ${stamp.text.uppercase()} ]"
        val fontSize = (max(w, h) * 0.038f * stamp.scale).coerceIn(18f, 72f)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stamp.color.toInt()
            textSize = fontSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        textPaint.getTextBounds(stampText, 0, stampText.length, textBounds)

        val padX = fontSize * 0.5f
        val padY = fontSize * 0.35f
        val boxRect = RectF(
            cx - textBounds.width() / 2f - padX,
            cy - textBounds.height() / 2f - padY,
            cx + textBounds.width() / 2f + padX,
            cy + textBounds.height() / 2f + padY
        )

        // Semi-opaque background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(boxRect, 8f, 8f, bgPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stamp.color.toInt()
            style = Paint.Style.STROKE
            strokeWidth = max(3f, fontSize * 0.08f)
        }
        canvas.drawRoundRect(boxRect, 8f, 8f, borderPaint)

        // Text
        val fontMetrics = textPaint.fontMetrics
        val textY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(stampText, cx, textY, textPaint)

        canvas.restore()
    }

    /**
     * Advanced real-time color grading system:
     * Exposure, Brightness, Contrast, Saturation, Vibrance, Temperature, Tint,
     * Highlights, Shadows, Vignette, Sepia, Sharpness, Hue Shift.
     */
    private fun applyColorAdjustments(bitmap: Bitmap, adjustments: ColorAdjustments) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val finalMatrix = ColorMatrix()

        // 1. Exposure & Brightness & Contrast
        val contrast = adjustments.contrast.coerceIn(0.2f, 2.5f)
        val exposureFactor = (adjustments.exposure / 100f) * 60f
        val totalBrightness = adjustments.brightness.coerceIn(-100f, 100f) + exposureFactor
        val scale = contrast
        val translate = (1f - scale) * 128f + totalBrightness

        val cbMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        finalMatrix.postConcat(cbMatrix)

        // 2. Saturation & Vibrance
        val effectiveSat = (adjustments.saturation + (adjustments.vibrance / 100f) * 0.4f).coerceIn(0f, 3f)
        if (effectiveSat != 1f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(effectiveSat)
            finalMatrix.postConcat(satMatrix)
        }

        // 3. White Balance: Temperature (Warm/Cool) & Tint (Green/Magenta)
        if (adjustments.temperature != 0f || adjustments.tint != 0f) {
            val temp = adjustments.temperature / 100f // -1 to 1
            val tint = adjustments.tint / 100f       // -1 to 1

            // Warmth: Boost Red, slightly boost Green, reduce Blue
            val rTemp = 1f + (temp * 0.25f) + (tint * 0.15f)
            val gTemp = 1f + (temp * 0.05f) - (tint * 0.20f)
            val bTemp = 1f - (temp * 0.25f) + (tint * 0.15f)

            val wbMatrix = ColorMatrix(floatArrayOf(
                rTemp.coerceIn(0.4f, 1.8f), 0f, 0f, 0f, 0f,
                0f, gTemp.coerceIn(0.4f, 1.8f), 0f, 0f, 0f,
                0f, 0f, bTemp.coerceIn(0.4f, 1.8f), 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            finalMatrix.postConcat(wbMatrix)
        }

        // 4. Sepia Film Effect
        if (adjustments.sepia > 0f) {
            val s = (adjustments.sepia / 100f).coerceIn(0f, 1f)
            val sepiaMatrix = ColorMatrix(floatArrayOf(
                (1f - s) + s * 0.393f, s * 0.769f, s * 0.189f, 0f, 0f,
                s * 0.349f, (1f - s) + s * 0.686f, s * 0.168f, 0f, 0f,
                s * 0.272f, s * 0.534f, (1f - s) + s * 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            finalMatrix.postConcat(sepiaMatrix)
        }

        // 5. Hue Shift
        if (adjustments.hueShift != 0f) {
            val rad = Math.toRadians(adjustments.hueShift.toDouble())
            val cosVal = cos(rad).toFloat()
            val sinVal = sin(rad).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f

            val hueMatrix = ColorMatrix(floatArrayOf(
                lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0f, 0f,
                lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0f, 0f,
                lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            finalMatrix.postConcat(hueMatrix)
        }

        // Apply primary color matrix
        paint.colorFilter = ColorMatrixColorFilter(finalMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // 6. Vignette Effect
        if (adjustments.vignette > 0f) {
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            val radius = max(w, h) * 0.75f
            val vIntensity = (adjustments.vignette / 100f).coerceIn(0f, 1f)
            val alpha = (vIntensity * 230).toInt()

            val vignetteShader = RadialGradient(
                w / 2f, h / 2f, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb((alpha * 0.4f).toInt(), 0, 0, 0), Color.argb(alpha, 0, 0, 0)),
                floatArrayOf(0.4f, 0.75f, 1.0f),
                Shader.TileMode.CLAMP
            )

            val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = vignetteShader
            }
            canvas.drawRect(0f, 0f, w, h, vPaint)
        }
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
