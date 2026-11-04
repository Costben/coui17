// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.interfaces.HoldDownObserver
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.roundToInt

/**
 * A basic component with COUI style. Widely used in other extension components.
 *
 * @param modifier The modifier to be applied to the [BasicComponent].
 * @param title The title of the [BasicComponent].
 * @param titleColor The color of the title.
 * @param summary The summary of the [BasicComponent].
 * @param summaryColor The color of the summary.
 * @param startAction The [Composable] content on the start side of the [BasicComponent].
 * @param centerStartAction Whether the leading [startAction] stays on the row's centre line even when
 *   the text block wraps to three or more lines. The default mirrors COUI's `setIconTopInParent`,
 *   which anchors the icon to the centre of the first two lines once the text outgrows them.
 * @param endActions The [Composable] content on the end side of the [BasicComponent].
 * @param bottomAction The [Composable] content at the bottom of the [BasicComponent].
 * @param insideMargin The margin inside the [BasicComponent].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive an
 *   extra [CardListPosition.HeadOrTailPadding]. Defaults to [CardListPosition.None].
 * @param showDivider Whether to draw the hairline between this row and the next one, COUI
 *   `couiShowDivider` (true by default). Like `COUIPreference.drawDivider()`, only a head or middle
 *   row of a card group draws one.
 * @param onClick The callback when the [BasicComponent] is clicked.
 * @param onClickLabel Optional label describing the click action for accessibility services.
 * @param role The semantic [Role] of the [BasicComponent] for accessibility services.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [BasicComponent] is enabled.
 * @param interactionSource The [MutableInteractionSource] for the [BasicComponent].
 *   The value should remain null or non-null for the lifetime of this component;
 *   switching across recompositions will allocate a new internal source and lose pending interactions.
 */
