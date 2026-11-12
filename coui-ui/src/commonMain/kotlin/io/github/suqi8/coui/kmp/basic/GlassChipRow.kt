// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.BlurColors
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.roundToInt

/** One entry of a [GlassChipRow]. */
@Immutable
data class GlassChipItem(
    /** The label of the chip, kept to a single line. */
    val label: String,
    /** An optional leading icon, drawn at [GlassChipDefaults.IconSize]. */
    val icon: ImageVector? = null,
)

/** Defaults for [GlassChipRow] and [GlassChipButton]. */
object GlassChipDefaults {

    /** The height of a chip. COUI `coui_chip_tab_style_height`. */
    val Height = 32.dp

    /** The minimum width of a chip. COUI `coui_chip_default_min_width`. */
    val MinWidth = ChipTabDefaults.MinWidth

    /** The maximum width of a chip. COUI `coui_chip_default_max_width`. */
    val MaxWidth = ChipTabDefaults.MaxWidth

    /** The gap between two chips and between the leading slot and the row. */
    val Gap = ChipTabDefaults.GroupSpacing

    /** The start / end padding inside a chip. COUI `coui_chip_suggest_style_chip_horizontal_padding`. */
    val HorizontalPadding = ChipTabDefaults.StartPadding

    /** The size of a chip's leading icon. COUI `coui_chip_tab_style_chip_icon_size`. */
    val IconSize = ChipTabDefaults.IconSize

    /** The gap between a chip's icon and its label. COUI `coui_chip_suggest_style_chip_icon_end_padding`. */
    val IconGap = ChipTabDefaults.IconEndPadding

    /** The icon size of an icon-only [GlassChipButton]. */
    val ButtonIconSize = 18.dp

    /**
     * The width of the fading edge. COUI `ChipMaskHorizontalScrollView.FADING_EDGE_WIDTH`, which the
     * scroll view also reports as its `getHorizontalFadingEdgeLength()`.
     */
    val FadeWidth = ChipTabDefaults.FadeWidth

    /**
     * The blur radius of a chip, in pixels of the current density.
     *
     * The Notes app gives every chip a `RenderEffect` of `createBlurEffect(40, 40, MIRROR)` plus two
     * blended colours (`COUIMaterialBlurEffect.TYPE_CONTENT_CHIP_TAB_SELECTED` and its unselected
     * sibling). HWUI reads that 40 as a blur radius and turns it into a sigma of
     * `0.57735 * radius`; this engine takes a radius and turns it into `0.45 * radius`, so
     * `40 * 0.57735 / 0.45` engine pixels reproduce the same blur. The radius stays in pixels
     * because that is what the platform call takes — divide by the density to get the dp value
     * [textureBlur] wants.
     */
    val BlurRadiusPx: Float = CHIP_HWUI_BLUR_RADIUS_PX * HWUI_RADIUS_TO_SIGMA / ENGINE_SIGMA_PER_RADIUS
}

private const val CHIP_HWUI_BLUR_RADIUS_PX = 40f
private const val HWUI_RADIUS_TO_SIGMA = 0.57735f
private const val ENGINE_SIGMA_PER_RADIUS = 0.45f

/**
 * A row of capsule "glass" tabs over the page backdrop.
 *
 * The material is the one the ColorOS 17 Notes app draws under its title: each chip is a blurred
 * copy of the backdrop behind it, cut to a capsule and washed with two blend colours, so the row
 * reads as a single sheet of frosted glass rather than as filled pills.
 *
 * The row shares one [Backdrop] with the bar above it: the page is recorded once and every chip
 * samples that recording. The Notes app has the same structure — the chip blur is a background
 * `RenderEffect` on the chip view, and the backdrop is the window, not per chip.
 *
 * Unlike [ChipTabGroup] nothing here runs a layer recording per frame: the blur is rebuilt only
 * when [selectedIndex] or the theme changes, which is what keeps the row from flickering.
 *
 * The row must not be placed inside the very [Modifier.layerBackdrop] that records [backdrop]: a
 * chip would then sample a layer it is itself painted into, and the render node cycle overflows
 * the render thread's stack. Keep the row outside the recorded layer, as a sibling of the content
 * it samples (a bar above the list, not an item inside it), or pass `backdrop = null` and let the
 * chips fall back to their flat fill.
 *
 * @param items The chips to show.
 * @param selectedIndex The index of the selected chip, or `-1` for none.
 * @param onSelected Called with the index of the chip that was tapped.
 * @param modifier The modifier to be applied to the row.
 * @param backdrop The backdrop the chips sample. When `null` (or blur is disabled) the chips fall
 *   back to a flat fill.
 * @param leading An optional slot before the scrolling row, such as a [GlassChipButton].
 * @param contentPadding The padding around the whole row.
 * @param centered Whether the chips are centered when they fit in the viewport. When they overflow
 *   the row still scrolls, and the overflowing side keeps its fading edge.
 * @param onReselect Called when the selected chip is tapped a second time, for the "tap again to
 *   jump to the top and refresh" gesture. A single tap on the selected chip then does nothing.
 */
