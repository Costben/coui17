// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

/**
 * The ColorOS 17 search-history panel (`COUISearchHistoryView`,
 * `com/coui/appcompat/searchhistory/COUISearchHistoryView`), the block that sits under a search
 * field and shows the user's recent queries as a wrapping field of chips.
 *
 * It is three pieces stacked vertically: a title bar, a [FlowLayout] of chips, and — when the chips
 * do not fit the fold — a chevron button that toggles between the folded and unfolded row counts.
 *
 * Geometry follows the native constructor: the panel is padded
 * `coui_component_search_history_padding_start` / `_top` / `_end` / `_bottom` (16 / 16 / 16 / 32dp;
 * start and end become 20dp from `w600dp` up), the title bar carries
 * `coui_component_search_history_title_margin_vertical` (7dp) of vertical padding and the flow
 * container is separated from it by `coui_component_search_history_flow_margin_top` (8dp). The chips
 * are 8dp apart horizontally (`_flow_item_spacing`) and 8dp apart between lines (`_flow_line_spacing`).
 *
 * The title is a `coui_component_search_history_title` `COUITextView` with
 * `style/couiTextAppearanceButton` (14sp `sans-serif-medium`) coloured `?attr/couiColorPrimaryNeutral`
 * (`coui_color_primary_neutral`, light `#e6000000` / night `#e6ffffff` — the [COUITheme] `onSurface`
 * slot), aligned to the start of the bar. The delete icon is an 18dp
 * `drawable/coui_component_search_history_delete` vector tinted `?attr/couiColorSecondNeutral`
 * (`coui_color_secondary_neutral`, light `#8a000000` / night `#8affffff` — the `onSurfaceSecondary`
 * slot), aligned to the end with a `coui_component_search_history_delete_icon_end_margin` (0dp)
 * trailing margin; its ripple background (`drawable/coui_component_icon_bg`) is not ported.
 *
 * Clicking the delete icon runs the native `doDeleteAnimation`: the whole panel fades to alpha 0 over
 * `SEARCH_VIEW_DISAPPEAR_DURATION` (300ms) with `COUIMoveEaseInterpolator`
 * (`cubic-bezier(0.3, 0, 0.1, 1)`), and only then is [onDeleteClick] invoked. The panel stays
 * transparent afterwards, exactly like the native view — the host is expected to drop the panel or
 * repopulate it.
 *
 * @param items The recent searches, oldest to newest. Each becomes an unchecked [Chip] laid out by
 *   [FlowLayout].
 * @param onItemClick Called with the chip's text when a chip is clicked, matching
 *   `COUIFlowLayout.OnItemClickListener.onItemClick`.
 * @param onDeleteClick Called after the delete animation finishes, matching the native
 *   `setOnDeleteClickListener` ordering.
 * @param modifier The modifier to be applied to the panel.
 * @param title The title bar text, `attr/COUISearchHistoryView_title`. The native default is the
 *   empty string.
 * @param expandable `attr/COUISearchHistoryView_expandable`, native default true. When false the
 *   panel is always folded to [maxRowFolded] rows and the chevron button is not drawn.
 * @param maxRowFolded `attr/COUISearchHistoryView_maxRowFolded`, native default
 *   `Integer.MAX_VALUE`.
 * @param maxRowUnfolded The row count the panel unfolds to. The native constructor hard-codes this to
 *   `COUISearchHistoryView.DEFAULT_MAX_ROW_UNFOLDED` = 8 rows.
 * @param deleteIcon The image drawn in the delete button. Defaults to the ported
 *   `drawable/coui_component_search_history_delete` vector.
 * @param deleteIconDescription `attr/COUISearchHistoryView_deleteIconDescription`, the accessibility
 *   label of the delete button. The native default is null.
 * @param colors The [SearchHistoryViewColors].
 */
