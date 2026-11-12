// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.segmentbutton

import android.graphics.BlendMode
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingMaterial
import io.github.suqi8.coui.kmp.kit.text.couiAdaptFontSize
import io.github.suqi8.coui.kmp.kit.text.couiTextStyle
import kotlin.math.roundToInt

internal object CouiSegmentedControlDefaults {
    val height = 40.dp
    val smallHeight = 32.dp
    internal val padding = 4.dp
    internal val smallPadding = 2.dp
    internal val minWidth = 52.dp
    internal val labelPadding = 12.dp
    internal val switchDistance = 5.dp

    /** Plain container fill when backdrop sampling is off, preserving the native alpha. */
    fun containerColor(dark: Boolean): Color = Color(if (dark) 0x1FFFFFFF else 0x0F000000)

    /** Selected indicator fill shared by plain fill and transparent blur material modes. */
    fun indicatorColor(dark: Boolean): Color = Color(if (dark) 0x33FFFFFF else 0xB2FFFFFF)

    internal fun spotlightNoise(dark: Boolean): Float = if (dark) .1f else .3f
    internal fun spotlightDither(dark: Boolean): Float = if (dark) 0f else 1f

    /** `TYPE_CONTENT_SEGMENT_BUTTON` and `CAPSULE_6` material recipe, excluding the indicator. */
    @RequiresApi(29)
    fun material(dark: Boolean) = CouiFloatingMaterial(
        firstColor = Color(if (dark) 0x7F7F7F7F else 0x7F333333),
        firstBlendMode = if (dark) BlendMode.OVERLAY else BlendMode.COLOR_DODGE,
        secondColor = Color(if (dark) 0x99262626 else 0x99EFEFEF),
        secondBlendMode = BlendMode.LUMINOSITY,
        blurRadius = 25f,
        strokeType = 1,
        edgeAlpha = if (dark) .8f else .6f,
        innerGlowAlpha = if (dark) .05f else .15f,
        innerGlowScale = if (dark) 1f else 1.15f,
    )

    /** Native `couiTextButtonM` metrics without borrowing button or alert text sizes. */
    @Composable
    fun textStyle(): TextStyle {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        return remember(configuration, density) {
            val typeface = if (Build.VERSION.SDK_INT >= 28) {
                Typeface.create(Typeface.create("sans-serif-medium", Typeface.NORMAL), 500, false)
            } else {
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            val pixels = couiAdaptFontSize(with(density) { 14.sp.toPx() }.roundToInt().toFloat(), configuration, 1.35f)
            couiTextStyle(
                typeface,
                500,
                pixels,
                if (Build.VERSION.SDK_INT >= 35) 1.2245f else 1.1f,
                includeFontPadding = false,
                density = density,
            ).copy(letterSpacing = 0.sp)
        }
    }
}
