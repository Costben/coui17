// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.text

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontSlant
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.FontWidth
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val DEFAULT_ASCENT_RATIO = -0.9277344f
private const val DEFAULT_DESCENT_RATIO = 0.24414062f

internal actual fun couiFontMetrics(familyName: String, weight: Int, pixels: Float): CouiFontMetrics {
    val style = FontStyle(weight, FontWidth.NORMAL, FontSlant.UPRIGHT)
    val mgr = FontMgr.default
    val skiaFamily = when (familyName) {
        "sans-serif-medium", "sans-serif-semibold" -> "sans-serif"
        else -> familyName
    }
    val typeface = mgr.matchFamilyStyle(familyName, style)
        ?: mgr.matchFamilyStyle(skiaFamily, style)
        ?: mgr.matchFamilyStyle(null, style)
    val font = Font(typeface, pixels)
    val fm = font.metrics
    val rawAscent = fm.ascent
    val rawDescent = fm.descent
    if (!rawAscent.isFinite() || !rawDescent.isFinite() || rawDescent - rawAscent <= 0f) {
        val fallbackAscent = pixels * DEFAULT_ASCENT_RATIO
        val fallbackDescent = pixels * DEFAULT_DESCENT_RATIO
        return CouiFontMetrics(
            ascent = floor(fallbackAscent).roundToInt(),
            descent = ceil(fallbackDescent).roundToInt(),
            descentPx = fallbackDescent,
        )
    }
    return CouiFontMetrics(
        ascent = floor(rawAscent).roundToInt(),
        descent = ceil(rawDescent).roundToInt(),
        descentPx = rawDescent,
    )
}

internal actual fun couiPlatformFontFamily(familyName: String, weight: Int): FontFamily = FontFamily.SansSerif

internal actual fun couiPlatformTextStyle(includeFontPadding: Boolean): PlatformTextStyle? = null
