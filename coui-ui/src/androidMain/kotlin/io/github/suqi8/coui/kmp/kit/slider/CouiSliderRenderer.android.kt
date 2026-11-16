// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.slider

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

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
    val fwPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = thumbColor.toArgb()
        if (drawShadow) {
            setShadowLayer(4f * density, 0f, 2f * density, shadowColor.toArgb())
        }
    }
    nativeCanvas.drawCircle(x, y, radius, fwPaint)
}

internal actual fun Canvas.drawSliderInnerShadowStroke(
    bounds: Rect,
    strokeWidth: Float,
    blurRadius: Float,
    dy: Float,
    shadowColor: Color,
) {
    val fwPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        color = android.graphics.Color.TRANSPARENT
        setShadowLayer(blurRadius, 0f, dy, shadowColor.toArgb())
    }
    nativeCanvas.drawOval(bounds.left, bounds.top, bounds.right, bounds.bottom, fwPaint)
}

internal actual fun ImageBitmap.releaseResources() {
    asAndroidBitmap().recycle()
}

/** Compatibility overload for [CouiKitSlider] while it remains in `androidMain`. */
internal fun CouiSliderRenderer.draw(
    canvas: android.graphics.Canvas,
    height: Float,
    motion: CouiSliderMotion,
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
    draw(
        canvas = Canvas(canvas),
        height = height,
        length = motion.length,
        visualFraction = motion.visualFraction,
        heightFraction = motion.heightFraction,
        thumbRadiusDp = motion.thumbRadius.value,
        texture = motion.texture.value,
        thumbCover = motion.thumbCover.value,
        glitter = motion.glitter.value,
        colors = colors,
        enabled = enabled,
        rtl = rtl,
        tickCount = tickCount,
        keyPointFractions = keyPointFractions,
        secondaryProgressFraction = secondaryProgressFraction,
        showProgress = showProgress,
        showThumb = showThumb,
        startFromMiddle = startFromMiddle,
        customTrackHeight = customTrackHeight,
    )
}

/** Compatibility overload for [CouiKitSlider] while it remains in `androidMain`. */
internal fun CouiSliderRenderer.clipTrack(canvas: android.graphics.Canvas) {
    clipTrack(Canvas(canvas))
}
