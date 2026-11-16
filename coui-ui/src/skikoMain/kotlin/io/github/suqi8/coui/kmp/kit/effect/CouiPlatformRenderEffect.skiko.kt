// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.skiaImageFilter
import androidx.compose.ui.graphics.toArgb
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asSkikoRuntimeShader
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.BlendMode as SkiaBlendMode

internal actual fun isKitSurfaceSupported(): Boolean = true

internal actual fun isKitRenderEffectSupported(): Boolean = true

internal actual val isAgslEs3Supported: Boolean = false

internal actual fun RuntimeShader.asComposeRenderEffect(inputUniformName: String): RenderEffect = ImageFilter.makeRuntimeShader(
    asSkikoRuntimeShader(),
    inputUniformName,
    null,
).asComposeRenderEffect()

internal actual fun createBlurRenderEffect(
    radiusX: Float,
    radiusY: Float,
    edgeTreatment: TileMode,
): RenderEffect {
    val sigmaX = if (radiusX > 0f) radiusX * 0.57735f + 0.5f else 0f
    val sigmaY = if (radiusY > 0f) radiusY * 0.57735f + 0.5f else 0f
    return if (sigmaX <= 0f && sigmaY <= 0f) {
        ImageFilter.makeOffset(0f, 0f, null, null).asComposeRenderEffect()
    } else {
        ImageFilter.makeBlur(sigmaX, sigmaY, edgeTreatment.toSkiaFilterTileMode(), null, null).asComposeRenderEffect()
    }
}

internal actual fun createChainRenderEffect(outer: RenderEffect, inner: RenderEffect): RenderEffect = ImageFilter.makeCompose(
    outer.skiaImageFilter,
    inner.skiaImageFilter,
).asComposeRenderEffect()

internal actual fun createOffsetRenderEffect(offsetX: Float, offsetY: Float): RenderEffect = ImageFilter.makeOffset(offsetX, offsetY, null, null).asComposeRenderEffect()

internal actual fun createBlendModeRenderEffect(
    dst: RenderEffect,
    src: RenderEffect,
    blendMode: BlendMode,
): RenderEffect = ImageFilter.makeBlend(
    blendMode.toSkiaBlendMode(),
    dst.skiaImageFilter,
    src.skiaImageFilter,
    null,
).asComposeRenderEffect()

internal actual fun createBlendModeColorFilterRenderEffect(
    color: Color,
    blendMode: BlendMode,
    renderEffect: RenderEffect,
): RenderEffect = ImageFilter.makeColorFilter(
    ColorFilter.makeBlend(color.toArgb(), blendMode.toSkiaBlendMode()),
    renderEffect.skiaImageFilter,
    null,
).asComposeRenderEffect()

internal actual typealias PlatformRenderEffect = ImageFilter

internal actual fun PlatformRenderEffect.asComposePlatformRenderEffect(): RenderEffect = asComposeRenderEffect()

internal actual fun RenderEffect.asPlatformRenderEffect(): PlatformRenderEffect = skiaImageFilter

private fun TileMode.toSkiaFilterTileMode(): FilterTileMode = when (this) {
    TileMode.Clamp -> FilterTileMode.CLAMP
    TileMode.Repeated -> FilterTileMode.REPEAT
    TileMode.Mirror -> FilterTileMode.MIRROR
    TileMode.Decal -> FilterTileMode.DECAL
    else -> FilterTileMode.CLAMP
}

private fun BlendMode.toSkiaBlendMode(): SkiaBlendMode = when (this) {
    BlendMode.Clear -> SkiaBlendMode.CLEAR
    BlendMode.Src -> SkiaBlendMode.SRC
    BlendMode.Dst -> SkiaBlendMode.DST
    BlendMode.SrcOver -> SkiaBlendMode.SRC_OVER
    BlendMode.DstOver -> SkiaBlendMode.DST_OVER
    BlendMode.SrcIn -> SkiaBlendMode.SRC_IN
    BlendMode.DstIn -> SkiaBlendMode.DST_IN
    BlendMode.SrcOut -> SkiaBlendMode.SRC_OUT
    BlendMode.DstOut -> SkiaBlendMode.DST_OUT
    BlendMode.SrcAtop -> SkiaBlendMode.SRC_ATOP
    BlendMode.DstAtop -> SkiaBlendMode.DST_ATOP
    BlendMode.Xor -> SkiaBlendMode.XOR
    BlendMode.Plus -> SkiaBlendMode.PLUS
    BlendMode.Modulate -> SkiaBlendMode.MODULATE
    BlendMode.Screen -> SkiaBlendMode.SCREEN
    BlendMode.Overlay -> SkiaBlendMode.OVERLAY
    BlendMode.Darken -> SkiaBlendMode.DARKEN
    BlendMode.Lighten -> SkiaBlendMode.LIGHTEN
    BlendMode.ColorDodge -> SkiaBlendMode.COLOR_DODGE
    BlendMode.ColorBurn -> SkiaBlendMode.COLOR_BURN
    BlendMode.Hardlight -> SkiaBlendMode.HARD_LIGHT
    BlendMode.Softlight -> SkiaBlendMode.SOFT_LIGHT
    BlendMode.Difference -> SkiaBlendMode.DIFFERENCE
    BlendMode.Exclusion -> SkiaBlendMode.EXCLUSION
    BlendMode.Multiply -> SkiaBlendMode.MULTIPLY
    BlendMode.Hue -> SkiaBlendMode.HUE
    BlendMode.Saturation -> SkiaBlendMode.SATURATION
    BlendMode.Color -> SkiaBlendMode.COLOR
    BlendMode.Luminosity -> SkiaBlendMode.LUMINOSITY
    else -> SkiaBlendMode.SRC_OVER
}
