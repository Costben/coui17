// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Badge
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.SwitchColors
import io.github.suqi8.coui.kmp.basic.SwitchDefaults

/**
 * A switch with a title and a summary.
 *
 * `COUISwitchPreference` dispatches its preference click listener from two places: a tap on the row
 * (`onClick`), and a direct toggle of the switch
 * (`COUISwitchPreferenceClickHelper.dispatchOnPreferenceClickIfUserSwitchToggle`). [onClick] is
 * invoked from both, after [onCheckedChange].
 *
 * @param checked The checked state of the [SwitchPreference].
 * @param onCheckedChange The callback when the checked state of the [SwitchPreference] is changed.
 * @param title The title of the [SwitchPreference].
 * @param modifier The modifier to be applied to the [SwitchPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [SwitchPreference].
 * @param summaryColor The color of the summary.
 * @param startAction The [Composable] content on the start side of the [SwitchPreference].
 * @param endActions The [Composable] content on the end side of the [SwitchPreference].
 * @param bottomAction The [Composable] content at the bottom of the [SwitchPreference].
 * @param switchColors The [SwitchColors] of the [SwitchPreference].
 * @param insideMargin The margin inside the [SwitchPreference].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive extra padding.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [SwitchPreference] is clickable.
 * @param showRedDot Whether to draw the `jump_icon_red_dot` hint dot between the end actions and the
 *   switch (`hasTitleRedDot`).
 * @param onClick The row click listener, the port of the preference's `OnPreferenceClickListener`. It
 *   runs on a row tap and on a direct toggle of the switch.
 */
@Composable
@NonRestartableComposable
fun SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.switchColors(),
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    showRedDot: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentOnClick by rememberUpdatedState(onClick)
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
                    .padding(end = 8.dp)
                    .align(Alignment.CenterVertically)
                    .weight(1f, fill = false),
            ) {
                endActions()
            }
            if (showRedDot) {
                Badge(
                    modifier = Modifier
                        .padding(start = SwitchPreferenceDefaults.RedDotStartPadding)
                        .align(Alignment.CenterVertically),
                    dotDiameter = SwitchPreferenceDefaults.RedDotDiameter,
                )
            }
            SwitchPreferenceEndActions(
                checked = checked,
                onCheckedChange = currentOnCheckedChange,
                onClick = currentOnClick,
                enabled = enabled,
                switchColors = switchColors,
            )
        },
        bottomAction = bottomAction,
        onClick = {
            if (enabled) {
                currentOnCheckedChange(!checked)
                currentOnClick?.invoke()
            }
        },
        role = Role.Switch,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds showRedDot and onClick.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@NonRestartableComposable
fun SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    switchColors: SwitchColors = SwitchDefaults.switchColors(),
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
): Unit = SwitchPreference(
    checked = checked,
    onCheckedChange = onCheckedChange,
    title = title,
    modifier = modifier,
    titleColor = titleColor,
    summary = summary,
    summaryColor = summaryColor,
    startAction = startAction,
    endActions = endActions,
    bottomAction = bottomAction,
    switchColors = switchColors,
    insideMargin = insideMargin,
    cardListPosition = cardListPosition,
    holdDownState = holdDownState,
    enabled = enabled,
)

/**
 * `COUIHintRedDot` sizes the switch row's dot from `coui_dot_diameter_small` and separates it from
 * the switch by `coui_switch_preference_dot_margin_start`.
 */
object SwitchPreferenceDefaults {
    /** `coui_dot_diameter_small`, 6dp. */
    val RedDotDiameter = 6.dp

    /** `coui_switch_preference_dot_margin_start`, 6dp. */
    val RedDotStartPadding = 6.dp
}

@Composable
private fun SwitchPreferenceEndActions(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    switchColors: SwitchColors,
) {
    Switch(
        checked = checked,
        onCheckedChange = { checkedNew ->
            onCheckedChange?.invoke(checkedNew)
            // A toggle the user made on the switch itself also dispatches the preference click.
            onClick?.invoke()
        },
        enabled = enabled,
        colors = switchColors,
    )
}
