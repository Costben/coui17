// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.ProgressiveBlur
import io.github.suqi8.coui.kmp.blur.progressiveTextureBlur
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Default values for [AppBarBlurOverlay]. */
object AppBarBlurOverlayDefaults {

    /**
     * How far the overlay reaches past the app bar's bottom edge.
     *
     * COUI `homepage_appbar_blur_extra_height` (14dp). `AppBarBlurOverlayBehavior` measures the
     * overlay to `appBar.top .. appBar.bottom + extraHeightPx`, so the material keeps ramping past
     * the bar instead of stopping at its edge.
     */
    val ExtraHeight = 14.dp

    /**
     * `AppBarBlurHelper.ALPHA_SCROLL_THRESHOLD_DP`: the scroll distance over which the overlay's
     * alpha ramps from 0 to 1. At rest the overlay is invisible; it reaches full strength once the
     * content has scrolled this far.
     */
    val ScrollAlphaThresholdDp = 30f

    /**
     * The dark-theme material base colour.
     *
     * The framework resolves the dark base from `?couiColorBackgroundWithCard`, and that attribute is
     * `#ffffff` in the night configuration — a colour that cannot produce the profile measured on the
     * device. There the material's top edge lands at 60 over a `#454545` card, and with the dark
     * ramp's hard-coded top alpha `0.4 * B + 0.6 * 69 = 60` gives `B ≈ 46.5`. The nearest ColorOS
     * night token is `coui_color_background_top` / `coui_color_surface_top` = `#333333`.
     *
     * No explicit colour can be hiding the gap: `AppBarBlurHelper.setGradientBlurBgColor` has no call
     * site anywhere in the Settings APK's fourteen dex files, so its static never leaves `null` and
     * the page fallback is what resolves. The dark path therefore carries a darkening layer this port
     * cannot read — the framework's `com.oplus.view.*`, of which the repository only has the call
     * site `COUIMaterialEffect.java:94-113`. The measured value is used here instead, with the
     * light-sensing glass's own alpha ramp doing the rest.
     */
    val DarkContainerColor = Color(0xFF333333)

    /**
     * The material base colour for the current theme: the light theme's `surface`, which is
     * `coui_color_background_with_card` = `#f0f1f2` — the value the device's fully opaque top edge
     * holds — and [DarkContainerColor] for the dark theme.
     */
    @Composable
    fun containerColor(): Color {
        val scheme = COUITheme.colorScheme
        return if (scheme.background.luminance() < 0.5f) DarkContainerColor else scheme.surface
    }

    /**
     * The scroll-driven alpha the native overlay applies, `min(1, scrollPx / 30dp)`.
     *
     * Feed the result to [AppBarBlurOverlay]'s `scrollFraction`.
     *
     * @param scrollPx The distance the content has scrolled beneath the bar, in pixels.
     * @param density The density the threshold is converted with.
     */
    fun scrollAlpha(scrollPx: Float, density: Density): Float {
        val threshold = with(density) { ScrollAlphaThresholdDp.dp.toPx() }
        if (threshold <= 0f) return 1f
        return (scrollPx / threshold).coerceIn(0f, 1f)
    }
}

/**
 * The ColorOS 17 app bar material overlay — `AppBarBlurOverlayView` as `AppBarBlurHelper` dresses
 * it.
 *
 * The native overlay is inflated at `layout_height=0dp` and resized by `AppBarBlurOverlayBehavior`
 * to the app bar's bounds plus [AppBarBlurOverlayDefaults.ExtraHeight], so **the caller sizes this
 * node**: pass `Modifier.height(appBarHeight + extraHeight)` (or equivalent). The overlay itself
 * paints nothing; the helper gives it a gradient alpha ramp plus a `RenderEffect` progressive blur,
 * and the behavior drives the whole view's alpha from the scroll offset.
 *
 * The ramp is anchored to the **bar**, not to the node: it is fully transparent over the
 * [extraHeight] band the caller added below the bar, and climbs from alpha 0 on the bar's bottom
 * edge to full strength at the node's top edge. The extra band is folded into the brush's stop
 * positions, so the material reads as the bar's own surface rather than as a taller gradient.
 *
 * The blur is a progressive one at the framework's 30px gradient radius, so it is full strength
 * along the top edge and has faded to nothing by the bar's bottom edge; the native blend curve
 * (`pow(1 - t, gradientBlurSpeed)`) is applied by [ProgressiveBlur].
 *
 * When [backdrop] is `null` — or the platform cannot run runtime shaders, which is everything below
 * Android 13 — the blur is skipped and the plain ramp is painted, the way the framework's gradient
 * lives in the app bar's own background drawable rather than in the RenderEffect.
 *
 * @param modifier The modifier to be applied to the overlay. The caller is expected to size the
 *   node to the app bar's height plus [extraHeight].
 * @param containerColor The base colour the alpha ramp is drawn in. Defaults to
 *   [AppBarBlurOverlayDefaults.containerColor], which is the theme's `surface` in the light theme —
 *   `coui_color_background_with_card`, the value the device's fully opaque top edge holds — and
 *   [AppBarBlurOverlayDefaults.DarkContainerColor] in the dark theme.
 * @param backdrop The content to sample, normally [LocalBackdrop]'s value.
 * @param extraHeight How far the overlay reaches past the bar's bottom edge. Defaults to
 *   [AppBarBlurOverlayDefaults.ExtraHeight].
 * @param scrollFraction The scroll-driven alpha, `min(1, scrollPx / 30dp)` — see
 *   [AppBarBlurOverlayDefaults.scrollAlpha]. At `0` the overlay is invisible and draws nothing; the
 *   value is coerced into `0f..1f`.
 * @param content The app bar content the caller hollows out, drawn on top of the material. It is
 *   not part of the material: the title and the actions stay readable at rest, when the material is
 *   still invisible.
 */
