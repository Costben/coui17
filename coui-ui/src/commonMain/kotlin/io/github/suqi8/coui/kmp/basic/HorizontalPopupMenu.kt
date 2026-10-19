// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The ColorOS 17 horizontal icon menu block of a popup list.
 *
 * COUI carries this block inside the same RecyclerView as the vertical rows — `PopupListItem`
 * items with `getItemType() == 4` become `MixedPopupListAdapter.HorizontalViewHolder`s and the
 * window swaps in a `PopupMenuNoPredictiveGridLayoutManager` whose span count comes from
 * `COUIPopupListWindow.createMenuGridLayoutManager`: 3 columns when any horizontal item has a
 * title, otherwise `max(3, min(5, count))`. `SpanSizeLookup` gives each horizontal item one span
 * and every other item type the full width.
 *
 * Compose this as a block inside the popup content, next to the vertical rows, e.g.
 * `ListPopupColumn { HorizontalPopupMenu { … }; DropdownImpl(…) }`. Column count, item widths and
 * the per-item margins are derived from the children exactly as COUI derives them from the
 * adapter, so the block fills the popup width with equal columns.
 *
 * Two deliberate differences from the native adapter, both in the direction of not discarding the
 * caller's content:
 *  - COUI drops horizontal items beyond the column limit (`setItemList: Horizontal items exceed
 *    limit! … only first N items will be displayed`). Compose cannot un-compose children, so the
 *    excess wraps onto a second grid row instead.
 *  - The 12dp divider band COUI inserts after the block (`MixedPopupListAdapter.DividerViewHolder`)
 *    is owned by [ListPopupColumn]'s measure policy, which only bands adjacent registered rows.
 *
 * @param modifier The modifier to be applied to the block.
 * @param content The [HorizontalPopupMenuItem]s; they are laid out in composition order.
 */
@Composable
fun HorizontalPopupMenu(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = HorizontalPopupMenuMeasurePolicy,
    )
}

/**
 * One square button of a [HorizontalPopupMenu]: icon on top, optional label underneath.
 *
 * `res/layout/coui_popup_list_window_horizontal_item.xml` is a centred vertical `LinearLayout` with
 * `coui_popup_list_window_horizontal_item_text_padding_*` and an `ImageView` over a `TextView`.
 * `MixedPopupListAdapter.HorizontalViewHolder.bind` then re-sizes the `ImageView` per shape (20dp
 * with a label, 24dp without), sets `coui_popup_list_window_horizontal_item_icon_title_gap` as the
 * label's top margin, hides the label entirely in icon-only shape, and applies the item's own
 * padding through `configHorizontalItemLayout`.
 *
 * The label is `?couiTextBodyXS` at `?couiColorLabelPrimary`, `maxLines = 2`, centred.
 *
 * The press mask is a smooth rounded rectangle over the whole item — `configureHorizontalItemMaskPath`
 * builds it from the full `view` rect, unlike the vertical rows, which inset it by
 * `coui_popup_list_window_mask_margin_horizontal`. On this OS generation it uses radius 17dp
 * (icon-only) / 23dp (with label) at `coui_popup_list_window_horizontal_mask_weight_16_1` = 3.0; the
 * retired arc radii 12dp / 16dp are the `OS <= 37` fallback.
 *
 * The item is measured with an exact width by [HorizontalPopupMenu]; the caller does not size it.
 *
 * @param icon The icon drawn above the label.
 * @param onClick Invoked when the item is activated.
 * @param modifier The modifier to be applied to the item.
 * @param label The label drawn under the icon. `null` or empty selects the icon-only shape.
 * @param enabled Whether the item reacts to input.
 * @param contentDescription Accessible name for an icon-only item; the label names the other shape.
 */