@Composable
fun SearchHistoryView(
    items: List<String>,
    onItemClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    expandable: Boolean = true,
    maxRowFolded: Int = SearchHistoryViewDefaults.MaxRowFolded,
    maxRowUnfolded: Int = SearchHistoryViewDefaults.MaxRowUnfolded,
    deleteIcon: ImageVector = SearchHistoryDeleteIcon,
    deleteIconDescription: String? = null,
    colors: SearchHistoryViewColors = SearchHistoryViewDefaults.colors(),
) {
    val scope = rememberCoroutineScope()
    val viewAlpha = remember { Animatable(1f) }
    var expanded by remember { mutableStateOf(false) }

    // COUISearchHistoryView sets the flow container to maxRowUnfolded 8 and leaves maxRowFolded at
    // the attribute default; COUIFlowLayout.isNeedExpandButton + getMaxRow pick which one applies.
    val canExpand = expandable && maxRowFolded < maxRowUnfolded
    val maxRows = if (canExpand && expanded) maxRowUnfolded else maxRowFolded

    Column(
        modifier = modifier
            .graphicsLayer { alpha = viewAlpha.value }
            .padding(
                start = SearchHistoryViewDefaults.PaddingStart,
                top = SearchHistoryViewDefaults.PaddingTop,
                end = SearchHistoryViewDefaults.PaddingEnd,
                bottom = SearchHistoryViewDefaults.PaddingBottom,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SearchHistoryViewDefaults.TitleMarginVertical),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = colors.titleColor,
                style = SearchHistoryViewDefaults.titleTextStyle(),
            )
            Box(
                modifier = Modifier
                    .padding(end = SearchHistoryViewDefaults.DeleteIconEndMargin)
                    .size(SearchHistoryViewDefaults.DeleteIconSize)
                    .clickable {
                        scope.launch {
                            viewAlpha.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = DELETE_ANIMATION_DURATION_MILLIS,
                                    easing = MoveEase,
                                ),
                            )
                            onDeleteClick()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = deleteIcon,
                    contentDescription = deleteIconDescription,
                    modifier = Modifier.size(SearchHistoryViewDefaults.DeleteIconSize),
                    tint = colors.deleteIconColor,
                )
            }
        }

        FlowLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SearchHistoryViewDefaults.FlowMarginTop),
            itemSpacing = SearchHistoryViewDefaults.FlowItemSpacing,
            lineSpacing = SearchHistoryViewDefaults.FlowLineSpacing,
            maxRows = maxRows,
        ) {
            items.forEach { item ->
                Chip(
                    selected = false,
                    onClick = { onItemClick(item) },
                    label = item,
                    minWidth = 0.dp,
                    minHeight = SearchHistoryViewDefaults.ChipHeight,
                    cornerRadius = SearchHistoryViewDefaults.ChipCornerRadius,
                )
            }
        }

        if (canExpand) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(SearchHistoryViewDefaults.ExpandButtonSize)
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SearchHistoryExpandIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(SearchHistoryViewDefaults.ExpandIconSize)
                            .rotate(if (expanded) 180f else 0f),
                        tint = colors.expandIconColor,
                    )
                }
            }
        }
    }
}

/** `COUISearchHistoryView.SEARCH_VIEW_DISAPPEAR_DURATION` = 300ms. */
private const val DELETE_ANIMATION_DURATION_MILLIS = 300

/** `COUIMoveEaseInterpolator`: `PathInterpolator(0.3f, 0f, 0.1f, 1f)`. */
private val MoveEase = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/** Contains the default values used by [SearchHistoryView]. */
object SearchHistoryViewDefaults {
    /**
     * `dimen/coui_component_search_history_padding_start` = 16dp. The native resource also carries a
     * `w600dp` value of 20dp, which the Kotlin resource API does not express.
     */
    val PaddingStart = 16.dp

    /** `dimen/coui_component_search_history_padding_top` = 16dp. */
    val PaddingTop = 16.dp

    /** `dimen/coui_component_search_history_padding_end` = 16dp (20dp from `w600dp`). */
    val PaddingEnd = 16.dp

    /** `dimen/coui_component_search_history_padding_bottom` = 32dp. */
    val PaddingBottom = 32.dp

    /** `dimen/coui_component_search_history_title_margin_vertical` = 7dp. */
    val TitleMarginVertical = 7.dp

    /** `dimen/coui_component_search_history_delete_icon_size` = 18dp. */
    val DeleteIconSize = 18.dp

    /** `dimen/coui_component_search_history_delete_icon_end_margin` = 0dp. */
    val DeleteIconEndMargin = 0.dp

    /** `dimen/coui_component_search_history_flow_margin_top` = 8dp. */
    val FlowMarginTop = 8.dp

    /** `dimen/coui_component_search_history_flow_item_spacing` = 8dp. */
    val FlowItemSpacing = 8.dp

    /** `dimen/coui_component_search_history_flow_line_spacing` = 8dp. */
    val FlowLineSpacing = 8.dp

