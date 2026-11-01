// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Badge
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.ArrowRight
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * An arrow with a title and a summary.
 *
 * @param title The title of the [ArrowPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [ArrowPreference].
 * @param summaryColor The color of the summary.
 * @param startAction The [Composable] content on the start side of the [ArrowPreference].
 * @param endActions The [Composable] content on the end side of the [ArrowPreference].
 * @param bottomAction The [Composable] content at the bottom of the [ArrowPreference].
 * @param modifier The modifier to be applied to the [ArrowPreference].
 * @param insideMargin The margin inside the [ArrowPreference].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive extra padding.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [ArrowPreference] is clickable.
 * @param statusText Trailing status text drawn before the arrow, as `coui_statusText1` in
 *   `coui_preference_widget_jump.xml`: 14sp, one line, end-aligned, at most
 *   [ArrowPreferenceDefaults.StatusTextMaxWidth] wide and inset by
 *   [ArrowPreferenceDefaults.StatusTextStartPadding]. `null` hides it.
 * @param showRedDot Whether to draw the `jump_icon_red_dot` hint dot between the status text and the
 *   arrow, inset from the arrow by
 *   [ArrowPreferenceDefaults.RedDotEndPadding].
 */
@Composable
@NonRestartableComposable
fun ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    statusText: String? = null,
    showRedDot: Boolean = false,
) {
    BasicComponent(
        modifier = modifier,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = startAction,
        endActions = {
            Row(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .align(Alignment.CenterVertically)
                    .weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                endActions()
            }
            if (statusText != null) {
                Text(
                    modifier = Modifier
                        .padding(start = ArrowPreferenceDefaults.StatusTextStartPadding)
                        .widthIn(max = ArrowPreferenceDefaults.StatusTextMaxWidth)
                        .align(Alignment.CenterVertically),
                    text = statusText,
                    style = COUITheme.textStyles.body2,
                    color = COUITheme.colorScheme.onSurfaceSecondary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showRedDot) {
                Badge(
                    modifier = Modifier
                        .padding(end = ArrowPreferenceDefaults.RedDotEndPadding)
                        .align(Alignment.CenterVertically),
                )
            }
            ArrowPreferenceEndAction(
                enabled = enabled,
            )
        },
        bottomAction = bottomAction,
        onClick = onClick,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds statusText and showRedDot.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@NonRestartableComposable
fun ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
): Unit = ArrowPreference(
    title = title,
    modifier = modifier,
    titleColor = titleColor,
    summary = summary,
    summaryColor = summaryColor,
    startAction = startAction,
    endActions = endActions,
    bottomAction = bottomAction,
    insideMargin = insideMargin,
    cardListPosition = cardListPosition,
    onClick = onClick,
    holdDownState = holdDownState,
    enabled = enabled,
)

@Composable
private fun RowScope.ArrowPreferenceEndAction(
    enabled: Boolean,
) {
    val actionColors = ArrowPreferenceDefaults.endActionColors()
    val tintFilter = remember(enabled, actionColors) {
        ColorFilter.tint(actionColors.color(enabled = enabled))
    }
    Image(
        modifier = Modifier
            // RTL mirroring is handled by the icon itself (autoMirror).
            .size(width = 12.dp, height = 24.dp)
            .align(Alignment.CenterVertically),
        imageVector = COUIIcons.Basic.ArrowRight,
        contentDescription = null,
        colorFilter = tintFilter,
    )
}

object ArrowPreferenceDefaults {

    /**
     * The widest the trailing status text may get (`coui_preference_status_text_max_width`, 162dp).
     */
    val StatusTextMaxWidth = 162.dp

    /**
     * The gap between the row's end actions and the status text
     * (`support_preference_widget_focus_jump_margin_start`, 24dp).
     */
    val StatusTextStartPadding = 24.dp

    /**
     * The gap between the hint dot and the arrow
     * (`support_preference_reddot_margin_end_in_right_hasassignment`, 4dp).
     */
    val RedDotEndPadding = 4.dp

    /**
     * The default color of the arrow (COUI coui_btn_next, couiColorLabelTertiary).
     *
     * `coui_btn_next_disabled` paints the arrow with `?couiColorLabelQuaternary` —
     * `coui_color_label_quaternary` `#14000000` (night `#26ffffff`) — which is
     * [io.github.suqi8.coui.kmp.theme.Colors.disabledSecondaryVariant], not the heavier
     * `disabledOnSecondaryVariant` the disabled title uses.
     */
    @Composable
    fun endActionColors(): EndActionColors {
        val color = COUITheme.colorScheme.onSurfaceVariantActions
        val disabledColor = COUITheme.colorScheme.disabledSecondaryVariant
        return remember(color, disabledColor) {
            EndActionColors(
                color = color,
                disabledColor = disabledColor,
            )
        }
    }
}

@Immutable
data class EndActionColors(
    private val color: Color,
    private val disabledColor: Color,
) {
    @Stable
    internal fun color(enabled: Boolean): Color = if (enabled) color else disabledColor
}
