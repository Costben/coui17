// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.COUIEase
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The ColorOS 17 page indicator: a centred row of dots, one dot per page, with the current page
 * drawn in the trace colour and every other dot in the dot colour.
 *
 * The dots keep a constant centre-to-centre pitch of `DotSize + 2 * DotSpacing` = 12dp, so a dot of
 * any diameter is centred inside its own 12dp cell; the whole indicator is
 * [PageIndicatorDefaults.DotSize] tall, i.e. exactly one normal dot.
 *
 * Up to [PageIndicatorDefaults.MaxVisibleDots] (6) dots are shown. With more pages the visible dots
 * form a window that slides over the page list, and a dot that leaves the window runs down the COUI
 * size ladder: full size inside the window, then [PageIndicatorDefaults.DotSizeMedium], then
 * [PageIndicatorDefaults.DotSizeSmall], then gone (COUIPageIndicator's four
 * `INDICATOR_DOT_*_LEVEL_SIZE` rungs, interpolated with `COUIEaseInterpolator`).
 *
 * The window follows [currentIndex] with COUIPageIndicator's position spring
 * (`SpringForce` stiffness 1500, `dampingRatio` 1.0, `minimumVisibleChange` 0.005), and the trace
 * colour of the current dot cross-fades over the first / last 5% of the travel
 * (`DEFAULT_COLOR_FACTOR_THRESHOLD`).
 *
 * The dots are display-only. The guide dialog turns tapping off on its indicator, and like the
 * default style nothing here reacts to taps.
 *
 * @param count The number of pages. `count <= 0` draws nothing.
 * @param currentIndex The index of the active page. Clamped into `0 until count`.
 * @param modifier The modifier to be applied to the indicator.
 * @param activeColor The colour of the active dot, resolved from `coui_color_label_primary`, the
 *   theme's `COUITheme.colorScheme.onSurface`.
 * @param inactiveColor The colour of the other dots, resolved from `coui_color_hint_neutral`, the
 *   theme's `COUITheme.colorScheme.disabledOnSurface`.
 * @param contentDescription The accessibility description of the indicator, `null` for the built-in
 *   "current / count" text (COUI `coui_page_indicator_description`). New in 1.2.0.
 */
@Composable
fun PageIndicator(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = COUITheme.colorScheme.onSurface,
    inactiveColor: Color = COUITheme.colorScheme.disabledOnSurface,
    contentDescription: String? = null,
) {
    if (count <= 0) return
    val activeIndex = currentIndex.coerceIn(0, count - 1)
    val maxVisible = PageIndicatorDefaults.MaxVisibleDots
    val visibleCount = if (count <= maxVisible) count else maxVisible

    // COUIPageIndicator.PageIndicatorModel: the position is a float that springs to the new page.
    val position = remember { Animatable(activeIndex.toFloat()) }
    LaunchedEffect(activeIndex) {
        position.animateTo(
            targetValue = activeIndex.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = PageIndicatorDefaults.POSITION_STIFFNESS,
                visibilityThreshold = PageIndicatorDefaults.MINIMUM_VISIBLE_CHANGE,
            ),
        )
    }
    val currentPosition = position.value
    // A dot is fully drawn inside the window; the mask slides with the position, so the dots that
    // leave it walk down the size ladder. With more than six pages the window is additionally
    // clamped to the page list, as mVisibleOffset does.
    val windowStart = if (count <= maxVisible) {
        0f
    } else {
        (currentPosition - (maxVisible - 1) / 2f).coerceIn(0f, (count - maxVisible).toFloat())
    }

    val pitch = PageIndicatorDefaults.DotSize + PageIndicatorDefaults.DotSpacing * 2f
    val width = pitch * (visibleCount - 1).toFloat() + PageIndicatorDefaults.DotSize
    val description = contentDescription ?: "${activeIndex + 1} / $count"

    Canvas(
        modifier = modifier
            .size(width = width, height = PageIndicatorDefaults.DotSize)
            .semantics { this.contentDescription = description },
    ) {
        val pitchPx = pitch.toPx()
        val dotPx = PageIndicatorDefaults.DotSize.toPx()
        val centreY = size.height / 2f
        val firstSlot = windowStart.roundToInt() - PageIndicatorDefaults.LADDER_SLOTS
        val lastSlot = (windowStart + visibleCount - 1).roundToInt() + PageIndicatorDefaults.LADDER_SLOTS
        for (index in firstSlot..lastSlot) {
            val slot = index - windowStart
            val centerX = slot * pitchPx + dotPx / 2f
            // How far the dot's centre has left the fixed row of dots: a dot walking out of the
            // window shrinks along the size ladder and fades to nothing.
            val overshootPx = maxOf(0f, -centerX, centerX - (size.width - dotPx / 2f))
            val radius = PageIndicatorDefaults.ladderRadius(
                overshoot = overshootPx,
                pitch = pitchPx,
                normal = dotPx / 2f,
                medium = PageIndicatorDefaults.DotSizeMedium.toPx() / 2f,
                small = PageIndicatorDefaults.DotSizeSmall.toPx() / 2f,
            )
            if (radius <= 0f) continue
            drawCircle(
                color = lerp(
                    inactiveColor,
                    activeColor,
                    PageIndicatorDefaults.dotActiveFraction(index, currentPosition),
                ),
                radius = radius,
                center = Offset(centerX, centreY),
            )
        }
    }
}

