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
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.BlendMode as SkiaBlendMode

internal actual fun isKitSurfaceSupported(): Boolean = true

internal actual fun isKitRenderEffectSupported(): Boolean = true

// Skiko/Metal supports floating surfaces, metaball fusion, and CADisplayLink VSync springs.
internal actual fun isKitSearchBarSupported(): Boolean = true

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
    if (sigmaX <= 0f && sigmaY <= 0f) {
        return ImageFilter.makeOffset(0f, 0f, null, null).asComposeRenderEffect()
    }
    // Skia's ImageFilter.makeBlur bakes ceil(3 * sigma) into its generated Metal MSL shader, so
    // an animating blur radius (e.g. TopAppBarSearch expand/collapse 0..30px) compiles 20+ separate
    // Metal PSOs mid-animation. Route Decal blurs through one unified separable RuntimeShader so
    // every radius shares a single Metal pipeline.
    if (edgeTreatment == TileMode.Decal) {
        return cachedUnifiedDecalBlurEffect(sigmaX, sigmaY)
    }
    // Large Mirror blurs (e.g. toolbar glass sampling at 3x Retina) run ~8x faster at 0.5x scale
    // with visually indistinguishable Gaussian output and identical coordinate space.
    if (edgeTreatment == TileMode.Mirror && sigmaX >= 8f && sigmaY >= 8f) {
        val down = ImageFilter.makeMatrixTransform(
            Matrix33.makeScale(0.5f, 0.5f),
            SamplingMode.LINEAR,
            null,
        )
        val blurred = ImageFilter.makeBlur(
            sigmaX * 0.5f,
            sigmaY * 0.5f,
            FilterTileMode.MIRROR,
            down,
            null,
        )
        return ImageFilter.makeMatrixTransform(
            Matrix33.makeScale(2f, 2f),
            SamplingMode.LINEAR,
            blurred,
        ).asComposeRenderEffect()
    }
    return ImageFilter.makeBlur(sigmaX, sigmaY, edgeTreatment.toSkiaFilterTileMode(), null, null).asComposeRenderEffect()
}

private val unifiedDecalBlurCache = HashMap<Long, RenderEffect>(32)

private fun cachedUnifiedDecalBlurEffect(sigmaX: Float, sigmaY: Float): RenderEffect {
    val qx = (sigmaX * 4f).toInt().coerceAtLeast(0).toLong()
    val qy = (sigmaY * 4f).toInt().coerceAtLeast(0).toLong()
    val key = (qx shl 32) or (qy and 0xFFFFFFFFL)
    unifiedDecalBlurCache[key]?.let { return it }
    val created = createUnifiedDecalBlurEffect(qx / 4f, qy / 4f)
    if (unifiedDecalBlurCache.size >= 64) unifiedDecalBlurCache.clear()
    unifiedDecalBlurCache[key] = created
    return created
}

private const val UNIFIED_DECAL_BLUR_MAX_TAPS = 5

internal const val SKIKO_UNIFIED_DECAL_BLUR_SHADER = """
    uniform shader child;
    uniform float2 in_step;
    uniform float in_sigma;
    uniform float in_taps;

    half4 main(float2 xy) {
        if (in_sigma <= 0.05 || in_taps < 1.0) {
            return child.eval(xy);
        }
        float inv2s2 = -0.5 / (in_sigma * in_sigma);
        half4 sum = child.eval(xy);
        float weightSum = 1.0;
        for (int i = 1; i <= $UNIFIED_DECAL_BLUR_MAX_TAPS; i++) {
            float fi = float(i);
            if (fi > in_taps) break;
            float2 offset = in_step * fi;
            float dist = length(offset);
            float w = exp(dist * dist * inv2s2);
            sum += (child.eval(xy + offset) + child.eval(xy - offset)) * half(w);
            weightSum += 2.0 * w;
        }
        return sum / half(weightSum);
    }
"""

private fun createUnifiedDecalBlurEffect(sigmaX: Float, sigmaY: Float): RenderEffect {
    var filter: ImageFilter? = null
    if (sigmaX > 0.05f) {
        val reach = kotlin.math.ceil(sigmaX * 2.2f).coerceAtLeast(1f)
        val stride = kotlin.math.max(1f, reach / UNIFIED_DECAL_BLUR_MAX_TAPS)
        val taps = kotlin.math.ceil(reach / stride).coerceIn(1f, UNIFIED_DECAL_BLUR_MAX_TAPS.toFloat())
        val hShader = RuntimeShader(SKIKO_UNIFIED_DECAL_BLUR_SHADER).apply {
            setFloatUniform("in_step", stride, 0f)
            setFloatUniform("in_sigma", sigmaX)
            setFloatUniform("in_taps", taps)
        }
        filter = ImageFilter.makeRuntimeShader(hShader.asSkikoRuntimeShader(), "child", null)
    }
    if (sigmaY > 0.05f) {
        val reach = kotlin.math.ceil(sigmaY * 2.2f).coerceAtLeast(1f)
        val stride = kotlin.math.max(1f, reach / UNIFIED_DECAL_BLUR_MAX_TAPS)
        val taps = kotlin.math.ceil(reach / stride).coerceIn(1f, UNIFIED_DECAL_BLUR_MAX_TAPS.toFloat())
        val vShader = RuntimeShader(SKIKO_UNIFIED_DECAL_BLUR_SHADER).apply {
            setFloatUniform("in_step", 0f, stride)
            setFloatUniform("in_sigma", sigmaY)
            setFloatUniform("in_taps", taps)
        }
        val vFilter = ImageFilter.makeRuntimeShader(vShader.asSkikoRuntimeShader(), "child", null)
        filter = if (filter != null) ImageFilter.makeCompose(vFilter, filter) else vFilter
    }
    return (filter ?: ImageFilter.makeOffset(0f, 0f, null, null)).asComposeRenderEffect()
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