@Composable
fun HorizontalPopupMenuItem(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val hasText = !label.isNullOrEmpty()
    val colorScheme = COUITheme.colorScheme
    val squircleEnabled = isSquircleEnabled()
    val contentColor = if (enabled) colorScheme.onSurface else colorScheme.disabledOnSurface
    val maskColor = if (colorScheme.background.luminance() < 0.5f) PopupItemPressDark else PopupItemPressLight
    val maskRadius = when {
        hasText && squircleEnabled -> HorizontalPopupMenuDefaults.MaskRadiusWithTextSmooth
        hasText -> HorizontalPopupMenuDefaults.MaskRadiusWithText
        squircleEnabled -> HorizontalPopupMenuDefaults.MaskRadiusIconOnlySmooth
        else -> HorizontalPopupMenuDefaults.MaskRadiusIconOnly
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressProgress = remember { Animatable(0f) }

    // The row press mask and its spring are shared with the vertical rows, so a horizontal button
    // flashes and holds exactly like a list row (COUI drives both through COUIMaskEffectDrawable).
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> launch { pressProgress.animateTo(1f, PopupItemPressSpring) }

                is PressInteraction.Release, is PressInteraction.Cancel ->
                    launch { pressProgress.animateTo(0f, PopupItemPressSpring) }
            }
        }
    }

    Column(
        modifier = modifier
            .then(remember(hasText) { HorizontalPopupMenuParentData(hasText) })
            .drawWithCache {
                val radius = maskRadius.toPx()
                // configureHorizontalItemMaskPath clamps the mask rect to the icon-only height.
                val iconOnlyHeight = HorizontalPopupMenuDefaults.IconOnlySize.toPx()
                val maskHeight = if (hasText) size.height else min(size.height, iconOnlyHeight)
                val path = Path()
                onDrawBehind {
                    val progress = pressProgress.value
                    if (progress <= 0f) return@onDrawBehind
                    path.rewind()
                    path.addSquircleRect(
                        width = size.width,
                        height = maskHeight,
                        cornerRadius = radius,
                        squircleEnabled = squircleEnabled,
                    )
                    drawPath(path, color = maskColor, alpha = progress.coerceIn(0f, 1f))
                }
            }
            .selectable(
                selected = false,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .heightIn(min = HorizontalPopupMenuDefaults.TextMinHeight)
            .padding(
                if (hasText) {
                    PaddingValues(
                        horizontal = HorizontalPopupMenuDefaults.TextPaddingHorizontal,
                        vertical = HorizontalPopupMenuDefaults.TextPaddingVertical,
                    )
                } else {
                    PaddingValues(horizontal = HorizontalPopupMenuDefaults.IconOnlyPaddingHorizontal)
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (hasText) {
            Arrangement.spacedBy(HorizontalPopupMenuDefaults.IconTitleGap)
        } else {
            Arrangement.Center
        },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (hasText) null else contentDescription,
            modifier = Modifier.size(
                if (hasText) {
                    HorizontalPopupMenuDefaults.IconSizeWithText
                } else {
                    HorizontalPopupMenuDefaults.IconSizeIconOnly
                },
            ),
            tint = contentColor,
        )
        if (hasText) {
            Text(
                text = label,
                color = contentColor,
                fontSize = HorizontalPopupMenuDefaults.LabelFontSize,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Contains default values used by [HorizontalPopupMenu] and [HorizontalPopupMenuItem]. */
object HorizontalPopupMenuDefaults {

    /**
     * Columns when at least one item carries a label (COUI
     * `COUIPopupListWindow.HORIZONTAL_ITEM_COUNT_WITH_TEXT`).
     */
    val ColumnsWithText = 3

    /**
     * Columns of an icon-only block at most, and its floor as well: `createMenuGridLayoutManager`
     * resolves `Math.max(3, Math.min(5, count))` (COUI `HORIZONTAL_ITEM_COUNT`).
     */
    val MaxIconOnlyColumns = 5

    /** Columns of an icon-only block at least (the `Math.max(3, …)` above). */
    val MinColumns = 3

    /** Minimum width of one column (COUI `coui_popup_list_window_horizontal_item_min_width`). */
    val MinColumnWidth = 52.dp

    /** Height of an icon-only item, which is also its width floor (COUI `…_icon_only_width` / `…_icon_only_height`). */
    val IconOnlySize = 40.dp

    /** Minimum height of an item with a label (COUI `coui_popup_list_window_horizontal_item_text_min_height`). */
    val TextMinHeight = 40.dp

    /** Horizontal padding of an item with a label (COUI `…_text_padding_horizontal`). */
    val TextPaddingHorizontal = 6.dp

    /** Vertical padding of an item with a label (COUI `…_text_padding_vertical`). */
    val TextPaddingVertical = 6.dp

    /** Horizontal padding of an icon-only item (COUI `…_icon_only_padding_horizontal`). */
    val IconOnlyPaddingHorizontal = 8.dp

    /** Icon size of an item with a label (COUI `…_icon_size_with_text`). */
    val IconSizeWithText = 20.dp

    /** Icon size of an icon-only item (COUI `…_icon_size_icon_only`). */
    val IconSizeIconOnly = 24.dp

    /** Gap between the icon and the label (COUI `…_icon_title_gap`, applied as the label's top margin). */
    val IconTitleGap = 4.dp

    /** Gap between two labelled items (COUI `…_text_spacing_default`). */
    val TextSpacing = 4.dp

    /** Gap between two icon-only items (COUI `…_icon_only_spacing_default`). */
    val IconOnlySpacing = 8.dp

    /**
     * Floor the container-derived gap may not fall below (COUI `…_horizontal_item_spacing_min`);
     * below it the items are packed and the edges absorb the remainder.
     */
    val SpacingMin = 4.dp

    /** Leading and trailing margin of a labelled block (COUI `…_text_edge_margin`). */
    val TextEdgeMargin = 8.dp

    /** Leading and trailing margin of an icon-only block (COUI `…_icon_only_edge_margin`). */
    val IconOnlyEdgeMargin = 12.dp

    /** Press mask radius of an icon-only item, `OS <= 37` arc (COUI `…_horizontal_mask_radius_icon_only`). */
    val MaskRadiusIconOnly = 12.dp

    /** Press mask radius of an icon-only item on the smooth-corner generation (COUI `…_icon_only_16_1`). */
    val MaskRadiusIconOnlySmooth = 17.dp

    /** Press mask radius of a labelled item, `OS <= 37` arc (COUI `…_horizontal_mask_radius_with_text`). */
    val MaskRadiusWithText = 16.dp

    /** Press mask radius of a labelled item on the smooth-corner generation (COUI `…_with_text_16_1`). */
    val MaskRadiusWithTextSmooth = 23.dp

    /** The label size. COUI `?couiTextBodyXS` (COUI couiTextBodyXS: 12sp). */
    val LabelFontSize: TextUnit = 12.sp
}

/**
 * Reports whether an item carries a label, so the measure policy can pick the column count and the
 * per-shape metrics the same way `MixedPopupListAdapter.setItemList` counts titles.
 */
private class HorizontalPopupMenuParentData(val hasText: Boolean) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@HorizontalPopupMenuParentData
}

private val IntrinsicMeasurable.horizontalItemHasText: Boolean
    get() = (parentData as? HorizontalPopupMenuParentData)?.hasText == true

/**
 * Lays the items out as a COUI popup grid: equal columns across the block's width, with the leading,
 * trailing and inter-item margins `MixedPopupListAdapter.configHorizontalItemLayout` distributes.
 *
 * That method first caps the default edge margin and gap to what the container can afford
 * (`spacing = clamp(spacingDefault, spacingMin, (width - 2 * edge) / (count - 1))`, then
 * `edge = min(edge, (width - (count - 1) * spacing) / 2)`), and then splits the leftover of the
 * integer division between an item's two sides: `start = round((edge * n - index * (2 * edge -
 * spacing)) / n)` and `end = round(((n - 1) * spacing + 2 * edge) / n) - start`. The item width is
 * therefore `columnWidth - start - end`, which lands every icon-only column on its 40dp square.
 */
private object HorizontalPopupMenuMeasurePolicy : MeasurePolicy {

    private fun columnCount(count: Int, hasText: Boolean): Int = when {
        hasText -> HorizontalPopupMenuDefaults.ColumnsWithText
        else -> count.coerceIn(HorizontalPopupMenuDefaults.MinColumns, HorizontalPopupMenuDefaults.MaxIconOnlyColumns)
    }

    private fun rowCount(count: Int, columns: Int): Int = (count + columns - 1) / columns

    /** Leading and trailing margin of the block: 8dp once a label is present, 12dp when icon-only. */
    private fun edgeMargin(hasText: Boolean): Dp = if (hasText) {
        HorizontalPopupMenuDefaults.TextEdgeMargin
    } else {
        HorizontalPopupMenuDefaults.IconOnlyEdgeMargin
    }

    /** Gap between two items: 4dp once a label is present, 8dp when icon-only. */
    private fun spacingDefault(hasText: Boolean): Dp = if (hasText) {
        HorizontalPopupMenuDefaults.TextSpacing
    } else {
        HorizontalPopupMenuDefaults.IconOnlySpacing
    }

    /** Width one column needs for the widest item, before the edges and gaps are added. */
    private fun Density.perColumnWidth(
        measurables: List<IntrinsicMeasurable>,
        hasText: Boolean,
    ): Int {
        var width = if (hasText) {
            HorizontalPopupMenuDefaults.MinColumnWidth.roundToPx()
        } else {
            HorizontalPopupMenuDefaults.IconOnlySize.roundToPx()
        }
        for (measurable in measurables) {
            width = max(width, measurable.maxIntrinsicWidth(0))
        }
        return width
    }

    private fun Density.gridWidth(
        measurables: List<IntrinsicMeasurable>,
        columns: Int,
        hasText: Boolean,
    ): Int {
        val perColumn = perColumnWidth(measurables, hasText)
        val edge = edgeMargin(hasText).roundToPx()
        val spacing = spacingDefault(hasText).roundToPx()
        return columns * perColumn + 2 * edge + (columns - 1) * spacing
    }

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val count = measurables.size
        if (count == 0) return layout(0, 0) {}

        val hasText = measurables.any { it.horizontalItemHasText }
        val columns = columnCount(count, hasText)
        val rows = rowCount(count, columns)

        val defaultEdge = edgeMargin(hasText).roundToPx()
        val defaultSpacing = spacingDefault(hasText).roundToPx()
        val spacingMin = HorizontalPopupMenuDefaults.SpacingMin.roundToPx()

        // The block is the popup's widest child, so it sizes itself the way ListPopupColumn sizes
        // the popup: its own intrinsic width clamped into the window's [min, max] band.
        val minPx = ListPopupDefaults.MinWidth.roundToPx()
        val maxPx = ListPopupDefaults.MaxWidth.roundToPx()
        val intrinsic = gridWidth(measurables, columns, hasText)
        val upper = max(maxPx, constraints.minWidth).coerceAtMost(constraints.maxWidth)
        val lower = max(minPx, constraints.minWidth).coerceAtMost(upper)
        val width = intrinsic.coerceIn(lower, upper)
        val columnWidth = width / columns

        val minItemHeight = if (hasText) HorizontalPopupMenuDefaults.TextMinHeight.roundToPx() else 0
        val fixedItemHeight = HorizontalPopupMenuDefaults.IconOnlySize.roundToPx()

        val placeables = ArrayList<Placeable>(count)
        val offsets = ArrayList<IntOffset>(count)
        var index = 0
        var y = 0

        for (row in 0 until rows) {
            val rowCount = min(columns, count - row * columns)
            var edge = defaultEdge
            var spacing = if (rowCount <= 1) 0 else defaultSpacing
            if (rowCount > 1) {
                val gaps = rowCount - 1
                val affordable = (width - 2 * edge) / gaps
                val capped = min(defaultSpacing, affordable)
                spacing = if (affordable >= spacingMin) max(spacingMin, capped) else max(0, capped)
                edge = min(edge, max(0, (width - gaps * spacing) / 2))
            }
            val perItem = if (rowCount <= 1) {
                edge
            } else {
                (((rowCount - 1) * spacing + 2 * edge).toFloat() / rowCount).roundToInt()
            }

            val rowPlaceables = ArrayList<Placeable>(rowCount)
            val rowX = ArrayList<Int>(rowCount)
            var rowHeight = 0
            for (column in 0 until rowCount) {
                val start = if (rowCount <= 1) {
                    edge
                } else {
                    max(
                        0,
                        ((edge * rowCount - column * (2 * edge - spacing)).toFloat() / rowCount).roundToInt(),
                    )
                }
                val end = if (rowCount <= 1) edge else max(0, perItem - start)
                val itemWidth = (columnWidth - start - end).coerceAtLeast(0)
                val placeable = measurables[index + column].measure(
                    Constraints(
                        minWidth = itemWidth,
                        maxWidth = itemWidth,
                        minHeight = minItemHeight,
                        maxHeight = if (hasText) Constraints.Infinity else fixedItemHeight,
                    ),
                )
                rowPlaceables.add(placeable)
                rowX.add(column * columnWidth + start)
                rowHeight = max(rowHeight, placeable.height)
            }

            for (column in 0 until rowCount) {
                placeables.add(rowPlaceables[column])
                offsets.add(IntOffset(rowX[column], y))
            }
            index += rowCount
            y += rowHeight
        }

        return layout(width, y) {
            for (i in placeables.indices) {
                placeables[i].place(offsets[i])
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = gridWidth(
        measurables = measurables,
        columns = columnCount(measurables.size, measurables.any { it.horizontalItemHasText }),
        hasText = measurables.any { it.horizontalItemHasText },
    )

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = minIntrinsicWidth(measurables, height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = intrinsicHeight(measurables, width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = intrinsicHeight(measurables, width)

    private fun Density.intrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int {
        if (measurables.isEmpty()) return 0
        val hasText = measurables.any { it.horizontalItemHasText }
        val columns = columnCount(measurables.size, hasText)
        val rows = rowCount(measurables.size, columns)
        val rowHeight = if (hasText) {
            measurables.maxOf { it.minIntrinsicHeight(width) }
        } else {
            HorizontalPopupMenuDefaults.IconOnlySize.roundToPx()
        }
        return rows * rowHeight
    }
}
