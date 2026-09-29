// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.RadioButtonColors
import io.github.suqi8.coui.kmp.basic.RadioButtonDefaults
import io.github.suqi8.coui.kmp.preference.internal.PreferenceWidgetDivider

/**
 * A [MarkPreference] whose mark is separated from the text block by a vertical hairline,
 * mirroring COUIMarkWithDividerPreference.
 *
 * The device subclass swaps the row for `res/layout/coui_mark_with_divider_preference.xml`: the same
 * text block and tail mark as the plain mark row, with a 1dp x 22dp `?couiColorDivider` line before
 * the mark. This port keeps [MarkPreference] and hands the line to its `endActions`.
 *
 * `COUIMarkWithDividerPreference` also splits the row into two click targets — `main_layout` fires
 * `OnMainLayoutClickListener` and `radio_layout` toggles the mark — but neither the repo's
 * [MarkPreference] nor this wrapper models that split; the whole row keeps the single
 * [MarkPreference] click target.
 *
 * @param title The title of the [MarkWithDividerPreference].
 * @param checked Whether this row currently carries the mark.
 * @param onClick The callback when the [MarkWithDividerPreference] is clicked.
 * @param modifier The modifier to be applied to the [MarkWithDividerPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [MarkWithDividerPreference].
 * @param summaryColor The color of the summary.
 * @param markColors The [RadioButtonColors] of the mark.
 * @param startAction The [Composable] content on the start side.
 * @param endActions The [Composable] content on the end side, placed between the divider and the mark.
 * @param bottomAction The [Composable] content at the bottom of the row.
 * @param insideMargin The margin inside the [MarkWithDividerPreference].
 * @param cardListPosition The row's position inside its card group.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [MarkWithDividerPreference] is clickable.
 */
@Composable
@NonRestartableComposable
fun MarkWithDividerPreference(
    title: String,
    checked: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    markColors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
) {
    MarkPreference(
        title = title,
        checked = checked,
        onClick = onClick,
        modifier = modifier,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        markColors = markColors,
        startAction = startAction,
        endActions = {
            PreferenceWidgetDivider()
            endActions?.let {
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically),
                ) {
                    it()
                }
            }
        },
        markLocation = MarkLocation.End,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}
