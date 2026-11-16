// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.slider

// Ported from COUI-Kit (ItosEO/COUI-Kit@8a09da4) coui/src/main/java/com/itos/coui/slider/CouiSliderRenderer.kt. Extended with secondary progress, middle fill, custom keypoints, and visibility options.

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Immutable
internal data class CouiSliderColors(
    val activeTrack: Color,
    val inactiveTrack: Color,
    val thumb: Color,
    val activeTick: Color,
    val inactiveTick: Color,
    val thumbShadow: Color,
    val secondaryTrack: Color = Color.Unspecified,
)

internal expect fun Canvas.drawSliderThumbWithShadow(
    x: Float,
    y: Float,
    radius: Float,
    density: Float,
    thumbColor: Color,
    shadowColor: Color,
    drawShadow: Boolean,
    paint: Paint,
)

internal expect fun Canvas.drawSliderInnerShadowStroke(
    bounds: Rect,
    strokeWidth: Float,
    blurRadius: Float,
    dy: Float,
    shadowColor: Color,
)

internal expect fun ImageBitmap.releaseResources()

/** Horizontal track and textured thumb renderer; host [Shape] controls both track outlines and light clipping. */
internal class CouiSliderRenderer(
    private val density: Float,
    private val shape: Shape,
    private val layoutDirection: LayoutDirection,
) {
    private val paint = Paint().apply { isAntiAlias = true }
    private val bitmapPaint = Paint().apply {
        isAntiAlias = true
        filterQuality = FilterQuality.Low
    }
    private val layerPaint = Paint()
    private val backgroundPath = Path()
    private val progressPath = Path()
    private val secondaryPath = Path()
    private var backgroundBounds = Rect.Zero
    private var progressBounds = Rect.Zero
    private var secondaryBounds = Rect.Zero
    private val outlineDensity = Density(density)
    private var layerB: ImageBitmap? = null

    fun draw(
        canvas: Canvas,
        height: Float,
        length: Float,
        visualFraction: Float,
        heightFraction: Float,
        thumbRadiusDp: Float,
        texture: Float,
        thumbCover: Float,
        glitter: Float,
        colors: CouiSliderColors,
        enabled: Boolean,
        rtl: Boolean,
        tickCount: Int,
        keyPointFractions: FloatArray? = null,
        secondaryProgressFraction: Float? = null,
        showProgress: Boolean = true,
        showThumb: Boolean = true,
        startFromMiddle: Boolean = false,
        customTrackHeight: Float? = null,
    ) {
        val centerY = (height.toInt() / 2).toFloat()
        val defaultTrackHeight = 20f * density
        val trackHeight = customTrackHeight ?: defaultTrackHeight
        val scale = (trackHeight / defaultTrackHeight).coerceAtLeast(0.1f)
        val start = 14f * density * scale
        val thumbX = start + length * if (rtl) 1f - visualFraction else visualFraction
        val trackRadius = trackHeight / 2f
        val backgroundRadius = backgroundHeight(heightFraction, scale, trackHeight) / 2f
        val backgroundRect = Rect(
            left = start - backgroundRadius,
            top = centerY - backgroundRadius,
            right = start + length + backgroundRadius,
            bottom = centerY + backgroundRadius,
        )
        backgroundBounds = updateTrackPath(backgroundPath, backgroundBounds, backgroundRect)
        paint.color = colors.inactiveTrack
        canvas.drawPath(backgroundPath, paint)

        if (secondaryProgressFraction != null && colors.secondaryTrack != Color.Unspecified) {
            val secFraction = secondaryProgressFraction.coerceIn(0f, 1f)
            val secThumbX = start + length * if (rtl) 1f - secFraction else secFraction
            val secOriginX = if (startFromMiddle) {
                start + length / 2f
            } else {
                if (rtl) start + length else start
            }
            val secondaryRect = Rect(
                left = min(secOriginX, secThumbX) - trackRadius,
                top = centerY - trackRadius,
                right = max(secOriginX, secThumbX) + trackRadius,
                bottom = centerY + trackRadius,
            )
            secondaryBounds = updateTrackPath(secondaryPath, secondaryBounds, secondaryRect)
            paint.color = colors.secondaryTrack
            canvas.drawPath(secondaryPath, paint)
        }

        var progressRect = Rect.Zero
        if (showProgress) {
            val originX = if (startFromMiddle) {
                start + length / 2f
            } else {
                if (rtl) start + length else start
            }
            progressRect = Rect(
                left = min(originX, thumbX) - trackRadius,
                top = centerY - trackRadius,
                right = max(originX, thumbX) + trackRadius,
                bottom = centerY + trackRadius,
            )
            progressBounds = updateTrackPath(progressPath, progressBounds, progressRect)
            paint.color = colors.activeTrack
            canvas.drawPath(progressPath, paint)
        }

        val thumbRadius = thumbRadiusDp * density * scale
        if (keyPointFractions != null && keyPointFractions.isNotEmpty()) {
            if (keyPointFractions.size <= 50) {
                val markRadius = 3f * density * scale
                for (markFraction in keyPointFractions) {
                    drawTickCircle(
                        canvas = canvas,
                        start = start,
                        length = length,
                        centerY = centerY,
                        thumbX = thumbX,
                        thumbRadius = thumbRadius,
                        markRadius = markRadius,
                        markFraction = markFraction,
                        visualFraction = visualFraction,
                        rtl = rtl,
                        colors = colors,
                    )
                }
            }
        } else if (tickCount in 1..50) {
            val markRadius = 3f * density * scale
            for (index in 0..tickCount) {
                val markFraction = index.toFloat() / tickCount
                drawTickCircle(
                    canvas = canvas,
                    start = start,
                    length = length,
                    centerY = centerY,
                    thumbX = thumbX,
                    thumbRadius = thumbRadius,
                    markRadius = markRadius,
                    markFraction = markFraction,
                    visualFraction = visualFraction,
                    rtl = rtl,
                    colors = colors,
                )
            }
        }

        if (showThumb) {
            drawThumb(canvas, thumbX, centerY, thumbRadius, texture, thumbCover, colors, enabled)
        }

        if (showProgress) {
            val glitterAlpha = (255.0 * (1.0 - exp(-ln(85.0) / 360.0 * glitter.coerceAtLeast(0f)))).roundToInt()
            if (glitterAlpha > 0 && progressRect.width > 0f) {
                paint.color = Color.White
                paint.alpha = glitterAlpha.coerceIn(0, 255) / 255f
                paint.shader = LinearGradientShader(
                    from = Offset(if (rtl) progressRect.right else progressRect.left, 0f),
                    to = Offset(if (rtl) progressRect.left else progressRect.right, 0f),
                    colors = GlitterColors,
                    tileMode = TileMode.Clamp,
                )
                canvas.drawPath(progressPath, paint)
                paint.shader = null
                paint.alpha = 1f
            }
        }
    }

    fun clipTrack(canvas: Canvas) {
        canvas.clipPath(backgroundPath)
    }

    fun dispose() {
        layerB?.releaseResources()
        layerB = null
    }

    private fun drawTickCircle(
        canvas: Canvas,
        start: Float,
        length: Float,
        centerY: Float,
        thumbX: Float,
        thumbRadius: Float,
        markRadius: Float,
        markFraction: Float,
        visualFraction: Float,
        rtl: Boolean,
        colors: CouiSliderColors,
    ) {
        val x = start + length * if (rtl) 1f - markFraction else markFraction
        if (abs(x - thumbX) + markRadius > thumbRadius) {
            val isSelected = if (rtl) markFraction >= visualFraction else markFraction <= visualFraction
            paint.color = if (isSelected) colors.activeTick else colors.inactiveTick
            canvas.drawCircle(Offset(x, centerY), markRadius, paint)
        }
    }

    private fun backgroundHeight(heightFraction: Float, scale: Float = 1f, baseHeight: Float = 20f * density): Float {
        val height = baseHeight + 8f * density * scale * heightFraction
        return if (heightFraction > 0f && heightFraction < 1f) height.roundToInt().toFloat() else height
    }

    private fun updateTrackPath(path: Path, previousBounds: Rect, bounds: Rect): Rect {
        if (previousBounds == bounds) return previousBounds
        val outline = shape.createOutline(Size(bounds.width, bounds.height), layoutDirection, outlineDensity)
        path.reset()
        path.addOutline(outline)
        path.translate(Offset(bounds.left, bounds.top))
        return bounds
    }

    private fun drawThumb(
        canvas: Canvas,
        x: Float,
        y: Float,
        radius: Float,
        texture: Float,
        thumbCover: Float,
        colors: CouiSliderColors,
        enabled: Boolean,
    ) {
        val textureAlpha = (texture * 2.55f).toInt().coerceIn(0, 255)
        if (!enabled || textureAlpha <= 0) {
            canvas.drawSliderThumbWithShadow(
                x = x,
                y = y,
                radius = radius,
                density = density,
                thumbColor = colors.thumb,
                shadowColor = colors.thumbShadow,
                drawShadow = enabled,
                paint = paint,
            )
            return
        }
        val textureRect = Rect(x - radius, y - radius, x + radius, y + radius)
        layerPaint.alpha = textureAlpha / 255f
        canvas.saveLayer(textureRect, layerPaint)
        paint.color = Color.White
        paint.alpha = 1f
        paint.shader = LinearGradientShader(
            from = Offset(0f, textureRect.bottom),
            to = Offset(0f, textureRect.top),
            colors = LayerAColors,
            colorStops = LayerAStops,
            tileMode = TileMode.Clamp,
        )
        canvas.drawRoundRect(
            left = textureRect.left,
            top = textureRect.top,
            right = textureRect.right,
            bottom = textureRect.bottom,
            radiusX = radius,
            radiusY = radius,
            paint = paint,
        )
        paint.shader = null
        // Native inner-shadow stroke, blur, and padding use physical pixels; the bitmap is created once.
        val bitmap = layerB ?: createLayerB(radius).also { layerB = it }
        val bitmapLeft = textureRect.left - 12f
        val bitmapTop = textureRect.top - 12f
        val bitmapWidth = textureRect.width + 24f
        val bitmapHeight = textureRect.height + 24f
        canvas.save()
        canvas.translate(bitmapLeft, bitmapTop)
        canvas.scale(bitmapWidth / bitmap.width, bitmapHeight / bitmap.height)
        canvas.drawImage(bitmap, Offset.Zero, bitmapPaint)
        canvas.restore()
        canvas.restore()
        paint.color = Color.White
        paint.alpha = (thumbCover * 2.55f).toInt().coerceIn(0, 255) / 255f
        canvas.drawCircle(Offset(x, y), radius, paint)
        paint.alpha = 1f
    }

    private fun createLayerB(radius: Float): ImageBitmap {
        val diameter = radius * 2f
        val size = ceil(diameter + 24f).toInt().coerceAtLeast(1)
        val bitmap = ImageBitmap(size, size)
        val bitmapCanvas = Canvas(bitmap)
        val bounds = Rect(12f, 12f, diameter + 12f, diameter + 12f)
        val fill = Paint().apply {
            isAntiAlias = true
            color = Color(0x77FFFFFF)
        }
        bitmapCanvas.drawRoundRect(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            radiusX = radius,
            radiusY = radius,
            paint = fill,
        )
        val outline = Path().apply { addOval(bounds, Path.Direction.CounterClockwise) }
        bitmapCanvas.clipPath(outline)
        bitmapCanvas.drawSliderInnerShadowStroke(
            bounds = bounds,
            strokeWidth = 10f,
            blurRadius = 5f,
            dy = 0.4f,
            shadowColor = Color.White,
        )
        return bitmap
    }

    private companion object {
        val LayerAColors = listOf(Color(0xE6FFFFFF), Color(0xE6FFFFFF), Color(0x8CFFFFFF))
        val LayerAStops = listOf(0f, 0.2f, 1f)
        val GlitterColors = listOf(Color(0x00FFFFFF), Color(0xA0FFFFFF))
    }
}
