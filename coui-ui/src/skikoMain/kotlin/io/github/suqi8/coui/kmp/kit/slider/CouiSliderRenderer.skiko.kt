// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.slider

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Color as SkiaColor
import org.jetbrains.skia.Paint as SkiaPaint
import org.jetbrains.skia.Rect as SkiaRect

private fun blurRadiusToSigma(radius: Float): Float = if (radius > 0f) 0.57735f * radius + 0.5f else 0f

internal actual fun Canvas.drawSliderThumbWithShadow(
    x: Float,
    y: Float,
    radius: Float,
    density: Float,
    thumbColor: Color,
    shadowColor: Color,
    drawShadow: Boolean,
    paint: Paint,
) {
    if (drawShadow) {
        val sigma = blurRadiusToSigma(4f * density)
        val shadowPaint = SkiaPaint().apply {
            isAntiAlias = true
            color = SkiaColor.BLACK
            imageFilter = ImageFilter.makeDropShadowOnly(
                dx = 0f,
                dy = 2f * density,
                sigmaX = sigma,
                sigmaY = sigma,
                color = shadowColor.toArgb(),
            )
        }
        skiaCanvas.drawCircle(x, y, radius, shadowPaint)
        shadowPaint.close()
    }
    paint.color = thumbColor
    drawCircle(Offset(x, y), radius, paint)
}

internal actual fun Canvas.drawSliderInnerShadowStroke(
    bounds: Rect,
    strokeWidth: Float,
    blurRadius: Float,
    dy: Float,
    shadowColor: Color,
) {
    val sigma = blurRadiusToSigma(blurRadius)
    val strokePaint = SkiaPaint().apply {
        isAntiAlias = true
        mode = PaintMode.STROKE
        this.strokeWidth = strokeWidth
        color = SkiaColor.WHITE
        imageFilter = ImageFilter.makeDropShadowOnly(
            dx = 0f,
            dy = dy,
            sigmaX = sigma,
            sigmaY = sigma,
            color = shadowColor.toArgb(),
        )
    }
    skiaCanvas.drawOval(SkiaRect.makeLTRB(bounds.left, bounds.top, bounds.right, bounds.bottom), strokePaint)
    strokePaint.close()
}

internal actual fun ImageBitmap.releaseResources() {
    // Skia-backed ImageBitmap resources are managed by Skia/GC.
}
