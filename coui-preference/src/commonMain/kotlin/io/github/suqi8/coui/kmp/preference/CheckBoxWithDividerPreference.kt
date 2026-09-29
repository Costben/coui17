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
import io.github.suqi8.coui.kmp.basic.CheckboxColors
import io.github.suqi8.coui.kmp.basic.CheckboxDefaults
import io.github.suqi8.coui.kmp.preference.internal.PreferenceWidgetDivider

/**
 * A [CheckboxPreference] whose checkbox is separated from the text block by a vertical hairline,
 * mirroring COUICheckBoxWithDividerPreference.
 *
 * The device subclass swaps the row for `res/layout/coui_check_box_divider_preference.xml`: the same
 * text block and checkbox as the plain checkbox row, with the checkbox moved to the tail and a
 * 1dp x 22dp `?couiColorDivider` line before it. This port keeps [CheckboxPreference] with
 * [CheckboxLocation.End] and hands the line to its `endActions`.
 *
 * @param title The title of the [CheckBoxWithDividerPreference].
 * @param checked The checked state of the checkbox.
 * @param onCheckedChange The callback when the checked state is changed.
 * @param modifier The modifier to be applied to the [CheckBoxWithDividerPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [CheckBoxWithDividerPreference].
 * @param summaryColor The color of the summary.
 * @param checkboxColors The [CheckboxColors] of the checkbox.
 * @param startAction The [Composable] content on the start side.
 * @param endActions The [Composable] content on the end side, placed between the divider and the checkbox.
 * @param bottomAction The [Composable] content at the bottom of the row.
 * @param insideMargin The margin inside the [CheckBoxWithDividerPreference].
 * @param cardListPosition The row's position inside its card group.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [CheckBoxWithDividerPreference] is clickable.
 */
@Composable
@NonRestartableComposable
fun CheckBoxWithDividerPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    checkboxColors: CheckboxColors = CheckboxDefaults.checkboxColors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
) {
    CheckboxPreference(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        checkboxColors = checkboxColors,
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
        checkboxLocation = CheckboxLocation.End,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}