    /**
     * The height of a history chip. The chip layout is `res/layout/coui_component_item_search_history`,
     * a `COUIChip` with `style/Widget.COUI.Chip.Suggestion`, whose height is
     * `dimen/coui_chip_suggest_style_height` = 28dp.
     */
    val ChipHeight = 28.dp

    /**
     * The chip corner radius. `style/Widget.COUI.Chip` sets `chipCornerRadius` to 16777215
     * (`0x00FFFFFF`, i.e. -1), which `COUIChipDrawable` resolves to half the chip height — a capsule.
     */
    val ChipCornerRadius = 14.dp

    /**
     * The chevron button's box. `COUIFlowLayout.getBaseExpandButton` sizes it from
     * `R.styleable.Chip_chipMinHeight`, i.e. `dimen/coui_chip_default_min_height` = 28dp.
     */
    val ExpandButtonSize = 28.dp

    /**
     * The chevron inside the button. `drawable/coui_component_expand_arrow_drop_down` is a 16dp
     * vector.
     */
    val ExpandIconSize = 16.dp

    /** `attr/COUIFlowLayout_maxRowFolded` / `attr/COUISearchHistoryView_maxRowFolded` default. */
    val MaxRowFolded = Int.MAX_VALUE

    /** `COUISearchHistoryView.DEFAULT_MAX_ROW_UNFOLDED` = 8. */
    val MaxRowUnfolded = 8

    /**
     * The title text style, `style/couiTextAppearanceButton` — 14sp `sans-serif-medium`. The theme
     * has no 14sp medium tier, so it is [io.github.suqi8.coui.kmp.theme.TextStyles.body2] at
     * [FontWeight.Medium], the same derivation [SnackbarDefaults.textStyle] uses.
     */
    @Composable
    fun titleTextStyle() = COUITheme.textStyles.body2.copy(fontWeight = FontWeight.Medium)

    /**
     * The panel colours.
     *
     * The title uses `?attr/couiColorPrimaryNeutral` (`coui_color_primary_neutral`, `#e6000000` light
     * / `#e6ffffff` night), which is exactly [io.github.suqi8.coui.kmp.theme.Colors.onSurface]. Both
     * icons use `?attr/couiColorSecondNeutral` (`coui_color_secondary_neutral`, `#8a000000` light /
     * `#8affffff` night), which is exactly
     * [io.github.suqi8.coui.kmp.theme.Colors.onSurfaceSecondary] — the delete vector's
     * `android:fillColor` and the chevron vector's `android:strokeColor` are the same attribute.
     */
    @Composable
    fun colors(
        titleColor: Color = COUITheme.colorScheme.onSurface,
        deleteIconColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        expandIconColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
    ): SearchHistoryViewColors = remember(titleColor, deleteIconColor, expandIconColor) {
        SearchHistoryViewColors(
            titleColor = titleColor,
            deleteIconColor = deleteIconColor,
            expandIconColor = expandIconColor,
        )
    }
}

/**
 * The colours of a [SearchHistoryView].
 *
 * @property titleColor `?attr/couiColorPrimaryNeutral`, the title text colour.
 * @property deleteIconColor `?attr/couiColorSecondNeutral`, the delete vector's fill tint.
 * @property expandIconColor `?attr/couiColorSecondNeutral`, the chevron vector's stroke tint.
 */
@Immutable
data class SearchHistoryViewColors(
    val titleColor: Color,
    val deleteIconColor: Color,
    val expandIconColor: Color,
)

/**
 * `drawable/coui_component_search_history_delete`: an 18dp x 18dp vector holding a single
 * `fillType="evenOdd"` path whose `android:fillColor` is `?attr/couiColorSecondNeutral`. The three
 * sub-paths after the outer silhouette punch the body hollow and cut the three slots, so the glyph
 * must keep [PathFillType.EvenOdd].
 */
val SearchHistoryDeleteIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SearchHistoryDelete",
        defaultWidth = 18.dp,
        defaultHeight = 18.dp,
        viewportWidth = 18f,
        viewportHeight = 18f,
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(6.781f, 2.001f)
            curveTo(6.985f, 1.388f, 7.559f, 0.975f, 8.204f, 0.975f)
            horizontalLineTo(9.799f)
            curveTo(10.444f, 0.975f, 11.018f, 1.388f, 11.222f, 2.001f)
            lineTo(11.63f, 3.225f)
            horizontalLineTo(14.247f)
            horizontalLineTo(14.255f)
            horizontalLineTo(15.189f)
            curveTo(15.479f, 3.225f, 15.714f, 3.46f, 15.714f, 3.75f)
            curveTo(15.714f, 4.04f, 15.479f, 4.275f, 15.189f, 4.275f)
            horizontalLineTo(14.747f)
            lineTo(14.08f, 15.613f)
            curveTo(14.033f, 16.406f, 13.376f, 17.025f, 12.582f, 17.025f)
            horizontalLineTo(5.421f)
            curveTo(4.627f, 17.025f, 3.97f, 16.406f, 3.923f, 15.613f)
            lineTo(3.257f, 4.275f)
            horizontalLineTo(2.814f)
            curveTo(2.524f, 4.275f, 2.289f, 4.04f, 2.289f, 3.75f)
            curveTo(2.289f, 3.46f, 2.524f, 3.225f, 2.814f, 3.225f)
            horizontalLineTo(3.748f)
            horizontalLineTo(3.756f)
            horizontalLineTo(6.373f)
            lineTo(6.781f, 2.001f)
            close()
            moveTo(10.226f, 2.333f)
            lineTo(10.523f, 3.225f)
            horizontalLineTo(7.48f)
            lineTo(7.777f, 2.333f)
            curveTo(7.839f, 2.149f, 8.011f, 2.025f, 8.204f, 2.025f)
            horizontalLineTo(9.799f)
            curveTo(9.993f, 2.025f, 10.165f, 2.149f, 10.226f, 2.333f)
            close()
            moveTo(4.308f, 4.275f)
            horizontalLineTo(6.023f)
            horizontalLineTo(6.752f)
            horizontalLineTo(11.252f)
            horizontalLineTo(11.98f)
            horizontalLineTo(13.695f)
            lineTo(13.031f, 15.551f)
            curveTo(13.017f, 15.789f, 12.821f, 15.975f, 12.582f, 15.975f)
            horizontalLineTo(5.421f)
            curveTo(5.183f, 15.975f, 4.986f, 15.789f, 4.972f, 15.551f)
            lineTo(4.308f, 4.275f)
            close()
            moveTo(7.089f, 7.483f)
            curveTo(7.079f, 7.193f, 6.837f, 6.966f, 6.547f, 6.975f)
            curveTo(6.257f, 6.985f, 6.03f, 7.227f, 6.039f, 7.517f)
            lineTo(6.228f, 13.329f)
            curveTo(6.237f, 13.619f, 6.479f, 13.847f, 6.769f, 13.837f)
            curveTo(7.059f, 13.828f, 7.286f, 13.585f, 7.277f, 13.295f)
            lineTo(7.089f, 7.483f)
            close()
            moveTo(11.965f, 7.517f)
            curveTo(11.974f, 7.227f, 11.747f, 6.985f, 11.457f, 6.975f)
            curveTo(11.167f, 6.966f, 10.925f, 7.193f, 10.915f, 7.483f)
            lineTo(10.727f, 13.295f)
            curveTo(10.717f, 13.585f, 10.945f, 13.828f, 11.235f, 13.837f)
            curveTo(11.524f, 13.847f, 11.767f, 13.619f, 11.776f, 13.329f)
            lineTo(11.965f, 7.517f)
            close()
            moveTo(9.002f, 6.975f)
            curveTo(9.292f, 6.975f, 9.527f, 7.21f, 9.527f, 7.5f)
            verticalLineTo(13.312f)
            curveTo(9.527f, 13.602f, 9.292f, 13.837f, 9.002f, 13.837f)
            curveTo(8.712f, 13.837f, 8.477f, 13.602f, 8.477f, 13.312f)
            verticalLineTo(7.5f)
            curveTo(8.477f, 7.21f, 8.712f, 6.975f, 9.002f, 6.975f)
            close()
        }
    }.build()
}

/**
 * `drawable/coui_component_expand_arrow_drop_down`: a 16dp x 16dp chevron stroked with
 * `?attr/couiColorSecondNeutral`, stroke width 0.933333 and round cap/join. The expanded state uses
 * `drawable/coui_component_expand_arrow_drop_up`, which is the same path rotated 180 degrees about
 * the viewport centre.
 */
val SearchHistoryExpandIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SearchHistoryExpand",
        defaultWidth = 16.dp,
        defaultHeight = 16.dp,
        viewportWidth = 16f,
        viewportHeight = 16f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 0.933333f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(4f, 6f)
            lineTo(8f, 10f)
            lineTo(12f, 6f)
        }
    }.build()
}
