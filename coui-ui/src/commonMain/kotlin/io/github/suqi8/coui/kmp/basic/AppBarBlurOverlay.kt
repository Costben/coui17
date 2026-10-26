// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
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
     * How far the material reaches past the app bar's bottom edge.
     *
     * `AppBarBlurOverlayView.EXTRA_HEIGHT_DP_SCROLL_DRIVEN` (24dp). `AppBarBlurOverlayBehavior`
     * measures the overlay to `appBar.top .. appBar.bottom + extraHeightPx`, so the bar's bottom
     * edge keeps some blur and surface alpha, and both fade out over this band instead of stopping
     * on the bar's own edge. (The settings homepage inflates its non-scroll-driven overlay with
     * `homepage_appbar_blur_extra_height` = 14dp — measured 54px at 620dpi — but its scroll-driven
     * form uses 24dp; the taller band is the one that leaves a visible fade under a collapsed bar.)
     */
    val ExtraHeight = 24.dp

    /**
     * `AppBarBlurOverlayBehavior.ALPHA_SCROLL_THRESHOLD_DP`: the scroll distance over which the overlay's
     * alpha ramps from 0 to 1. At rest the overlay is invisible; it reaches full strength once the
     * content has scrolled this far.
     */
    val ScrollAlphaThresholdDp = 30f

    /**
     * The dark-theme material base colour.
     *
     * The dark page background renders as a flat `48 / 48 / 48` (`#303030`), measured across whole
     * pages on two native surfaces. `#303030` is also the platform's own dark background
     * (`background_material_dark` → `material_grey_850` = `#ff303030`), so it is a colour the
     * platform declares, not one inferred from a screenshot mean. The material's top edge over a
     * `#454545` card lands at 60, and the dark ramp's hard-coded top alpha `0.4 * B + 0.6 * 69 = 60`
     * gives `B ≈ 46.5`, consistent with 48.
     *
     * The colour token that produces this value cannot be resolved: an enabled runtime overlay sits
     * on top of the night configuration, and the darkening layer is applied by the vendor framework
     * itself. The rendered value is used here instead, with the light-sensing glass's own alpha ramp
     * doing the rest.
     */
    val DarkContainerColor = Color(0xFF303030)

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

    /**
     * [scrollAlpha] with a resting floor, for a bar whose material never fades out completely.
     *
     * ColorOS 17's home page keeps a layer of material while the bar is fully expanded. The
     * downstream project measured `0.55` as that floor on a PLK110 running ColorOS 17, and feeds
     * this overload's result straight to [AppBarBlurOverlay]'s `scrollFraction`.
     *
     * Pass the content's actual displacement, **not** the bar's collapsed fraction. Binding the
     * alpha to the collapsed fraction makes it dip whenever the bar expands from an intermediate
     * position: the downstream project saw alpha fall from `1.0` to `0.55` when the list was
     * dragged down 240px from the middle and the avatar row came back, even though the material
     * had been fully opaque. Measured status-bar band luminance stayed at 222–225 (0–255) during
     * that drag against a 223–235 baseline, which is why the alpha has to follow displacement.
     *
     * @param scrollPx The distance the content has scrolled beneath the bar, in pixels.
     * @param density The density the threshold is converted with.
     * @param minAlpha The floor the result never drops below, coerced into `0f..1f`.
     */
    fun scrollAlpha(scrollPx: Float, density: Density, minAlpha: Float): Float {
        val floor = minAlpha.coerceIn(0f, 1f)
        return (floor + (1f - floor) * scrollAlpha(scrollPx, density)).coerceIn(0f, 1f)
    }
}

