// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.text

import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

private fun createAndroidTypeface(familyName: String, weight: Int): Typeface {
    val base = Typeface.create(familyName, Typeface.NORMAL)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Typeface.create(base, weight, false)
    } else {
        val style = if (weight >= 600) Typeface.BOLD else Typeface.NORMAL
        Typeface.create(base, style)
    }
}

internal actual fun couiFontMetrics(familyName: String, weight: Int, pixels: Float): CouiFontMetrics {
    val paint = Paint().apply {
        typeface = createAndroidTypeface(familyName, weight)
        textSize = pixels
    }
    val metricsInt = paint.fontMetricsInt
    return CouiFontMetrics(
        ascent = metricsInt.ascent,
        descent = metricsInt.descent,
        descentPx = paint.fontMetrics.descent,
    )
}

internal actual fun couiPlatformFontFamily(familyName: String, weight: Int): FontFamily = FontFamily(createAndroidTypeface(familyName, weight))

internal actual fun couiPlatformTextStyle(includeFontPadding: Boolean): PlatformTextStyle? = PlatformTextStyle(includeFontPadding = includeFontPadding)

/** Shared TextView font metrics and line-height algorithm for Android callers holding a [Typeface]. */
internal fun couiTextStyle(
    typeface: Typeface,
    weight: Int,
    pixels: Float,
    lineSpacing: Float,
    includeFontPadding: Boolean,
    density: Density,
): TextStyle {
    val metrics = Paint().apply {
        this.typeface = typeface
        textSize = pixels
    }.fontMetricsInt
    return buildCouiTextStyle(
        ascent = metrics.ascent,
        descent = metrics.descent,
        fontFamily = FontFamily(typeface),
        weight = weight,
        pixels = pixels,
        lineSpacing = lineSpacing,
        includeFontPadding = includeFontPadding,
        density = density,
    )
}

/** COUIChangeTextUtil.adaptFontSize; pixels has already been SP-converted and rounded, keeping post-unscale rounding. */
internal fun couiAdaptFontSize(pixels: Float, configuration: Configuration, maxScale: Float): Float {
    val scale = if (configuration.densityDpi == 300 || configuration.densityDpi == 296 ||
        configuration.smallestScreenWidthDp <= 210
    ) {
        1f
    } else {
        configuration.fontScale
    }
    return couiAdaptFontSize(pixels, scale, maxScale)
}
