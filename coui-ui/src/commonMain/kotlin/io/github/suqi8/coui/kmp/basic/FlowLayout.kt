// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.COUIMoveEase
import kotlinx.coroutines.delay

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
 * `Placeable.placeRelative`, mirroring the native `isRtl` branch that lays a child out at
 * `maxWidth - offsetLeft - childWidth`.
 *
 * Folding keeps the native's state machine: [expandable] turns the fold on, [maxRowFolded] and
 * [maxRowUnfolded] are the two limits, and `isExpand` drives `doExpand` / `doFold`. A fold is only
 * offered when the chips need more than [maxRowFolded] lines; then [expandButton] is appended to the
 * last folded line and [foldButton] to the last line of the expanded layout, exactly as
 * `ensureAndExpandButton` inserts `expandButton` / `foldButton` as ordinary children before measuring.
 *
 * The transition runs the native's animators: expanding waits
 * [FlowLayoutDefaults.HiddenViewsAppearDelayMillis] (100ms), then grows the container over
 * [FlowLayoutDefaults.HiddenViewsAppearDurationMillis] (400ms), and the button fades over
 * [FlowLayoutDefaults.ExpandButtonAppearDurationMillis] (250ms); folding runs
 * [FlowLayoutDefaults.HiddenViewsDisappearDurationMillis] (300ms). All of them use
 * `COUIMoveEaseInterpolator`.
 *
 * @param modifier The modifier to be applied to the flow layout.
 * @param itemSpacing `attr/itemSpacing` — the horizontal gap between two chips on one line. The
 *   attribute default is 0; [SearchHistoryView] sets 8dp
 *   (`coui_component_search_history_flow_item_spacing`).
 * @param lineSpacing `attr/lineSpacing` — the vertical gap between two lines. The attribute default
 *   is 0; [SearchHistoryView] sets 8dp (`coui_component_search_history_flow_line_spacing`).
 * @param maxRows The number of lines to place. Lines past this are dropped. A value of 0 or less
 *   means no limit.
 * @param expanded Whether the chips past the fold are shown (`isExpand`). The layout keeps this
 *   itself unless [onExpandedChange] is supplied.
 * @param onExpandedChange Called with the new state when the expand/fold button is clicked.
 * @param expandable Whether folding is available at all (`attr/expandable`, default true).
 * @param maxRowFolded The folded line limit (`attr/maxRowFolded`). Falls back to [maxRows].
 * @param maxRowUnfolded The expanded line limit (`attr/maxRowUnfolded`). Falls back to [maxRows].
 * @param expandButton The button appended to the last folded line, or null for none. It receives the
 *   action that expands the layout, like the native button's own click listener.
 * @param foldButton The button appended to the last expanded line, or null for none. It receives the
 *   action that folds the layout.
 * @param content The chips to flow.
 */