/**
 * The ColorOS 17 app bar material overlay — `AppBarBlurOverlayView` as `AppBarBlurHelper` dresses
 * it.
 *
 * The native overlay is inflated at `layout_height=0dp` and resized by `AppBarBlurOverlayBehavior`
 * to the app bar's bounds plus [AppBarBlurOverlayDefaults.ExtraHeight], so the material is taller
 * than the bar. This composable measures its [content] to find the bar, draws the material over
 * `bar + extraHeight`, and reports the **bar's** height, so the extra band never reaches a
 * `Scaffold`'s content padding.
 *
 * The material keeps the framework's gradient shape, anchored to the node's bottom edge rather
 * than to the bar's: `GradientBackgroundDrawable` builds `LinearGradient(0, height, 0, 0, colors,
 * positions)`, which is alpha 0 on the bottom edge, 0.7 at 40% up, and 1 at the top (light theme),
 * or 0 up to 0.4 (dark theme). Because the node is `bar + extraHeight` tall, the bar's own bottom
 * edge still carries some surface colour and the colour fades to nothing over the extra band.
 *
 * The blur is a progressive one at the framework's 30px gradient radius, strongest at the node's
 * top edge and fading to sharp on its bottom edge. Stretching it over the extra band is what keeps
 * the bar's bottom edge visibly blurred — the same edge is almost sharp when the ramp is squeezed
 * into the bar alone — while the node's bottom edge is pixel-sharp, so the material meets the list
 * without a seam. The native blend curve (`pow(1 - t, gradientBlurSpeed)`) is applied by
 * [ProgressiveBlur].
 *
 * When [backdrop] is `null` — or the platform cannot run runtime shaders, which is everything below
 * Android 13 — the blur is skipped and the plain ramp is painted, the way the framework's gradient
 * lives in the app bar's own background drawable rather than in the RenderEffect.
 *
 * On ColorOS with OplusView sub-SDK 33.34 or later, [backdrop] is not used: the material is the
 * platform view `AppBarBlurHelper` dresses, with the framework's gradient-blur backdrop effect set
 * once, and scrolling only changes that view's alpha.
 *
 * @param modifier The modifier to be applied to the overlay. The reported size is the size of
 *   [content], coerced up to the incoming minimum constraints; the material is drawn [extraHeight]
 *   past the reported bottom edge.
 * @param containerColor The base colour the alpha ramp is drawn in. Defaults to
 *   [AppBarBlurOverlayDefaults.containerColor], which is the theme's `surface` in the light theme —
 *   `coui_color_background_with_card`, the value the device's fully opaque top edge holds — and
 *   [AppBarBlurOverlayDefaults.DarkContainerColor] in the dark theme.
 * @param backdrop The content to sample, normally [LocalBackdrop]'s value.
 * @param extraHeight How far the material reaches past the bar's bottom edge. Defaults to
 *   [AppBarBlurOverlayDefaults.ExtraHeight].
 * @param scrollFraction The scroll-driven alpha, `min(1, scrollPx / 30dp)` — see
 *   [AppBarBlurOverlayDefaults.scrollAlpha]. At `0` the overlay is invisible and draws nothing; the
 *   value is coerced into `0f..1f`.
 * @param content The app bar content. It is measured to find the bar's height, drawn on top of the
 *   material, and defines the layout height this composable reports.
 */
@Composable
fun AppBarBlurOverlay(
    modifier: Modifier = Modifier,
    containerColor: Color = AppBarBlurOverlayDefaults.containerColor(),
    backdrop: Backdrop? = LocalBackdrop.current,
    extraHeight: Dp = AppBarBlurOverlayDefaults.ExtraHeight,
    scrollFraction: Float = 1f,
    platformRenderer: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    AppBarBlurOverlay(
        modifier = modifier,
        containerColor = containerColor,
        backdrop = backdrop,
        extraHeight = extraHeight,
        scrollFraction = { scrollFraction },
        platformRenderer = platformRenderer,
        content = content,
    )
}

