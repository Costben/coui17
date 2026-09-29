// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/**
 * The ColorOS wrapping tag container (`COUIFlowLayout`,
 * `com/coui/appcompat/searchhistory/COUIFlowLayout`), the row-major flow layout that lays out the
 * chips of a [SearchHistoryView].
 *
 * Placement repeats the native `Line.canAdd` / `Line.addView`:
 * `usedWidth + itemSpacing + childWidth <= maxWidth`, with the first child of a line always
 * accepted so a chip wider than the container still gets a line of its own. A line is as tall as its
 * tallest child (`Line.addView` keeps `height = max(childHeight, height)`), the whole block is
 * `sum(lineHeights) + lineSpacing * (lineCount - 1)` (`calculateLinesHeight`), and children are
 * placed at the line's top edge, which is exactly where the native `Line.layout` puts them
 * (`view.layout(left, offsetTop, right, offsetTop + measuredHeight)`). Horizontal placement uses
 * [Placeable.placeRelative], mirroring the native `isRtl` branch that lays a child out at
 * `maxWidth - offsetLeft - childWidth`.
 *
 * Folding is the one place this port departs from the native implementation. The native keeps every
 * chip laid out, then hides the chips of the rows past the fold with `View.INVISIBLE` plus an alpha
 * animation, and its `drawChild` refuses to draw anything below the container's folded height. Here
 * the children of the lines past [maxRows] are measured but simply **not placed**, so they are never
 * drawn and never take part in the height — the folded height is the same sum the native computes
 * over `lines.take(maxRowFolded)`.
 *
 * @param modifier The modifier to be applied to the flow layout.
 * @param itemSpacing `attr/itemSpacing` — the horizontal gap between two chips on one line. The
 *   attribute default is 0; [SearchHistoryView] sets 8dp
 *   (`coui_component_search_history_flow_item_spacing`).
 * @param lineSpacing `attr/lineSpacing` — the vertical gap between two lines. The attribute default
 *   is 0; [SearchHistoryView] sets 8dp (`coui_component_search_history_flow_line_spacing`).
 * @param maxRows The number of lines to place. Lines past this are dropped, which is the native
 *   `maxRowFolded` / `maxRowUnfolded` fold. A value of 0 or less means no limit.
 * @param content The chips to flow.
 */
@Composable
fun FlowLayout(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = FlowLayoutDefaults.ItemSpacing,
    lineSpacing: Dp = FlowLayoutDefaults.LineSpacing,
    maxRows: Int = FlowLayoutDefaults.MaxRows,
    content: @Composable () -> Unit,
) {
    val itemSpacingPx = with(LocalDensity.current) { itemSpacing.roundToPx() }
    val lineSpacingPx = with(LocalDensity.current) { lineSpacing.roundToPx() }

    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

        val lines = mutableListOf<MutableList<Placeable>>()
        var line = mutableListOf<Placeable>()
        var usedWidth = 0L
        placeables.forEach { placeable ->
            val needed = if (line.isEmpty()) {
                placeable.width.toLong()
            } else {
                usedWidth + itemSpacingPx + placeable.width
            }
            if (line.isNotEmpty() && needed > maxWidth) {
                lines.add(line)
                line = mutableListOf(placeable)
                usedWidth = placeable.width.toLong()
            } else {
                line.add(placeable)
                usedWidth = needed
            }
        }
        if (line.isNotEmpty()) lines.add(line)

        val visibleLines = if (maxRows <= 0) lines else lines.take(maxRows)

        var contentHeight = 0
        visibleLines.forEachIndexed { index, visibleLine ->
            if (index > 0) contentHeight += lineSpacingPx
            contentHeight += visibleLine.maxOf { it.height }
        }

        val intrinsicWidth = visibleLines.maxOfOrNull { visibleLine ->
            visibleLine.sumOf { it.width } + itemSpacingPx * (visibleLine.size - 1)
        } ?: 0
        val layoutWidth = constraints.constrainWidth(
            if (constraints.hasBoundedWidth) constraints.maxWidth else intrinsicWidth,
        )

        layout(layoutWidth, constraints.constrainHeight(contentHeight)) {
            var y = 0
            visibleLines.forEach { visibleLine ->
                var x = 0
                visibleLine.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + itemSpacingPx
                }
                y += visibleLine.maxOf { it.height } + lineSpacingPx
            }
        }
    }
}

/** Contains the default values used by [FlowLayout]. */
object FlowLayoutDefaults {
    /**
     * `attr/COUIFlowLayout_itemSpacing` — `COUIFlowLayout` reads it with a default of 0
     * (`obtainStyledAttributes(...).getDimensionPixelOffset(R.styleable.COUIFlowLayout_itemSpacing, 0)`).
     */
    val ItemSpacing = 0.dp

    /** `attr/COUIFlowLayout_lineSpacing` — read with a default of 0, same as [ItemSpacing]. */
    val LineSpacing = 0.dp

    /**
     * `attr/COUIFlowLayout_maxRowFolded` / `_maxRowUnfolded` both default to `Integer.MAX_VALUE`,
     * i.e. unbounded. [SearchHistoryView] narrows them to its own defaults.
     */
    val MaxRows = Int.MAX_VALUE
}