@Composable
fun FlowLayout(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = FlowLayoutDefaults.ItemSpacing,
    lineSpacing: Dp = FlowLayoutDefaults.LineSpacing,
    maxRows: Int = FlowLayoutDefaults.MaxRows,
    expanded: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    expandable: Boolean = true,
    maxRowFolded: Int = FlowLayoutDefaults.MaxRowFolded,
    maxRowUnfolded: Int = FlowLayoutDefaults.MaxRowUnfolded,
    expandButton: (@Composable (onClick: () -> Unit) -> Unit)? = null,
    foldButton: (@Composable (onClick: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val itemSpacingPx = with(LocalDensity.current) { itemSpacing.roundToPx() }
    val lineSpacingPx = with(LocalDensity.current) { lineSpacing.roundToPx() }

    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    // COUIFlowLayout.isExpand: the layout owns the state unless the caller takes it over.
    var internalExpanded by remember { mutableStateOf(expanded) }
    val isExpanded = if (onExpandedChange == null) internalExpanded else expanded
    val toggle: () -> Unit = remember(onExpandedChange) {
        {
            if (onExpandedChange == null) internalExpanded = !internalExpanded
            currentOnExpandedChange?.invoke(!isExpanded)
        }
    }

    // The native's reveal/fold animators, driven off a single 0..1 progress.
    val progress = remember { Animatable(if (isExpanded) 1f else 0f) }
    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) 1f else 0f
        if (progress.value == target) return@LaunchedEffect
        if (isExpanded) {
            delay(FlowLayoutDefaults.HiddenViewsAppearDelayMillis.toLong())
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    FlowLayoutDefaults.HiddenViewsAppearDurationMillis,
                    easing = COUIMoveEase,
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    FlowLayoutDefaults.HiddenViewsDisappearDurationMillis,
                    easing = COUIMoveEase,
                ),
            )
        }
    }

    val foldedRows = when {
        !expandable -> Int.MAX_VALUE
        maxRowFolded != Int.MAX_VALUE -> maxRowFolded
        else -> maxRows
    }
    val unfoldedRows = if (maxRowUnfolded != Int.MAX_VALUE) maxRowUnfolded else maxRows

    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            content()
            // The button is an ordinary child appended last, so it takes part in the line breaking
            // exactly like the view the native inserts.
            if (expandable) {
                if (isExpanded) foldButton?.invoke(toggle) else expandButton?.invoke(toggle)
            }
        },
    ) { measurables, constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val buttonMeasurable = if (expandable) measurables.lastOrNull() else null
        val chipMeasurables = if (expandable) measurables.dropLast(1) else measurables

        val chips = chipMeasurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val button = buttonMeasurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))

        fun buildLines(items: List<Placeable>): MutableList<MutableList<Placeable>> {
            val lines = mutableListOf<MutableList<Placeable>>()
            var line = mutableListOf<Placeable>()
            var usedWidth = 0L
            items.forEach { placeable ->
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
            return lines
        }

        val chipLines = buildLines(chips)
        // `isNeedExpandButton`: only offer a fold when the chips really need more than the folded
        // limit, and offer the fold button once the extra rows are showing.
        val showButton = expandable && button != null && (isExpanded || chipLines.size > foldedRows)
        val lines = if (showButton) {
            val built = buildLines(chips)
            val last = built.lastOrNull()
            val used = last?.sumOf { it.width }?.let { it + itemSpacingPx * last.size } ?: 0
            if (last != null && used + itemSpacingPx + button!!.width <= maxWidth) {
                last.add(button)
            } else {
                built.add(mutableListOf(button!!))
            }
            built
        } else {
            chipLines
        }

        fun heightOf(count: Int): Int {
            if (lines.isEmpty()) return 0
            val visible = lines.take(count.coerceIn(1, lines.size))
            return visible.sumOf { line -> line.maxOf { it.height } } + lineSpacingPx * (visible.size - 1)
        }

        val foldedHeight = heightOf(foldedRows)
        val unfoldedHeight = heightOf(unfoldedRows)
        val animatedHeight = foldedHeight + (unfoldedHeight - foldedHeight) * progress.value

        val intrinsicWidth = lines.maxOfOrNull { line ->
            line.sumOf { it.width } + itemSpacingPx * (line.size - 1)
        } ?: 0
        val layoutWidth = constraints.constrainWidth(
            if (constraints.hasBoundedWidth) constraints.maxWidth else intrinsicWidth,
        )

        layout(layoutWidth, constraints.constrainHeight(animatedHeight.toInt())) {
            var y = 0
            lines.forEach { line ->
                if (y >= animatedHeight) return@forEach
                var x = 0
                line.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + itemSpacingPx
                }
                y += line.maxOf { it.height } + lineSpacingPx
            }
        }
    }
}

/** Binary-compatibility bridge for the [FlowLayout] overload that predates the fold state machine. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the expandable fold state machine and buttons.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun FlowLayout(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = FlowLayoutDefaults.ItemSpacing,
    lineSpacing: Dp = FlowLayoutDefaults.LineSpacing,
    maxRows: Int = FlowLayoutDefaults.MaxRows,
    content: @Composable () -> Unit,
) {
    FlowLayout(
        modifier = modifier,
        itemSpacing = itemSpacing,
        lineSpacing = lineSpacing,
        maxRows = maxRows,
        expanded = true,
        expandable = false,
        content = content,
    )
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

    /** `attr/COUIFlowLayout_maxRowFolded`, `Integer.MAX_VALUE` by default. */
    val MaxRowFolded = Int.MAX_VALUE

    /** `attr/COUIFlowLayout_maxRowUnfolded`, `Integer.MAX_VALUE` by default. */
    val MaxRowUnfolded = Int.MAX_VALUE

    /** `HIDDEN_VIEWS_APPEAR_DELAY`: how long an expand waits before the rows start to appear. */
    val HiddenViewsAppearDelayMillis = 100

    /** `HIDDEN_VIEWS_APPEAR_DURATION`: the expand transition, 400ms. */
    val HiddenViewsAppearDurationMillis = 400

    /** `HIDDEN_VIEWS_DISAPPEAR_DURATION`: the fold transition, 300ms. */
    val HiddenViewsDisappearDurationMillis = 300

    /** `EXPAND_BUTTON_APPEAR_DURATION`: the button's own fade, 250ms. */
    val ExpandButtonAppearDurationMillis = 250
}