/**
 * [AppBarBlurOverlay] whose alpha is read from a producer instead of a composed value.
 *
 * The producer is read in the draw phase, so a scroll-driven
 * [scrollFraction] does not recompose the bar on every frame. At rest it must return `0` (the
 * native overlay's `INVISIBLE` state); the material then draws nothing, leaving the content under
 * the bar sharp. A caller with no scroll state to observe should return `1f`.
 *
 * The `scrollFraction` overload with a [Float] forwards to this one.
 *
 * @param platformRenderer When the platform has its own backdrop blur (ColorOS), use it. It samples
 *   the window content, so a material whose **size changes every frame** — an app bar whose content
 *   height animates, for instance — feeds its own output back into the effect and the bar goes black
 *   for the duration of the animation. Pass `false` for those bars: COUI's own engine samples the
 *   recorded [backdrop] layer instead, which is stable under resizes.
 */
@Composable
fun AppBarBlurOverlay(
    scrollFraction: () -> Float,
    modifier: Modifier = Modifier,
    containerColor: Color = AppBarBlurOverlayDefaults.containerColor(),
    backdrop: Backdrop? = LocalBackdrop.current,
    extraHeight: Dp = AppBarBlurOverlayDefaults.ExtraHeight,
    platformRenderer: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    // Same contract as the app bar material in [TopAppBar]: the blur needs content underneath the
    // bar. Outside a `Scaffold(edgeToEdge = true)` the caller has to provide [LocalEdgeToEdge]
    // itself, otherwise this degrades to a plain box around [content].
    val edgeToEdge = LocalEdgeToEdge.current
    AppBarMaterialLayout(
        materialEnabled = edgeToEdge,
        containerColor = containerColor,
        backdrop = backdrop,
        extraHeight = extraHeight,
        scrollFraction = scrollFraction,
        platformRenderer = platformRenderer,
        modifier = modifier,
        content = content,
    )
}

/**
 * The single measure policy behind [AppBarBlurOverlay] and the app bar scaffold in [TopAppBar].
 *
 * [content] is measured with its minimum constraints dropped, and the layout reports its size
 * coerced up to the incoming minimum constraints. When [materialEnabled] is set, the material is
 * measured to that size plus [extraHeight] and placed behind [content] at the top-left corner, so it
 * reaches [extraHeight] past the reported bottom edge. When it is not set, the material child is
 * omitted and the layout is a plain box around [content].
 *
 * [content] is invoked from exactly one place, and toggling [materialEnabled] only adds or removes
 * the material child, so the content subtree is not recreated.
 */