@Composable
fun AppBarBlurOverlay(
    modifier: Modifier = Modifier,
    containerColor: Color = AppBarBlurOverlayDefaults.containerColor(),
    backdrop: Backdrop? = LocalBackdrop.current,
    extraHeight: Dp = AppBarBlurOverlayDefaults.ExtraHeight,
    scrollFraction: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    val extraHeightPx = with(density) { extraHeight.toPx() }
    val materialAlpha = scrollFraction.coerceIn(0f, 1f)
    // `isCOUIDarkThemeOrNightMode`, which picks the two-stop dark ramp over the three-stop light one.
    // It is a property of the theme, not of the base colour, so a caller-supplied colour cannot move
    // the ramp to the other branch.
    val isDarkTheme = COUITheme.colorScheme.background.luminance() < 0.5f

    BoxWithConstraints(modifier = modifier) {
        val material = if (materialAlpha > 0f) {
            // The ramp is anchored to the bar's bottom edge, which sits `extraHeight` above the
            // node's. A node with no usable height has no extra band to fold in, so the stops fall
            // back to the bar-height case.
            val nodeHeightPx = constraints.maxHeight
            val barBottomFraction = if (nodeHeightPx > 0 && nodeHeightPx != Constraints.Infinity) {
                (1f - extraHeightPx / nodeHeightPx).coerceIn(0f, 1f)
            } else {
                1f
            }
            val brush = appBarBlurOverlayBrush(containerColor, barBottomFraction, isDarkTheme)
            if (backdrop == null || !isRuntimeShaderSupported()) {
                Modifier.background(brush)
            } else {
                Modifier.progressiveTextureBlur(
                    backdrop = backdrop,
                    shape = RectangleShape,
                    blurRadius = with(density) { AppBarGlassDefaults.GradientBlurRadiusPx.toDp().value },
                    gradient = ProgressiveBlur.Top.copy(curve = AppBarGlassDefaults.GradientBlurSpeed),
                    colors = BlurDefaults.blurColors(),
                ).background(brush)
            }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = materialAlpha }
                .then(material),
        )
        content()
    }
}

/**
 * The overlay's alpha ramp, stretched over the node's full height.
 *
 * `GradientBackgroundDrawable` builds `LinearGradient(0, barHeight, 0, 0, colors, positions)` — the
 * ramp is measured from the **bar's** bottom edge up, while the node also carries the extra band
 * below the bar. With `p` measured from the bar bottom and [barBottomFraction] the fraction of the
 * node the bar occupies, a node stop at fraction `f` sits at `p = 1 - f / barBottomFraction`; the
 * stops below put the ramp's `p = 0` stop exactly on the bar's bottom edge and leave everything
 * under it transparent.
 *
 * Compose's [Brush.verticalGradient] runs top→bottom while the native gradient runs bottom→top, so
 * the stops are mirrored — the same way the app bar's own gradient brush mirrors them. The
 * positions and alphas come from [AppBarGlassDefaults].
 */
@Composable
private fun appBarBlurOverlayBrush(color: Color, barBottomFraction: Float, dark: Boolean): Brush = remember(color, dark, barBottomFraction) {
    if (dark) {
        Brush.verticalGradient(
            0f to color.copy(alpha = AppBarGlassDefaults.GradientAlphaDark),
            barBottomFraction to color.copy(alpha = AppBarGlassDefaults.GradientAlpha0),
        )
    } else {
        Brush.verticalGradient(
            0f to color.copy(alpha = AppBarGlassDefaults.GradientAlpha2),
            barBottomFraction * (1f - AppBarGlassDefaults.GradientPos1) to
                color.copy(alpha = AppBarGlassDefaults.GradientAlpha1),
            barBottomFraction to color.copy(alpha = AppBarGlassDefaults.GradientAlpha0),
        )
    }
}