@Composable
fun BasicComponent(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    centerStartAction: Boolean = false,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    // `COUICustomListSelectedLinearLayout` re-measures its icon once the title and summary have been
    // laid out, because both the icon's margins and its top offset depend on how many lines the text
    // block wraps to. Compose learns the same geometry from `onTextLayout`, which runs inside the text
    // measure, so the row reads it there too: publishing the value as a composition local made the row
    // recompose once the text had settled, and the frame before that recomposition was a one-line row
    // with the icon centred.
    val titleGeometry = remember { mutableStateOf(TextLineGeometry()) }
    val summaryGeometry = remember { mutableStateOf(TextLineGeometry()) }
    val metricsSource = remember {
        PreferenceTextMetricsSource {
            PreferenceTextMetrics(titleGeometry.value, summaryGeometry.value)
        }
    }
    CompositionLocalProvider(LocalPreferenceTextMetrics provides metricsSource) {
        BasicComponent(
            startAction = startAction,
            centerStartAction = centerStartAction,
            endActions = endActions,
            bottomAction = bottomAction,
            modifier = modifier,
            insideMargin = insideMargin,
            cardListPosition = cardListPosition,
            showDivider = showDivider,
            onClick = onClick,
            onClickLabel = onClickLabel,
            role = role,
            holdDownState = holdDownState,
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = COUITheme.textStyles.headline1,
                    fontWeight = FontWeight.Medium,
                    color = titleColor.color(enabled),
                    onTextLayout = { titleGeometry.value = it.lineGeometry() },
                )
            }
            if (summary != null) {
                // `coui_preference.xml` stacks the summary straight under the title: the 2dp
                // `support_preference_margin_between_line` belongs to the icon placement, not the text.
                Text(
                    text = summary,
                    style = COUITheme.textStyles.footnote1,
                    color = summaryColor.color(enabled),
                    onTextLayout = { summaryGeometry.value = it.lineGeometry() },
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the showDivider parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun BasicComponent(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
): Unit = BasicComponent(
    modifier = modifier,
    title = title,
    titleColor = titleColor,
    summary = summary,
    summaryColor = summaryColor,
    startAction = startAction,
    endActions = endActions,
    bottomAction = bottomAction,
    insideMargin = insideMargin,
    cardListPosition = cardListPosition,
    showDivider = true,
    onClick = onClick,
    onClickLabel = onClickLabel,
    role = role,
    holdDownState = holdDownState,
    enabled = enabled,
    interactionSource = interactionSource,
)

/**
 * A basic component with COUI style. Widely used in other extension components.
 *
 * @param modifier The modifier to be applied to the [BasicComponent].
 * @param startAction The [Composable] content on the start side of the [BasicComponent].
 * @param centerStartAction Whether the leading [startAction] stays on the row's centre line even when
 *   the text block wraps to three or more lines. The default mirrors COUI's `setIconTopInParent`,
 *   which anchors the icon to the centre of the first two lines once the text outgrows them.
 * @param endActions The [Composable] content on the end side of the [BasicComponent].
 * @param bottomAction The [Composable] content at the bottom of the [BasicComponent].
 * @param insideMargin The margin inside the [BasicComponent].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive an
 *   extra [CardListPosition.HeadOrTailPadding], so a standalone row is 4dp taller than a middle row
 *   and a head/tail row is 2dp taller. Defaults to [CardListPosition.None] (no extra padding).
 * @param showDivider Whether to draw the hairline between this row and the next one, COUI
 *   `couiShowDivider` (true by default). Like `COUIPreference.drawDivider()`, only a head or middle
 *   row of a card group draws one.
 * @param onClick The callback when the [BasicComponent] is clicked.
 * @param onClickLabel Optional label describing the click action for accessibility services.
 * @param role The semantic [Role] of the [BasicComponent] for accessibility services.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [BasicComponent] is enabled.
 * @param interactionSource The [MutableInteractionSource] for the [BasicComponent].
 *   The value should remain null or non-null for the lifetime of this component;
 *   switching across recompositions will allocate a new internal source and lose pending interactions.
 * @param content The content of the [BasicComponent].
 */
@Composable
fun BasicComponent(
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    centerStartAction: Boolean = false,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val currentOnClick by rememberUpdatedState(onClick)

    HoldDownObserver(holdDownState, interactionSource)

    val clickableModifier = remember(enabled, onClick != null, interactionSource, role, onClickLabel) {
        if (enabled && onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                onClickLabel = onClickLabel,
                role = role,
                onClick = { currentOnClick?.invoke() },
            )
        } else {
            Modifier
        }
    }

    // COUI COUICardListSelectedItemLayout.setPadding: the rounded outer edges of a card group each get
    // an extra coui_list_card_head_or_tail_padding, and the row's minimum height grows by the same total.
    val extraPaddingTop = cardListPosition.extraPaddingTop
    val extraPaddingBottom = cardListPosition.extraPaddingBottom

    // COUIPreferenceItemDecoration draws coui_list_divider_height of couiColorDivider between rows,
    // inset by coui_preference_divider_default_horizontal_padding on both sides. The decoration sits
    // in the gap between items, so the hairline does not add to the row height.
    val drawDivider = cardListPosition.drawsRowDivider(showDivider)
    val dividerColor = COUITheme.colorScheme.dividerLine

    // Where the leading icon aligns against the text, published by the overload that renders the title
    // and summary itself. Read in measure rather than here: the geometry only exists once the text has
    // been measured, and reading it here would recompose the row for it.
    val textMetricsSource = LocalPreferenceTextMetrics.current

    // The vertical inside margin belongs to the text block, not to the whole row: a leading icon
    // carries its own top and bottom margins, and those already size the row when they are taller
    // than the text. Padding the icon as well stacked 20dp on top of it — a 24dp icon row came out
    // 68.4dp where the ColorOS 17 Settings list draws 52dp, and a title-only row with an icon looked
    // half again as tall as the 48dp row of a page whose items have no icon.
    val layoutDirection = LocalLayoutDirection.current
    val topInsidePadding = insideMargin.calculateTopPadding()
    val bottomInsidePadding = insideMargin.calculateBottomPadding()
    val density = LocalDensity.current

    // A bottom action sits directly under the text: the action's own height — a 36dp slider touch
    // target around a 20dp track — already leaves the control 8dp below the title, so the text block
    // drops its bottom inset and the row's bottom inset moves below the action. Keeping both put 40dp
    // between a slider preference's title and its track where ColorOS 17 draws 8dp.
    val contentBottomPadding = if (bottomAction == null) bottomInsidePadding else 0.dp
    val topInsidePaddingPx = with(density) { topInsidePadding.roundToPx() }
    val contentBottomPaddingPx = with(density) { contentBottomPadding.roundToPx() }

    Column(
        modifier = modifier
            .heightIn(min = BasicComponentDefaults.MinHeight + extraPaddingTop + extraPaddingBottom)
            .fillMaxWidth()
            .then(clickableModifier)
            .then(
                if (drawDivider) {
                    Modifier.drawWithContent {
                        drawContent()
                        val inset = BasicComponentDefaults.DividerHorizontalPadding.toPx()
                        val thickness = BasicComponentDefaults.DividerHeight.toPx()
                        drawRect(
                            color = dividerColor,
                            topLeft = Offset(inset, size.height - thickness),
                            size = Size((size.width - inset * 2f).coerceAtLeast(0f), thickness),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .padding(
                start = insideMargin.calculateStartPadding(layoutDirection),
                end = insideMargin.calculateEndPadding(layoutDirection),
            )
            .padding(top = extraPaddingTop, bottom = extraPaddingBottom),
        verticalArrangement = Arrangement.Center,
    ) {
        if (startAction == null && endActions == null) {
            Column(
                modifier = Modifier.padding(top = topInsidePadding, bottom = contentBottomPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                content = content,
            )
        } else {
            Layout(
                content = {
                    startAction?.let {
                        Column(
                            modifier = Modifier.layoutId("start"),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start,
                        ) { it() }
                    }
                    Column(
                        modifier = Modifier.layoutId("center"),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                        content = content,
                    )
                    endActions?.let {
                        Column(
                            modifier = Modifier.layoutId("end"),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.End,
                        ) {
                            // Center end actions vertically so trailing value text lines up with taller siblings.
                            Row(verticalAlignment = Alignment.CenterVertically) { it() }
                        }
                    }
                },
            ) { measurables, constraints ->
                // COUI PreferenceIcon's layout_marginEnd (coui_preference_icon_margin_right, 16dp):
                // the only gap between the start action and the text block.
                val spacerPx = 16.dp.roundToPx()

                val startMeasurable = measurables.firstOrNull { it.layoutId == "start" }
                val centerMeasurable = measurables.first { it.layoutId == "center" }
                val endMeasurable = measurables.firstOrNull { it.layoutId == "end" }

                val maxWidth = constraints.maxWidth
                val maxHeight = constraints.maxHeight

                val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

                val startPlaceable = startMeasurable?.measure(looseConstraints)
                val startWidth = startPlaceable?.width ?: 0
                val startSpacerWidth = if (startWidth > 0) spacerPx else 0
                val widthAfterStart = (maxWidth - startWidth - startSpacerWidth).coerceAtLeast(0)

                val endIntrinsicWidth = endMeasurable?.maxIntrinsicWidth(maxHeight) ?: 0
                // `coui_preference.xml` lets a wrap_content `widget_frame` take all it wants, but an
                // uncapped trailing value leaves the text block no width and breaks it into a single
                // column of characters, so the cap stays even though the native row has none.
                val endHardCap = (widthAfterStart - spacerPx).coerceAtLeast(0) * 6 / 10
                val endTargetWidth = endIntrinsicWidth.coerceAtMost(endHardCap)
                val endPlaceable = endMeasurable?.measure(
                    looseConstraints.copy(maxWidth = endTargetWidth),
                )
                val endActualWidth = endPlaceable?.width ?: 0
                val endSpacerWidth = if (endActualWidth > 0) spacerPx else 0

                val widthForCenter = (widthAfterStart - endActualWidth - endSpacerWidth).coerceAtLeast(0)
                val centerPlaceable = centerMeasurable.measure(
                    looseConstraints.copy(maxWidth = widthForCenter),
                )

                // Read after the text block: `onTextLayout` has already run inside that measure, so this
                // is the geometry of the frame being laid out, not of the one before it.
                val textMetrics = textMetricsSource?.read()

                val startHeight = startPlaceable?.height ?: 0
                // COUICustomListSelectedLinearLayout.setIconCenterInParent gives the icon equal top and
                // bottom margins picked by its size, so they never move it off the centre line; they
                // only grow the row.
                val iconMarginPx = if (startWidth > 0) {
                    iconVerticalMarginPx(startHeight, textMetrics?.lineCount ?: 1)
                } else {
                    0
                }
                val startBlockHeight = startHeight + iconMarginPx * 2
                val endHeight = endPlaceable?.height ?: 0
                // The text and the trailing actions keep the row's top inset. The bottom inset is
                // dropped while a bottom action follows, because that action owns the space under
                // the text; the leading icon keeps neither inset, since its margins are already part
                // of its block.
                val contentHeight = topInsidePaddingPx + contentBottomPaddingPx
                val rowHeight = maxOf(
                    startBlockHeight,
                    centerPlaceable.height + contentHeight,
                    endHeight + contentHeight,
                )
                val layoutHeight = rowHeight
                    .coerceIn(constraints.minHeight, maxHeight.takeIf { it != Constraints.Infinity } ?: rowHeight)

                // Three or more lines of text switch the icon from centred to top-anchored on the
                // first two lines, and setIconTopInParent drops both margins; see [preferenceIconTopPx].
                // Callers that want the icon on the row's centre line regardless of text length opt
                // out with [centerStartAction].
                val topAnchoredStartTop = if (centerStartAction) {
                    null
                } else {
                    preferenceIconTopPx(
                        metrics = textMetrics,
                        iconHeightPx = startHeight,
                        marginBetweenLinePx = BasicComponentDefaults.MarginBetweenLine.roundToPx(),
                    )
                }

                layout(width = maxWidth, height = layoutHeight) {
                    val startTop = topAnchoredStartTop
                        ?: ((rowHeight - startBlockHeight).coerceAtLeast(0) / 2 + iconMarginPx)
                    // With a bottom action the text keeps only its top inset instead of being
                    // centred in the row, so the action starts right below it.
                    val centerTop = if (bottomAction == null) {
                        (rowHeight - centerPlaceable.height) / 2
                    } else {
                        topInsidePaddingPx
                    }
                    val endTop = (centerTop + (centerPlaceable.height - endHeight) / 2).coerceAtLeast(0)

                    startPlaceable?.placeRelative(0, startTop)

                    val centerX = startWidth + startSpacerWidth
                    centerPlaceable.placeRelative(centerX, centerTop)

                    endPlaceable?.let {
                        val endX = maxWidth - it.width
                        it.placeRelative(endX, endTop)
                    }
                }
            }
        }

        if (bottomAction != null) {
            Column(modifier = Modifier.padding(bottom = bottomInsidePadding)) {
                bottomAction()
            }
        }
    }
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the showDivider parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun BasicComponent(
    modifier: Modifier = Modifier,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BasicComponent(
        modifier = modifier,
        startAction = startAction,
        endActions = endActions,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        showDivider = true,
        onClick = onClick,
        onClickLabel = onClickLabel,
        role = role,
        holdDownState = holdDownState,
        enabled = enabled,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * Whether a row at this position draws the hairline below it, mirroring `COUIPreference.drawDivider()`:
 * only the head and middle rows of a card group do, and only while `couiShowDivider` is on.
 */
internal fun CardListPosition.drawsRowDivider(showDivider: Boolean): Boolean = showDivider && (this == CardListPosition.Head || this == CardListPosition.Middle)

object BasicComponentDefaults {

    /**
     * The default minimum height of the [BasicComponent], before any card-group
     * position padding is added.
     */
    val MinHeight = 48.dp

    /**
     * The default margin inside the [BasicComponent].
     */
    val InsideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp)

    /**
     * The height of the hairline [BasicComponent] draws between two rows of a card group, COUI
     * `coui_list_divider_height` (0.33dp).
     */
    val DividerHeight = 0.33.dp

    /**
     * The horizontal inset of that hairline on both sides, COUI
     * `coui_preference_divider_default_horizontal_padding` (32dp).
     */
    val DividerHorizontalPadding = 32.dp

    /**
     * The gap COUI counts between the title and the summary when it measures the leading icon,
     * `support_preference_margin_between_line` (2dp). The two texts themselves are stacked without a
     * gap; the value only feeds the icon placement of a multi-line row.
     */
    val MarginBetweenLine = 2.dp

    /**
     * The default color of the title.
     */
    @Composable
    fun titleColor(
        color: Color = COUITheme.colorScheme.onBackground,
        disabledColor: Color = COUITheme.colorScheme.disabledOnSecondaryVariant,
    ): BasicComponentColors = remember(color, disabledColor) {
        BasicComponentColors(
            color = color,
            disabledColor = disabledColor,
        )
    }

    /**
     * The default color of the summary.
     *
     * `coui_preference_secondary_text_color` resolves to `?couiColorSecondNeutral` in every state
     * but disabled, i.e. `coui_color_secondary_neutral` (`#8a000000`, night `#8affffff`) — the color
     * [Colors.onSurfaceSecondary] carries.
     */
    @Composable
    fun summaryColor(
        color: Color = COUITheme.colorScheme.onSurfaceSecondary,
        disabledColor: Color = COUITheme.colorScheme.disabledOnSecondaryVariant,
    ): BasicComponentColors = remember(color, disabledColor) {
        BasicComponentColors(
            color = color,
            disabledColor = disabledColor,
        )
    }
}

@Immutable
data class BasicComponentColors(
    val color: Color,
    val disabledColor: Color,
) {
    @Stable
    internal fun color(enabled: Boolean): Color = if (enabled) color else disabledColor
}

/**
 * How one of a row's text views wrapped, reduced to what the leading icon needs: its line count, the
 * height of a line, and the view's total height. Mirrors the `getLineCount()`, `getLineTop()` and
 * `getLineBottom()` reads of `COUICustomListSelectedLinearLayout.operateMultilineIconPosition`.
 */
@Immutable
internal data class TextLineGeometry(
    val lines: Int = 0,
    val firstLineBottomPx: Int = 0,
    val secondLineBottomPx: Int = 0,
    val heightPx: Int = 0,
)

internal fun TextLayoutResult.lineGeometry(): TextLineGeometry = TextLineGeometry(
    lines = lineCount,
    firstLineBottomPx = if (lineCount > 0) getLineBottom(0).roundToInt() else 0,
    secondLineBottomPx = if (lineCount > 1) getLineBottom(1).roundToInt() else 0,
    heightPx = size.height,
)

/** The title and summary geometry of a preference row, in the order the two are stacked. */
@Immutable
internal data class PreferenceTextMetrics(
    val title: TextLineGeometry,
    val summary: TextLineGeometry,
) {
    val lineCount: Int get() = title.lines + summary.lines
}

/**
 * Where a row's text geometry comes from, read on demand so the caller can take it inside a measure
 * block instead of recomposing when the text settles.
 */
internal fun interface PreferenceTextMetricsSource {
    fun read(): PreferenceTextMetrics
}

/**
 * The text geometry of the enclosing row. [BasicComponent]'s string overload publishes it once the
 * title and summary have been laid out; rows that build their own content leave it `null`, and their
 * leading icon stays centred.
 */
internal val LocalPreferenceTextMetrics = compositionLocalOf<PreferenceTextMetricsSource?> { null }

/**
 * The distance from the top of the text block to the centre of its first two lines, or `null` when
 * the block is at most two lines tall and `setIconCenterInParent` keeps the icon centred.
 *
 * `checkFirstTwoLineCenter` walks title, summary and assignment in order and spends a budget of two
 * lines on them, so the centre is taken from the title alone when it wraps, and from the title's
 * first line plus the summary's first line otherwise.
 */
internal fun preferenceFirstTwoLineCentrePx(
    metrics: PreferenceTextMetrics,
    marginBetweenLinePx: Int,
): Float? {
    if (metrics.lineCount <= 2) return null
    return when {
        metrics.title.lines >= 2 -> metrics.title.secondLineBottomPx / 2f

        metrics.title.lines == 1 ->
            (metrics.title.heightPx + marginBetweenLinePx + metrics.summary.firstLineBottomPx) / 2f

        else -> metrics.summary.secondLineBottomPx / 2f
    }
}

/**
 * The equal top and bottom margin COUI gives a leading icon of [iconHeightPx]: the single-line ladder
 * (`coui_preference_icon_margin_top` 14dp, then a 24dp icon 12dp, 32dp 10dp, 36dp 12dp, 50dp 8dp) or,
 * once the text block wraps to exactly two lines, the `_top_multiline` pair (24dp 20dp, 32dp 16dp,
 * 36dp 14dp; the 50dp branch keeps 8dp). Three or more lines are placed by [preferenceIconTopPx]
 * instead, which drops both margins.
 */
internal fun Density.iconVerticalMarginPx(iconHeightPx: Int, lineCount: Int): Int {
    if (lineCount > 2) return 0
    val multiline = lineCount == 2
    return when (iconHeightPx.toDp().value.roundToInt()) {
        24 -> if (multiline) 20.dp else 12.dp
        32 -> if (multiline) 16.dp else 10.dp
        36 -> if (multiline) 14.dp else 12.dp
        50 -> 8.dp
        else -> 14.dp
    }.roundToPx()
}

/**
 * The top offset of the leading icon inside the text block, COUI
 * `COUICustomListSelectedLinearLayout.setIconTopInParent`: the icon is anchored so that its centre
 * sits on the centre of the first two lines, and never rises above the block. `null` means the icon
 * stays centred.
 *
 * The native measurement works from the row's outer edge, where the text block starts
 * `support_preference_text_content_padding_top` (10dp) lower and the icon has an 8dp floor. The port
 * keeps that 10dp in the row's inside margin, so the coordinate here starts at the text block and the
 * floor becomes "not above the block".
 */
internal fun preferenceIconTopPx(
    metrics: PreferenceTextMetrics?,
    iconHeightPx: Int,
    marginBetweenLinePx: Int,
): Int? {
    if (metrics == null) return null
    val centre = preferenceFirstTwoLineCentrePx(metrics, marginBetweenLinePx) ?: return null
    return (centre - iconHeightPx / 2f).roundToInt().coerceAtLeast(0)
}