@Composable
internal fun AppBarMaterialLayout(
    materialEnabled: Boolean,
    containerColor: Color,
    backdrop: Backdrop?,
    extraHeight: Dp,
    scrollFraction: () -> Float,
    platformRenderer: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val extraHeightPx = with(LocalDensity.current) { extraHeight.roundToPx() }
    Layout(
        content = {
            if (materialEnabled) {
                AppBarMaterial(
                    modifier = Modifier.layoutId("material"),
                    containerColor = containerColor,
                    backdrop = backdrop,
                    scrollFraction = scrollFraction,
                    platformRenderer = platformRenderer,
                )
            }
            Box(Modifier.layoutId("content")) { content() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val contentPlaceable =
            measurables
                .fastFirst { it.layoutId == "content" }
                .measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = maxOf(contentPlaceable.width, constraints.minWidth)
        val height = maxOf(contentPlaceable.height, constraints.minHeight)
        val materialPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == "material" }
                ?.measure(
                    Constraints.fixed(
                        width = width,
                        height = height + extraHeightPx,
                    ),
                )
        layout(width, height) {
            materialPlaceable?.placeRelative(0, 0)
            contentPlaceable.placeRelative(0, 0)
        }
    }
}

/**
 * The app bar material alone, for callers that place it behind their own bar content.
 *
 * It paints the gradient surface and the progressive blur over whatever size the caller measures
 * it to. [io.github.suqi8.coui.kmp.basic.TopAppBar] measures it to the bar plus
 * [AppBarBlurOverlayDefaults.ExtraHeight] while reporting only the bar height.
 *
 * @param modifier The size and position of the material node.
 * @param containerColor The base colour of the alpha ramp.
 * @param backdrop The content to sample; `null` skips the blur and paints the ramp alone.
 * @param scrollFraction The material's overall alpha, read in the draw phase. At `0` the material
 *   draws nothing, so the content under the bar stays sharp — the native overlay's resting state. The
 *   node stays composed at `0`, so scrolling away from rest does not rebuild the blur.
 */
@Composable
internal fun AppBarMaterial(
    modifier: Modifier = Modifier,
    containerColor: Color = AppBarBlurOverlayDefaults.containerColor(),
    backdrop: Backdrop? = LocalBackdrop.current,
    scrollFraction: () -> Float = { 1f },
    platformRenderer: Boolean = true,
) {
    val density = LocalDensity.current
    // The effect stays composed at every alpha, including 0. `AppBarBlurOverlayBehavior` only
    // changes the overlay's alpha (and visibility at exactly 0) while scrolling; the RenderEffect is
    // set once. Removing the blur node at rest and rebuilding it when scrolling starts recreated its
    // layers and shaders on the first frame of every scroll, which showed as a flash. The alpha is a
    // draw-phase read, so the bar still does not recompose on scroll frames.
    //
    // `isCOUIDarkThemeOrNightMode`, which picks the two-stop dark ramp over the three-stop light one.
    // It is a property of the theme, not of the base colour, so a caller-supplied colour cannot move
    // the ramp to the other branch.
    val isDarkTheme = COUITheme.colorScheme.background.luminance() < 0.5f
    // Where the platform has its own backdrop blur (ColorOS), use it exactly the way
    // `AppBarBlurHelper` does instead of re-recording the content through COUI's engine.
    val native = if (platformRenderer) rememberPlatformAppBarBackdropRenderer() else null
    if (native != null) {
        native.Material(
            containerColor = containerColor,
            dark = isDarkTheme,
            alpha = scrollFraction,
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    val brush = appBarBlurOverlayBrush(containerColor, isDarkTheme)
    val effect =
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = scrollFraction().coerceIn(0f, 1f)
                // The alpha must be grouped, the way the native overlay's View.setAlpha is. The
                // blur draws through its own child layers, and a modulated alpha only scales this
                // node's direct draws, so it never reached them: any alpha above 0 painted the
                // material at full strength and the 30dp fade-in was a one-frame pop. The offscreen
                // buffer only exists while the material is translucent.
                compositingStrategy = CompositingStrategy.Auto
            }
            .then(effect),
    )
}

/**
 * The overlay's alpha ramp, stretched over the node's full height.
 *
 * `GradientBackgroundDrawable` builds `LinearGradient(0, height, 0, 0, colors, positions)` — the
 * ramp is measured from the **node's** bottom edge up. The node carries the bar plus the extra
 * band, so the bar's own bottom edge still has some alpha and the surface fades out below it.
 *
 * Compose's [Brush.verticalGradient] runs top→bottom while the native gradient runs bottom→top, so
 * the stops are mirrored — the same way the app bar's own gradient brush mirrors them. The
 * positions and alphas come from [AppBarGlassDefaults].
 */
@Composable
private fun appBarBlurOverlayBrush(color: Color, dark: Boolean): Brush = remember(color, dark) {
    if (dark) {
        Brush.verticalGradient(
            0f to color.copy(alpha = AppBarGlassDefaults.GradientAlphaDark),
            1f to color.copy(alpha = AppBarGlassDefaults.GradientAlpha0),
        )
    } else {
        Brush.verticalGradient(
            0f to color.copy(alpha = AppBarGlassDefaults.GradientAlpha2),
            1f - AppBarGlassDefaults.GradientPos1 to color.copy(alpha = AppBarGlassDefaults.GradientAlpha1),
            1f to color.copy(alpha = AppBarGlassDefaults.GradientAlpha0),
        )
    }
}
