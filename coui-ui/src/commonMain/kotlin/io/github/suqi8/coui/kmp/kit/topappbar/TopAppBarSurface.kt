// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdropEffectScope
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.couiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.drawCouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.rememberCouiBackdrop
import io.github.suqi8.coui.kmp.kit.effect.CouiCoverageBlurEffect
import io.github.suqi8.coui.kmp.kit.effect.createBlendModeRenderEffect
import io.github.suqi8.coui.kmp.kit.effect.createOffsetRenderEffect
import io.github.suqi8.coui.kmp.kit.effect.drawCouiGradientBackdrop
import io.github.suqi8.coui.kmp.kit.effect.isKitRenderEffectSupported
import io.github.suqi8.coui.kmp.kit.effect.isKitRuntimeShaderSupported
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme

internal val LocalCouiToolbarBackdrop = staticCompositionLocalOf<CouiBackdrop?> { null }
internal val LocalCouiToolbarOverlay = staticCompositionLocalOf<SharedTransitionScope?> { null }

private const val FadeStopCount = 65
private val BlurLayers = listOf(
    Triple(2.7.dp, 0.68f, 1f),
    Triple(6.3.dp, 0.44f, 0.82f),
    Triple(CouiTopAppBarDefaults.referenceBlurRadius, 0.28f, 0.68f),
).map { (radius, start, end) ->
    radius to List(FadeStopCount) { index ->
        start + (end - start) * index / (FadeStopCount - 1f)
    }
}

private object TopAppBarSurfaceGradientTokens {
    // Three mask layers share this opacity curve; colors and stop positions do not vary with bar height and are built once.
    val FadeColors = List(FadeStopCount) { index ->
        val progress = index / (FadeStopCount - 1f)
        val smoothProgress = progress * progress * (3f - 2f * progress)
        Color(0f, 0f, 0f, 1f - smoothProgress, ColorSpaces.ExtendedSrgb)
    }
}

/**
 * Background starts at the top of the app bar and extends downward without changing layout or touch height.
 * When [gradientSpeed] is `null`, the fixed progressive fade is used; positive values select the full-height power curve.
 * When [gradientColor] is unspecified, light mode uses white and dark mode uses black; [Color.Transparent] disables the scrim,
 * and an explicit alpha multiplies the preset opacity.
 * [topExtension] is read during measurement so the background extends upward and translates without altering content bounds or touch area.
 */
@Composable
internal fun TopAppBarSurface(
    modifier: Modifier = Modifier,
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
    fadeHeight: Dp = CouiTopAppBarDefaults.fadeHeight,
    referenceBlurRadius: Dp = CouiTopAppBarDefaults.referenceBlurRadius,
    bottomExtension: Dp = CouiTopAppBarDefaults.bottomExtension,
    backgroundAlpha: Float = 1f,
    gradientSpeed: Float? = null,
    gradientColor: Color = Color.Unspecified,
    dark: Boolean = CouiTheme.isDark,
    topExtension: () -> Dp = { 0.dp },
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current.density
    val destination = rememberCouiBackdrop(parent = backdrop)
    val backgroundModifier = if (backdrop != null) {
        if (isKitRuntimeShaderSupported()) {
            Modifier.drawCouiGradientBackdrop(
                backdrop = backdrop,
                fadeHeightPx = fadeHeight.value * density,
                referenceBlurRadiusPx = referenceBlurRadius.value * density,
                gradientSpeed = gradientSpeed,
            )
        } else if (isKitRenderEffectSupported()) {
            val blurEffects = remember(density) {
                BlurLayers.map { (radius, _) -> CouiCoverageBlurEffect(radius.value * density) }
            }
            Modifier.drawCouiBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                effects = { progressiveBlur(blurEffects, fadeHeight) },
            )
        } else {
            Modifier
        }
    } else {
        Modifier.background(CouiTheme.colorSchemeFor(dark).background)
    }
    val resolvedGradientColor = gradientColor.takeOrElse { if (dark) Color.Black else Color.White }
    val gradient = remember(resolvedGradientColor, dark) {
        // Extended color encoding preserves alpha precision instead of quantizing each stop to 8 bits.
        val color = resolvedGradientColor.convert(ColorSpaces.ExtendedSrgb)
        Brush.verticalGradient(
            List(FadeStopCount) { index ->
                color.copy(alpha = color.alpha * topBarGradientAlpha(index / (FadeStopCount - 1f), dark))
            },
        )
    }
    SharedTransitionScope { overlayModifier ->
        Layout(
            modifier = modifier.then(overlayModifier),
            content = {
                Box(Modifier.couiBackdrop(destination)) {
                    Box(Modifier.matchParentSize().graphicsLayer { alpha = backgroundAlpha.coerceIn(0f, 1f) }) {
                        Spacer(Modifier.matchParentSize().then(backgroundModifier))
                        if (resolvedGradientColor.alpha > 0f) Spacer(Modifier.matchParentSize().background(gradient))
                    }
                }
                Box {
                    CompositionLocalProvider(
                        LocalCouiToolbarBackdrop provides destination,
                        LocalCouiToolbarOverlay provides this@SharedTransitionScope,
                        content = content,
                    )
                }
            },
        ) { measurables, constraints ->
            val topBar = measurables[1].measure(constraints.copy(minHeight = 0))
            val topExtensionPx = topExtension().roundToPx()
            val background = measurables[0].measure(
                Constraints.fixed(
                    width = topBar.width,
                    height = topBar.height + topExtensionPx + bottomExtension.roundToPx(),
                ),
            )
            // When collapsed, the fixed-height background shifts upward with the bar so the progressive curve does not stretch to the current bar height.
            layout(topBar.width, topBar.height) {
                background.placeRelative(0, -topExtensionPx)
                topBar.placeRelative(0, 0)
            }
        }
    }
}

