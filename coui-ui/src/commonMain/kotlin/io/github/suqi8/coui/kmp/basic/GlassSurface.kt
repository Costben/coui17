// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.highlight.Highlight
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The size class of a glass container, selecting the matching edge highlight.
 *
 * ColorOS scales the stroke with the surface: a large sheet gets a thicker, softer halo than a
 * compact chip. The presets themselves live in `Highlight.GlassStroke*`.
 */
enum class GlassContainer {
    /** Compact surfaces: chips, small buttons, item backgrounds. */
    Small,

    /** Default: bars, cards, popups. */
    Medium,

    /** Full-width sheets and dialogs. */
    Large,
}

/** Default values for the ColorOS 17 light-sensing glass surfaces. */
object GlassDefaults {

    /**
     * Blur radius in dp.
     *
     * ColorOS does not publish a bar-radius token; the only radius values it ships are the px radii
     * of the popup and dialog backgrounds (`coui_popup_*`/`coui_dialog_*`, 160–400px), which use the
     * framework's own blur unit rather than `RenderEffect`'s. 25dp matches the value the library's
     * own `BlurredBar` uses for a top bar.
     */
    val BlurRadius = 25f

    /**
     * Alpha of the container color blended over the blurred backdrop.
     *
     * The bar colors (`coui_color_bar` `#FAFAFA`/`#1E1E1E`) are opaque fills used when blur is
     * unavailable; over a blur they are drawn translucent so the backdrop stays readable.
     */
    val ContainerAlpha = 0.8f
}

/**
 * Resolves the edge highlight for a glass container in the current theme.
 *
 * @param container The size class of the surface.
 * @param backdrop The backdrop the surface samples; when `null` the surface is opaque and no
 *   highlight is drawn.
 */
@Composable
fun rememberGlassHighlight(
    container: GlassContainer = GlassContainer.Medium,
    backdrop: Backdrop? = LocalBackdrop.current,
): Highlight? {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    return remember(container, isDark, backdrop) {
        if (backdrop == null) {
            null
        } else {
            when (container) {
                GlassContainer.Small -> if (isDark) Highlight.GlassStrokeSmallDark else Highlight.GlassStrokeSmallLight
                GlassContainer.Medium -> if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
                GlassContainer.Large -> if (isDark) Highlight.GlassStrokeBigDark else Highlight.GlassStrokeBigLight
            }
        }
    }
}

/**
 * The ColorOS 17 light-sensing glass background.
 *
 * Draws [containerColor] over the blurred [backdrop] and lights the rim with [highlight], producing
 * the frosted surface ColorOS uses for bars, popups and sheets. When [backdrop] is `null` — or the
 * platform cannot run runtime shaders, which is everything below Android 13 — the effect is
 * skipped and [fallback] is used instead, so hosts without a backdrop keep their opaque rendering.
 *
 * @param backdrop The content to blur, normally [LocalBackdrop]'s value.
 * @param shape The silhouette of the surface, used to clip the blur.
 * @param containerColor The color laid over the blurred backdrop.
 * @param fallback The modifier used when glass is unavailable, typically the matching
 *   `squircleBackground`.
 * @param blurRadius The blur radius in dp.
 * @param containerAlpha The alpha of [containerColor] over the blur.
 * @param blendColors Overrides the tint layers. When `null` a single [containerColor] layer at
 *   [containerAlpha] is used; pass an explicit list for surfaces whose native recipe tints with more
 *   than one color, such as the popup glass.
 * @param highlight The rim highlight, normally from [rememberGlassHighlight].
 */
@Composable
fun Modifier.couiGlassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    containerColor: Color,
    fallback: Modifier,
    blurRadius: Float = GlassDefaults.BlurRadius,
    containerAlpha: Float = GlassDefaults.ContainerAlpha,
    blendColors: List<BlendColorEntry>? = null,
    highlight: Highlight? = null,
): Modifier {
    if (backdrop == null || !isRuntimeShaderSupported()) return fallback
    return textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = blurRadius,
        colors = BlurDefaults.blurColors(
            blendColors = blendColors
                ?: listOf(BlendColorEntry(color = containerColor.copy(alpha = containerAlpha))),
        ),
        highlight = highlight,
    )
}

/**
 * [couiGlassSurface] taking a shape provider, for a surface whose silhouette moves — a popup
 * morphing between two anchors, for instance. [shape] is re-read on every outline request, so it
 * must return a value that changes with the geometry; see the `textureEffect` provider overload
 * for the contract.
 *
 * @param shape Supplies the surface's silhouette for the current frame.
 */
@Composable
fun Modifier.couiGlassSurface(
    backdrop: Backdrop?,
    shape: () -> Shape,
    containerColor: Color,
    fallback: Modifier,
    blurRadius: Float = GlassDefaults.BlurRadius,
    containerAlpha: Float = GlassDefaults.ContainerAlpha,
    blendColors: List<BlendColorEntry>? = null,
    highlight: Highlight? = null,
): Modifier {
    if (backdrop == null || !isRuntimeShaderSupported()) return fallback
    return textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = blurRadius,
        colors = BlurDefaults.blurColors(
            blendColors = blendColors
                ?: listOf(BlendColorEntry(color = containerColor.copy(alpha = containerAlpha))),
        ),
        highlight = highlight,
    )
}
