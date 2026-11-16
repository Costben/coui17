// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.text

// Ported from COUI-Kit (ItosEO/COUI-Kit@8a09da4) coui/src/main/java/com/itos/coui/text/CouiTextMetrics.kt.

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/** Integer and pixel font metrics resolved for a typeface and pixel size. */
internal data class CouiFontMetrics(
    val ascent: Int,
    val descent: Int,
    val descentPx: Float,
)

internal expect fun couiFontMetrics(familyName: String, weight: Int, pixels: Float): CouiFontMetrics

internal expect fun couiPlatformFontFamily(familyName: String, weight: Int): FontFamily

internal expect fun couiPlatformTextStyle(includeFontPadding: Boolean): PlatformTextStyle?

/** Shared TextView font metrics and line-height algorithm; roles, font sources, and presets stay with components. */
internal fun couiTextStyle(
    familyName: String,
    weight: Int,
    pixels: Float,
    lineSpacing: Float,
    includeFontPadding: Boolean,
    density: Density,
): TextStyle {
    val metrics = couiFontMetrics(familyName, weight, pixels)
    return buildCouiTextStyle(
        ascent = metrics.ascent,
        descent = metrics.descent,
        fontFamily = couiPlatformFontFamily(familyName, weight),
        weight = weight,
        pixels = pixels,
        lineSpacing = lineSpacing,
        includeFontPadding = includeFontPadding,
        density = density,
    )
}

internal fun buildCouiTextStyle(
    ascent: Int,
    descent: Int,
    fontFamily: FontFamily,
    weight: Int,
    pixels: Float,
    lineSpacing: Float,
    includeFontPadding: Boolean,
    density: Density,
): TextStyle {
    val base = descent - ascent
    // lineSpacingMultiplier applies to ascent/descent; extra spacing goes below the line and is omitted on the last line.
    val height = base + (base * (lineSpacing - 1f)).roundToInt()
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight(weight),
        fontSynthesis = FontSynthesis.None,
        fontSize = with(density) { pixels.toSp() },
        lineHeight = with(density) { height.toFloat().toSp() },
        platformStyle = couiPlatformTextStyle(includeFontPadding),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Top,
            trim = LineHeightStyle.Trim.LastLineBottom,
            mode = LineHeightStyle.Mode.Minimum,
        ),
    )
}

/** COUIChangeTextUtil.adaptFontSize; pixels has already been SP-converted and rounded, keeping post-unscale rounding. */
internal fun couiAdaptFontSize(pixels: Float, fontScale: Float, maxScale: Float): Float {
    val safeScale = if (fontScale > 0f) fontScale else 1f
    return (pixels / safeScale).roundToInt() * safeScale.coerceAtMost(maxScale)
}

/** [couiAdaptFontSize] convenience overload reading [Density.fontScale]. */
internal fun couiAdaptFontSize(pixels: Float, density: Density, maxScale: Float): Float = couiAdaptFontSize(pixels, density.fontScale, maxScale)