@Composable
fun GlassChipRow(
    items: List<GlassChipItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalBackdrop.current,
    leading: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    centered: Boolean = false,
    onReselect: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    // Where each chip sits inside the scrolling content, excluding the scroll offset, so the
    // selected one can be pulled into view.
    val tabBounds = remember(items.size) { mutableStateMapOf<Int, Rect>() }
    // The Notes app only scrolls on a tap or a programmatic selection, never on first composition.
    var firstSelection by remember { mutableStateOf(true) }
    LaunchedEffect(selectedIndex) {
        if (firstSelection) {
            firstSelection = false
            return@LaunchedEffect
        }
        val bounds = tabBounds[selectedIndex] ?: return@LaunchedEffect
        with(density) {
            scrollState.scrollTabIntoView(bounds, GlassChipDefaults.FadeWidth.toPx())
        }
    }

    val chips: @Composable () -> Unit = {
        items.forEachIndexed { index, item ->
            GlassChip(
                label = item.label,
                icon = item.icon,
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                onReselect = if (index == selectedIndex) onReselect else null,
                backdrop = backdrop,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    tabBounds[index] = coordinates.boundsInParent()
                },
            )
        }
    }

    Row(
        modifier = modifier.padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(GlassChipDefaults.Gap))
        }
        if (centered) {
            BoxWithConstraints(Modifier.weight(1f).edgeFade(scrollState)) {
                val viewport = maxWidth
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        // The scrollable content is measured unbounded; this makes it at least as
                        // wide as the viewport so [Arrangement.spacedBy] has the slack to center.
                        .widthIn(min = viewport),
                    horizontalArrangement = Arrangement.spacedBy(
                        GlassChipDefaults.Gap,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    content = { chips() },
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .edgeFade(scrollState)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(GlassChipDefaults.Gap),
                verticalAlignment = Alignment.CenterVertically,
                content = { chips() },
            )
        }
    }
}

/**
 * An icon-only glass capsule, the leading slot of a [GlassChipRow].
 *
 * @param icon The icon to draw, at [GlassChipDefaults.ButtonIconSize].
 * @param contentDescription The accessibility label of the button.
 * @param onClick Called when the button is tapped.
 * @param modifier The modifier to be applied to the button.
 * @param backdrop The backdrop the capsule samples.
 */
@Composable
fun GlassChipButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalBackdrop.current,
) {
    GlassChipSurface(
        selected = true,
        onClick = onClick,
        onReselect = null,
        backdrop = backdrop,
        modifier = modifier.size(GlassChipDefaults.Height),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(GlassChipDefaults.ButtonIconSize),
            tint = chipContentColor(dark = true, fraction = 1f),
        )
    }
}

@Composable
private fun GlassChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    onReselect: (() -> Unit)?,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    GlassChipSurface(
        selected = selected,
        onClick = onClick,
        onReselect = onReselect,
        backdrop = backdrop,
        modifier = modifier
            .height(GlassChipDefaults.Height)
            .widthIn(min = GlassChipDefaults.MinWidth, max = GlassChipDefaults.MaxWidth),
        contentPadding = PaddingValues(horizontal = GlassChipDefaults.HorizontalPadding),
    ) { contentColor ->
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(GlassChipDefaults.IconSize),
                tint = contentColor,
            )
            Spacer(Modifier.width(GlassChipDefaults.IconGap))
        }
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassChipSurface(
    selected: Boolean,
    onClick: () -> Unit,
    onReselect: (() -> Unit)?,
    backdrop: Backdrop?,
    modifier: Modifier,
    contentPadding: PaddingValues,
    content: @Composable RowScope.(contentColor: Color) -> Unit,
) {
    val fraction by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 986.96f,
        ),
        visibilityThreshold = 1f / 256f,
        label = "glassChipFraction",
    )
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    val shape = remember { RoundedCornerShape(percent = 50) }
    val density = LocalDensity.current
    val blurRadius: Dp = remember(density) {
        with(density) { GlassChipDefaults.BlurRadiusPx.toDp() }
    }
    val colors = remember(dark, fraction) { chipBlurColors(dark, fraction) }
    val flatFill = remember(dark, fraction) {
        if (dark) {
            lerp(Color(0xFF373737), Color(0xFF4A4A4A), fraction)
        } else {
            lerp(Color(0x2E4E4E4E), Color(0x8FE8E8E8), fraction)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (backdrop != null) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = shape,
                        blurRadius = blurRadius.value,
                        colors = colors,
                    )
                } else {
                    Modifier.background(flatFill)
                },
            )
            .drawWithContent {
                drawContent()
                drawCheckedHighlight(fraction, dark)
            }
            .then(
                if (onReselect != null) {
                    Modifier.combinedClickable(onClick = onClick, onDoubleClick = onReselect)
                } else {
                    Modifier.clickable(onClick = onClick)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(chipContentColor(dark, fraction))
        }
    }
}

