// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The ColorOS 17 page indicator: a centred row of dots, one dot per page, with the current page
 * drawn in the trace colour and every other dot in the dot colour.
 *
 * The dots keep a constant centre-to-centre pitch of `DotSize + 2 * DotSpacing` = 12dp, so a dot of
 * any diameter is centred inside its own 12dp cell; the whole indicator is
 * [PageIndicatorDefaults.DotSize] tall, i.e. exactly one normal dot.
 *
 * Up to [PageIndicatorDefaults.MaxVisibleDots] (6) dots are drawn, the count the design clamps its
 * visible rect to; with more pages the visible dots slide as a window over the page list.
 *
 * The dots are display-only. The guide dialog turns tapping off on its indicator, and like the
 * default style nothing here reacts to taps.
 *
 * Not ported:
 * - the per-frame size/offset mask behind the sliding window. The level ladder
 *   (large/medium/small/gone sizes 3/5/7/9, at most 4 large and 2 medium dots) is known, but the
 *   exact offset maths is not, so this port slides a clamped window and shrinks the two dots that
 *   have hidden neighbours to [PageIndicatorDefaults.DotSizeMedium] as an approximation.
 *   [PageIndicatorDefaults.DotSizeSmall] is exposed but not applied;
 * - the moving-trace colour blend and the 1500-stiffness position spring (`dampingRatio` = 1.0,
 *   `minimumVisibleChange` = 0.005). The active dot simply carries [activeColor];
 * - the accessibility description, whose strings are not reproduced.
 *
 * @param count The number of pages. `count <= 0` draws nothing.
 * @param currentIndex The index of the active page. Clamped into `0 until count`.
 * @param modifier The modifier to be applied to the indicator.
 * @param activeColor The colour of the active dot, resolved from `coui_color_label_primary`, the
 *   theme's `COUITheme.colorScheme.onSurface`.
 * @param inactiveColor The colour of the other dots, resolved from `coui_color_hint_neutral`, the
 *   theme's `COUITheme.colorScheme.disabledOnSurface`.
 */
@Composable
fun PageIndicator(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = COUITheme.colorScheme.onSurface,
    inactiveColor: Color = COUITheme.colorScheme.disabledOnSurface,
) {
    if (count <= 0) return
    val activeIndex = currentIndex.coerceIn(0, count - 1)
    val maxVisible = PageIndicatorDefaults.MaxVisibleDots
    val visibleCount = if (count <= maxVisible) count else maxVisible
    val windowStart = if (count <= maxVisible) {
        0
    } else {
        // Keep the active dot near the middle of the window, clamped so the window never leaves the
        // page list. The native per-frame offset maths is not reproduced; see the KDoc above.
        (activeIndex - (maxVisible - 1) / 2).coerceIn(0, count - maxVisible)
    }
    val pitch = PageIndicatorDefaults.DotSize + PageIndicatorDefaults.DotSpacing * 2f
    val width = pitch * (visibleCount - 1).toFloat() + PageIndicatorDefaults.DotSize

    Canvas(
        modifier = modifier.size(width = width, height = PageIndicatorDefaults.DotSize),
    ) {
        val pitchPx = pitch.toPx()
        val dotPx = PageIndicatorDefaults.DotSize.toPx()
        val mediumPx = PageIndicatorDefaults.DotSizeMedium.toPx()
        val centreY = size.height / 2f
        repeat(visibleCount) { slot ->
            val index = windowStart + slot
            val fadedByWindow = (slot == 0 && windowStart > 0) ||
                (slot == visibleCount - 1 && windowStart + visibleCount < count)
            val diameter = if (fadedByWindow) mediumPx else dotPx
            drawCircle(
                color = if (index == activeIndex) activeColor else inactiveColor,
                radius = diameter / 2f,
                center = Offset(slot * pitchPx + dotPx / 2f, centreY),
            )
        }
    }
}

/** Contains default values used by [PageIndicator]. */
object PageIndicatorDefaults {

    /** The diameter of a dot, 6.0dp. */
    val DotSize = 6.dp

    /**
     * The diameter of a dot at the edge of a sliding window, 4.5dp. This port applies it to the
     * dots adjacent to a hidden page; see [PageIndicator].
     */
    val DotSizeMedium = 4.5.dp

    /**
     * The smallest dot diameter of the native level ladder, 2.5dp. Exposed for completeness: the port
     * never reaches it, because the per-frame mask that picks between the levels is not reproduced.
     */
    val DotSizeSmall = 2.5.dp

    /**
     * The half-interval between two dots, 3.0dp. The pitch is derived as `mInterval = 2 * spacing` =
     * 6dp, so the centre-to-centre pitch is `DotSize + 2 * DotSpacing` = 12dp.
     */
    val DotSpacing = 3.dp

    /**
     * The maximum number of dots drawn at once, 6; the visible rect is measured as `min(6, count)`
     * dots wide.
     */
    val MaxVisibleDots = 6
}
