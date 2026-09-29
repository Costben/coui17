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
import io.github.suqi8.coui.kmp.basic.SwitchColors
import io.github.suqi8.coui.kmp.basic.SwitchDefaults
import io.github.suqi8.coui.kmp.preference.internal.PreferenceWidgetDivider

/**
 * A [SwitchPreference] whose switch is separated from the text block by a vertical hairline,
 * mirroring COUISwitchWithDividerPreference.
 *
 * The device subclass only replaces the inflated row with
 * `res/layout/coui_switch_with_divider_preference.xml`: the same `main_layout` text block and switch
 * as the plain switch row, plus a `switch_layout` holding a 1dp x 22dp `?couiColorDivider` line
 * before the switch. This port keeps [SwitchPreference] and hands the line to its `endActions`,
 * which is the slot the base already reserves ahead of the switch.
 *
 * @param checked The checked state of the [SwitchWithDividerPreference].
 * @param onCheckedChange The callback when the checked state is changed.
 * @param title The title of the [SwitchWithDividerPreference].
 * @param modifier The modifier to be applied to the [SwitchWithDividerPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [SwitchWithDividerPreference].
 * @param summaryColor The color of the summary.
 * @param startAction The [Composable] content on the start side.
 * @param endActions The [Composable] content on the end side, placed between the divider and the switch.
 * @param bottomAction The [Composable] content at the bottom of the row.
 * @param switchColors The [SwitchColors] of the switch.
 * @param insideMargin The margin inside the [SwitchWithDividerPreference].
 * @param cardListPosition The row's position inside its card group.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [SwitchWithDividerPreference] is clickable.
 */
@Composable
@NonRestartableComposable
fun SwitchWithDividerPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.switchColors(),
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
) {
    SwitchPreference(
        checked = checked,
        onCheckedChange = onCheckedChange,
        title = title,
        modifier = modifier,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
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
        bottomAction = bottomAction,
        switchColors = switchColors,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}