/**
 * Scrolls the chip at [bounds] into the window `[fadePx, viewport - fadePx]`, the region the fading
 * edges leave visible. Mirrors `COUIChipTabGroup.checkOcclusion` / `scrollToChipTabWithAnimation`.
 */
private suspend fun ScrollState.scrollTabIntoView(bounds: Rect, fadePx: Float) {
    val range = maxValue
    if (range <= 0) return
    val scrollX = value
    val visibleLeft = if (scrollX > 0) scrollX + fadePx else scrollX.toFloat()
    val visibleRight =
        if (scrollX < range) scrollX + viewportSize - fadePx else (scrollX + viewportSize).toFloat()
    val delta = when {
        bounds.left < visibleLeft -> bounds.left - visibleLeft
        bounds.right > visibleRight -> bounds.right - visibleRight
        else -> return
    }
    animateScrollTo(
        value = (scrollX + delta).roundToInt().coerceIn(0, range),
        animationSpec = ChipTabDefaults.ScrollSpring,
    )
}

/**
 * The `PorterDuff.DST_OUT` gradients of `ChipMaskHorizontalScrollView.dispatchDraw`, drawn only on
 * the side the row can still scroll to.
 */
private fun Modifier.edgeFade(scrollState: ScrollState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (scrollState.maxValue <= 0) return@drawWithContent
        val fadePx = GlassChipDefaults.FadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fadePx <= 0f) return@drawWithContent
        if (scrollState.canScrollBackward) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    startX = 0f,
                    endX = fadePx,
                ),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstOut,
            )
        }
        if (scrollState.canScrollForward) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White),
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstOut,
            )
        }
    }

/**
 * The highlight of the checked chip: a hairline along the top and bottom arcs, nothing on the
 * sides, plus a thin inner glow at the bottom. The values match COUI's own `COUIChipTabDrawable`
 * port in [ChipTab] (0.23 / 0.14 white in dark themes, solid white in light).
 */
private fun DrawScope.drawCheckedHighlight(fraction: Float, dark: Boolean) {
    val alpha = fraction.coerceIn(0f, 1f)
    if (alpha <= 0f) return
    val strokeWidth = 1.dp.toPx()
    val inset = strokeWidth / 2f
    val strokeBrush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = (if (dark) 0.23f else 1f) * alpha),
        0.30f to Color.Transparent,
        0.70f to Color.Transparent,
        1f to Color.White.copy(alpha = (if (dark) 0.14f else 1f) * alpha),
        startY = 0f,
        endY = size.height,
    )
    val radius = (size.minDimension / 2f - inset).coerceAtLeast(0f)
    drawRoundRect(
        brush = strokeBrush,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = strokeWidth),
    )
    val glowHeight = 2.dp.toPx()
    val glowAlpha = (if (dark) 0.06f else 0.85f) * alpha
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = glowAlpha)),
            startY = size.height - glowHeight,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height - glowHeight),
        size = Size(size.width, glowHeight),
    )
}

private fun chipContentColor(dark: Boolean, fraction: Float): Color {
    val idle = if (dark) Color(0xC7FFFFFF) else Color(0xE6000000)
    val active = if (dark) Color(0xF2FFFFFF) else Color(0xF2000000)
    return lerp(idle, active, fraction)
}

/**
 * The two blend colours of a chip.
 *
 * Light: unchecked `#144E4E4E` ColorDodge + `#14606060` Luminosity, checked `#66A3A1A1` +
 * `#99F5F5F5`. Dark: unchecked `#33B2B2B2` Luminosity + `#33303030` Overlay, checked `#A6505050` +
 * `#4CA7A7A7`. These are the pairs the Notes app carries, interpolated by the checked fraction so
 * the selection spring drives the colour the same way the platform drives its layer alpha.
 */
private fun chipBlurColors(dark: Boolean, fraction: Float): BlurColors {
    val f = fraction.coerceIn(0f, 1f)
    return if (dark) {
        BlurColors(
            blendColors = listOf(
                BlendColorEntry(lerp(Color(0x33B2B2B2), Color(0xA6505050), f), BlurBlendMode.Luminosity),
                BlendColorEntry(lerp(Color(0x33303030), Color(0x4CA7A7A7), f), BlurBlendMode.Overlay),
            ),
        )
    } else {
        BlurColors(
            blendColors = listOf(
                BlendColorEntry(lerp(Color(0x144E4E4E), Color(0x66A3A1A1), f), BlurBlendMode.ColorDodge),
                BlendColorEntry(lerp(Color(0x14606060), Color(0x99F5F5F5), f), BlurBlendMode.Luminosity),
            ),
        )
    }
}