/** Binary-compatibility bridge for the [PageIndicator] overload that predates `contentDescription`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds contentDescription.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun PageIndicator(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = COUITheme.colorScheme.onSurface,
    inactiveColor: Color = COUITheme.colorScheme.disabledOnSurface,
) {
    PageIndicator(
        count = count,
        currentIndex = currentIndex,
        modifier = modifier,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        contentDescription = null,
    )
}

/** Contains default values used by [PageIndicator]. */
object PageIndicatorDefaults {

    /** The diameter of a dot, 6.0dp. */
    val DotSize = 6.dp

    /** The diameter of a dot one rung down the ladder, 4.5dp (COUI `dotSizeMedium`). */
    val DotSizeMedium = 4.5.dp

    /** The diameter of a dot two rungs down the ladder, 2.5dp (COUI `dotSizeSmall`). */
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

    /** The stiffness of the position spring (COUIPageIndicator initSpring). */
    const val POSITION_STIFFNESS = 1500f

    /** The minimum visible change of the position spring (`DEFAULT_MINIMUM_VISIBLE_CHANGE`). */
    const val MINIMUM_VISIBLE_CHANGE = 0.005f

    /** The trace colour cross-fade threshold (`DEFAULT_COLOR_FACTOR_THRESHOLD`). */
    const val COLOR_FACTOR_THRESHOLD = 0.05f

    /**
     * How much of the active colour the dot at [index] takes while the indicator sits at
     * [position], from 0 (inactive) to 1 (active).
     *
     * Mirrors COUIPageIndicator.drawTrace: the half of the trace nearer the current offset is always
     * painted in the full trace colour and only the other half cross-fades through getColorFactor,
     * so the dot of a page at rest is fully active.
     */
    internal fun dotActiveFraction(index: Int, position: Float): Float {
        val left = floor(position).toInt()
        val offset = position - left
        val colorFactor = when {
            offset <= COLOR_FACTOR_THRESHOLD -> offset / COLOR_FACTOR_THRESHOLD
            offset >= 1f - COLOR_FACTOR_THRESHOLD -> (1f - offset) / COLOR_FACTOR_THRESHOLD
            else -> 1f
        }.coerceIn(0f, 1f)
        return when (index) {
            left -> if (offset <= 0.5f) 1f else colorFactor
            left + 1 -> if (offset <= 0.5f) colorFactor else 1f
            else -> 0f
        }
    }

    /**
     * The number of dot slots a dot can travel outside the row before it is gone, i.e. the depth of
     * the four rung size ladder (COUIPageIndicator `INDICATOR_DOT_LEVEL`).
     */
    const val LADDER_SLOTS = 3

    /**
     * The radius of a dot whose centre is [overshoot] pixels outside the row, following
     * COUIPageIndicator's size ladder: a full size dot turns into
     * [DotSizeMedium], then [DotSizeSmall], then disappears, with `COUIEaseInterpolator` between
     * the rungs.
     */
    internal fun ladderRadius(overshoot: Float, pitch: Float, normal: Float, medium: Float, small: Float): Float {
        val rung = overshoot / pitch
        return when {
            rung <= 0f -> normal
            rung <= 1f -> COUIEase.transform(rung).let { normal + (medium - normal) * it }
            rung <= 2f -> COUIEase.transform(rung - 1f).let { medium + (small - medium) * it }
            rung <= LADDER_SLOTS.toFloat() -> COUIEase.transform(rung - 2f).let { small * (1f - it) }
            else -> 0f
        }
    }
}
