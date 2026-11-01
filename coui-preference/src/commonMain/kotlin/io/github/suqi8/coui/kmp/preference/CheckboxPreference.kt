// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Checkbox
import io.github.suqi8.coui.kmp.basic.CheckboxColors
import io.github.suqi8.coui.kmp.basic.CheckboxDefaults
import io.github.suqi8.coui.kmp.preference.internal.StartActionSlot

/**
 * A checkbox with a title and a summary.
 *
 * @param title The title of the [CheckboxPreference].
 * @param checked The checked state of the [CheckboxPreference].
 * @param onCheckedChange The callback when the checked state of the [CheckboxPreference] is changed.
 * @param modifier The modifier to be applied to the [CheckboxPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [CheckboxPreference].
 * @param summaryColor The color of the summary.
 * @param checkboxColors The [CheckboxColors] of the [CheckboxPreference].
 * @param startAction The [Composable] content on the start side of the [CheckboxPreference].
 * @param endActions The [Composable] content on the end side of the [CheckboxPreference].
 * @param checkboxLocation The location of checkbox, [CheckboxLocation.Start] or [CheckboxLocation.End].
 *   Defaults to [CheckboxLocation.End]: `Preference.COUI.COUICheckBoxPreference` binds the box through
 *   `android:widgetLayout=@layout/preference_checkbox`, i.e. into the row's trailing `widget_frame`.
 * @param bottomAction The [Composable] content at the bottom of the [CheckboxPreference].
 * @param insideMargin The margin inside the [CheckboxPreference].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive extra padding.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [CheckboxPreference] is clickable.
 * @param onMainLayoutClick Replaces the row click. `COUICheckBoxWithDividerPreference` splits the
 *   row into two targets: `main_layout` reports to this callback while `check_box_layout` keeps
 *   toggling. `null` (default) keeps the row toggling the box.
 */
@Composable
@NonRestartableComposable
fun CheckboxPreference(
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
    checkboxLocation: CheckboxLocation = CheckboxLocation.End,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    onMainLayoutClick: (() -> Unit)? = null,
) {
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentOnMainLayoutClick by rememberUpdatedState(onMainLayoutClick)

    BasicComponent(
        modifier = modifier,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = if (checkboxLocation == CheckboxLocation.Start || startAction != null) {
            {
                Row {
                    if (checkboxLocation == CheckboxLocation.Start) {
                        CheckboxPreferenceStartAction(
                            checked = checked,
                            onCheckedChange = currentOnCheckedChange,
                            enabled = enabled,
                            checkboxColors = checkboxColors,
                        )
                    }

                    startAction?.let {
                        StartActionSlot(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            content = it,
                        )
                    }
                }
            }
        } else {
            null
        },
        endActions = {
            endActions?.let {
                Row(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .align(Alignment.CenterVertically)
                        .weight(1f, fill = false),
                ) {
                    it()
                }
            }

            if (checkboxLocation == CheckboxLocation.End) {
                CheckboxPreferenceEndAction(
                    checked = checked,
                    onCheckedChange = currentOnCheckedChange,
                    enabled = enabled,
                    checkboxColors = checkboxColors,
                )
            }
        },
        bottomAction = bottomAction,
        onClick = {
            val mainLayoutClick = currentOnMainLayoutClick
            if (mainLayoutClick != null) {
                mainLayoutClick()
            } else {
                currentOnCheckedChange.takeIf { enabled }?.invoke(!checked)
            }
        },
        onClickLabel = CheckboxPreferenceDefaults.SwitchActionLabel,
        role = Role.Checkbox,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}

@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds the onMainLayoutClick parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@NonRestartableComposable
fun CheckboxPreference(
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
    checkboxLocation: CheckboxLocation = CheckboxLocation.End,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
): Unit = CheckboxPreference(
    title = title,
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier,
    titleColor = titleColor,
    summary = summary,
    summaryColor = summaryColor,
    checkboxColors = checkboxColors,
    startAction = startAction,
    endActions = endActions,
    checkboxLocation = checkboxLocation,
    bottomAction = bottomAction,
    insideMargin = insideMargin,
    cardListPosition = cardListPosition,
    holdDownState = holdDownState,
    enabled = enabled,
)

@Composable
private fun CheckboxPreferenceStartAction(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean,
    checkboxColors: CheckboxColors,
) {
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentChecked by rememberUpdatedState(checked)
    val onClick = remember(onCheckedChange != null) {
        if (onCheckedChange != null) {
            { currentOnCheckedChange?.invoke(!currentChecked) ?: Unit }
        } else {
            null
        }
    }
    Checkbox(
        modifier = Modifier
            .padding(end = 5.dp),
        state = ToggleableState(checked),
        onClick = onClick,
        enabled = enabled,
        colors = checkboxColors,
    )
}

@Composable
private fun CheckboxPreferenceEndAction(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean,
    checkboxColors: CheckboxColors,
) {
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentChecked by rememberUpdatedState(checked)
    val endMargin = LocalCheckboxEndMargin.current
    val onClick = remember(onCheckedChange != null) {
        if (onCheckedChange != null) {
            { currentOnCheckedChange?.invoke(!currentChecked) ?: Unit }
        } else {
            null
        }
    }
    Checkbox(
        modifier = Modifier.padding(end = endMargin),
        state = ToggleableState(checked),
        onClick = onClick,
        enabled = enabled,
        colors = checkboxColors,
    )
}

/**
 * Extra end margin for the trailing checkbox of a preference row. `coui_preference_widget_checkbox.xml`
 * gives the box `layout_marginRight=coui_preference_checkbox_margin_right` (-10dp) on a row padded by
 * `support_preference_title_padding_end` (32dp), so the box's right edge lands 22dp from the row's
 * edge — 16dp of [BasicComponentDefaults.InsideMargin] plus the 6dp
 * [CheckboxPreferenceDefaults.CheckboxEndMargin]. Variant rows that pad differently override it.
 */
internal val LocalCheckboxEndMargin = compositionLocalOf { CheckboxPreferenceDefaults.CheckboxEndMargin }

/**
 * Labels and measurements of [CheckboxPreference].
 */
object CheckboxPreferenceDefaults {

    /**
     * The click action label COUI puts on the row, `coui_accessibility_switch`
     * (`COUICheckBoxPreference.onBindViewHolder`). This class only ever sets the box to state 0 or 2
     * (`setState(mChecked ? 2 : 0)`), so the `coui_accessibility_select_all` variant it also carries
     * for state 1 is unreachable.
     */
    val SwitchActionLabel = "Switch"

    /**
     * How far the trailing box stays inside the row's `insideMargin` end edge: the row's 32dp
     * `support_preference_title_padding_end` minus the box's 10dp negative
     * `coui_preference_checkbox_margin_right`, minus the 16dp the row already pads.
     */
    val CheckboxEndMargin = 6.dp
}

enum class CheckboxLocation {
    Start,
    End,
}
