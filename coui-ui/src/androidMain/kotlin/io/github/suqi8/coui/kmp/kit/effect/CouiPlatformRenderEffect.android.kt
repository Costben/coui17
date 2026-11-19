// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import android.annotation.SuppressLint
import android.graphics.BlendModeColorFilter
import android.graphics.Shader
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.toArgb
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asAndroidRuntimeShader
import android.graphics.BlendMode as AndroidBlendMode
import android.graphics.RenderEffect as AndroidRenderEffect

@ChecksSdkIntAtLeast(api = 34)
internal actual fun isKitSurfaceSupported(): Boolean = Build.VERSION.SDK_INT >= 34

@SuppressLint("ObsoleteSdkInt")
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
internal actual fun isKitRenderEffectSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@ChecksSdkIntAtLeast(api = 34)
internal actual fun isKitSearchBarSupported(): Boolean = Build.VERSION.SDK_INT >= 34

internal actual val isAgslEs3Supported: Boolean = true

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal actual fun RuntimeShader.asComposeRenderEffect(inputUniformName: String): RenderEffect = AndroidRenderEffect.createRuntimeShaderEffect(
    asAndroidRuntimeShader(),
    inputUniformName,
).asComposeRenderEffect()

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun createBlurRenderEffect(
    radiusX: Float,
    radiusY: Float,
    edgeTreatment: TileMode,
): RenderEffect = AndroidRenderEffect.createBlurEffect(
    radiusX,
    radiusY,
    edgeTreatment.toAndroidTileMode(),
).asComposeRenderEffect()

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun createChainRenderEffect(outer: RenderEffect, inner: RenderEffect): RenderEffect = AndroidRenderEffect.createChainEffect(
    outer.asAndroidRenderEffect(),
    inner.asAndroidRenderEffect(),
).asComposeRenderEffect()

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun createOffsetRenderEffect(offsetX: Float, offsetY: Float): RenderEffect = AndroidRenderEffect.createOffsetEffect(offsetX, offsetY).asComposeRenderEffect()

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun createBlendModeRenderEffect(
    dst: RenderEffect,
    src: RenderEffect,
    blendMode: BlendMode,
): RenderEffect = AndroidRenderEffect.createBlendModeEffect(
    dst.asAndroidRenderEffect(),
    src.asAndroidRenderEffect(),
    blendMode.toAndroidBlendMode(),
).asComposeRenderEffect()

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun createBlendModeColorFilterRenderEffect(
    color: Color,
    blendMode: BlendMode,
    renderEffect: RenderEffect,
): RenderEffect = AndroidRenderEffect.createColorFilterEffect(
    BlendModeColorFilter(color.toArgb(), blendMode.toAndroidBlendMode()),
    renderEffect.asAndroidRenderEffect(),
).asComposeRenderEffect()

internal actual typealias PlatformRenderEffect = AndroidRenderEffect

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun PlatformRenderEffect.asComposePlatformRenderEffect(): RenderEffect = asComposeRenderEffect()

@RequiresApi(Build.VERSION_CODES.S)
internal actual fun RenderEffect.asPlatformRenderEffect(): PlatformRenderEffect = asAndroidRenderEffect()

@RequiresApi(Build.VERSION_CODES.S)
private fun TileMode.toAndroidTileMode(): Shader.TileMode = when (this) {
    TileMode.Clamp -> Shader.TileMode.CLAMP
    TileMode.Repeated -> Shader.TileMode.REPEAT
    TileMode.Mirror -> Shader.TileMode.MIRROR
    TileMode.Decal -> Shader.TileMode.DECAL
    else -> Shader.TileMode.CLAMP
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun BlendMode.toAndroidBlendMode(): AndroidBlendMode = when (this) {
    BlendMode.Clear -> AndroidBlendMode.CLEAR
    BlendMode.Src -> AndroidBlendMode.SRC
    BlendMode.Dst -> AndroidBlendMode.DST
    BlendMode.SrcOver -> AndroidBlendMode.SRC_OVER
    BlendMode.DstOver -> AndroidBlendMode.DST_OVER
    BlendMode.SrcIn -> AndroidBlendMode.SRC_IN
    BlendMode.DstIn -> AndroidBlendMode.DST_IN
    BlendMode.SrcOut -> AndroidBlendMode.SRC_OUT
    BlendMode.DstOut -> AndroidBlendMode.DST_OUT
    BlendMode.SrcAtop -> AndroidBlendMode.SRC_ATOP
    BlendMode.DstAtop -> AndroidBlendMode.DST_ATOP
    BlendMode.Xor -> AndroidBlendMode.XOR
    BlendMode.Plus -> AndroidBlendMode.PLUS
    BlendMode.Modulate -> AndroidBlendMode.MODULATE
    BlendMode.Screen -> AndroidBlendMode.SCREEN
    BlendMode.Overlay -> AndroidBlendMode.OVERLAY
    BlendMode.Darken -> AndroidBlendMode.DARKEN
    BlendMode.Lighten -> AndroidBlendMode.LIGHTEN
    BlendMode.ColorDodge -> AndroidBlendMode.COLOR_DODGE
    BlendMode.ColorBurn -> AndroidBlendMode.COLOR_BURN
    BlendMode.Hardlight -> AndroidBlendMode.HARD_LIGHT
    BlendMode.Softlight -> AndroidBlendMode.SOFT_LIGHT
    BlendMode.Difference -> AndroidBlendMode.DIFFERENCE
    BlendMode.Exclusion -> AndroidBlendMode.EXCLUSION
    BlendMode.Multiply -> AndroidBlendMode.MULTIPLY
    BlendMode.Hue -> AndroidBlendMode.HUE
    BlendMode.Saturation -> AndroidBlendMode.SATURATION
    BlendMode.Color -> AndroidBlendMode.COLOR
    BlendMode.Luminosity -> AndroidBlendMode.LUMINOSITY
    else -> AndroidBlendMode.SRC_OVER
}