private fun topBarGradientAlpha(position: Float, dark: Boolean): Float {
    if (dark) return .4f * (1f - position * position * (3f - 2f * position))
    // Keep light-mode .6/.7 stops; two Hermite segments share the join slope and settle at zero slope at the bottom.
    val first = position <= .6f
    val span = if (first) .6f else .4f
    val t = if (first) position / span else (position - .6f) / span
    val start = if (first) 1f else .7f
    val end = if (first) .7f else 0f
    val startSlope = if (first) -.5f else -.8f
    val endSlope = if (first) -.8f else 0f
    val smooth = t * t * (3f - 2f * t)
    return (
        start * (1f - smooth) + end * smooth +
            span * t * (1f - t) * (startSlope * (1f - t) - endSlope * t)
        ).coerceIn(0f, 1f)
}

private fun CouiBackdropEffectScope.progressiveBlur(blurEffects: List<CouiCoverageBlurEffect>, fadeHeight: Dp) {
    if (!size.isSpecified || size.height <= 0f) return

    // CouiBackdrop translates the sampling origin; masks use the same padding to keep platform blur outer margins.
    padding = blurEffects.maxOf { it.padding }
    var combined = createOffsetRenderEffect(0f, 0f)
    for ((index, layer) in BlurLayers.withIndex()) {
        val positions = layer.second
        val blurred = blurEffects[index].composeEffect
        val mask = fadeEffect(positions, fadeHeight)
        val maskedBlur = createBlendModeRenderEffect(blurred, mask, BlendMode.DstIn)
        // Composite onto the opaque sampled base inside the filter rather than emitting three translucent Compose layers.
        combined = createBlendModeRenderEffect(combined, maskedBlur, BlendMode.SrcOver)
    }
    // Preserve sampled content colors without theme background tinting adding extra shading gradients across surfaces.
    renderEffect = combined
}

private fun CouiBackdropEffectScope.fadeEffect(positions: List<Float>, fadeHeight: Dp): RenderEffect {
    // Gradient expands over a fixed length anchored to the bottom edge so collapse translates without compressing the curve.
    // Top coordinate may extend past 0; clamped endpoints extend boundary color without trimming the coordinate range.
    val gradientBottom = padding + size.height
    val gradientTop = gradientBottom - fadeHeight.toPx()
    return createShaderRenderEffect(
        LinearGradientShader(
            from = Offset(0f, gradientTop),
            to = Offset(0f, gradientBottom),
            colors = TopAppBarSurfaceGradientTokens.FadeColors,
            colorStops = positions,
            tileMode = TileMode.Clamp,
        ),
    )
}

internal expect fun createShaderRenderEffect(shader: Shader): RenderEffect
