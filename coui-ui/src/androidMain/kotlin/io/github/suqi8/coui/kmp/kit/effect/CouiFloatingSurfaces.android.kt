// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import android.graphics.BlendMode as AndroidBlendMode

/** Legacy `android.graphics.BlendMode` factory preserved for unmigrated `androidMain` callers. */
@RequiresApi(Build.VERSION_CODES.Q)
internal fun CouiFloatingMaterial(
    firstColor: Color,
    firstBlendMode: AndroidBlendMode,
    secondColor: Color,
    edgeAlpha: Float,
    innerGlowAlpha: Float,
    secondBlendMode: AndroidBlendMode = AndroidBlendMode.LUMINOSITY,
    blurRadius: Float = 25f,
    strokeType: Int = 1,
    index: Int = 2,
    extension: Int = 0,
    lightColor: Color = Color.White,
    themeTint: Color = Color.Transparent,
    edgeWidthDp: Float = 2.5f,
    innerGlowScale: Float = 1f,
): CouiFloatingMaterial = CouiFloatingMaterial(
    firstColor = firstColor,
    firstBlendMode = firstBlendMode.toComposeBlendMode(),
    secondColor = secondColor,
    edgeAlpha = edgeAlpha,
    innerGlowAlpha = innerGlowAlpha,
    secondBlendMode = secondBlendMode.toComposeBlendMode(),
    blurRadius = blurRadius,
    strokeType = strokeType,
    index = index,
    extension = extension,
    lightColor = lightColor,
    themeTint = themeTint,
    edgeWidthDp = edgeWidthDp,
    innerGlowScale = innerGlowScale,
)

@RequiresApi(Build.VERSION_CODES.Q)
private fun AndroidBlendMode.toComposeBlendMode(): BlendMode = when (this) {
    AndroidBlendMode.CLEAR -> BlendMode.Clear
    AndroidBlendMode.SRC -> BlendMode.Src
    AndroidBlendMode.DST -> BlendMode.Dst
    AndroidBlendMode.SRC_OVER -> BlendMode.SrcOver
    AndroidBlendMode.DST_OVER -> BlendMode.DstOver
    AndroidBlendMode.SRC_IN -> BlendMode.SrcIn
    AndroidBlendMode.DST_IN -> BlendMode.DstIn
    AndroidBlendMode.SRC_OUT -> BlendMode.SrcOut
    AndroidBlendMode.DST_OUT -> BlendMode.DstOut
    AndroidBlendMode.SRC_ATOP -> BlendMode.SrcAtop
    AndroidBlendMode.DST_ATOP -> BlendMode.DstAtop
    AndroidBlendMode.XOR -> BlendMode.Xor
    AndroidBlendMode.PLUS -> BlendMode.Plus
    AndroidBlendMode.MODULATE -> BlendMode.Modulate
    AndroidBlendMode.SCREEN -> BlendMode.Screen
    AndroidBlendMode.OVERLAY -> BlendMode.Overlay
    AndroidBlendMode.DARKEN -> BlendMode.Darken
    AndroidBlendMode.LIGHTEN -> BlendMode.Lighten
    AndroidBlendMode.COLOR_DODGE -> BlendMode.ColorDodge
    AndroidBlendMode.COLOR_BURN -> BlendMode.ColorBurn
    AndroidBlendMode.HARD_LIGHT -> BlendMode.Hardlight
    AndroidBlendMode.SOFT_LIGHT -> BlendMode.Softlight
    AndroidBlendMode.DIFFERENCE -> BlendMode.Difference
    AndroidBlendMode.EXCLUSION -> BlendMode.Exclusion
    AndroidBlendMode.MULTIPLY -> BlendMode.Multiply
    AndroidBlendMode.HUE -> BlendMode.Hue
    AndroidBlendMode.SATURATION -> BlendMode.Saturation
    AndroidBlendMode.COLOR -> BlendMode.Color
    AndroidBlendMode.LUMINOSITY -> BlendMode.Luminosity
}
