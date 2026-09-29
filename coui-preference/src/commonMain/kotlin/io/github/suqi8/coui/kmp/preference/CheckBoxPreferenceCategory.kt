// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Checkbox
import io.github.suqi8.coui.kmp.basic.CheckboxColors
import io.github.suqi8.coui.kmp.basic.CheckboxDefaults
import io.github.suqi8.coui.kmp.preference.internal.CategoryHeader
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A category header whose trailing widget is a checkbox, mirroring COUICheckBoxPreferenceCategory.
 *
 * The device class extends `COUIPreferenceCategory` and binds
 * `coui_preference_category_widget_layout_checkbox.xml` into the category's `widget_frame`: a bare
 * `COUICheckBox` (id `android.R.id.checkbox`) with `paddingEnd` of
 * `coui_preference_category_widget_layout_padding_end` (16dp) and a `layout_marginVertical` of
 * `coui_preference_category_widget_layout_margin_vertical` (4dp). It also carries a
 * `default_checkbox_state` attribute, which only picks the starting state and is therefore not part
 * of this port's signature — the caller owns [checked].
 *
 * `COUICheckBox` is a tri-state box and the device's `OnStateChangeListener` reports all three, so
 * [state] is a [ToggleableState] rather than a `Boolean` — a category-level "select all" box reads
 * [ToggleableState.Indeterminate] while only some of its children are on. The title itself stays a
 * plain label: `COUIPreferenceCategory` keeps the row non-clickable and only the box reacts.
 *
 * @param title The title of the [CheckBoxPreferenceCategory].
 * @param state The current [ToggleableState] of the checkbox.
 * @param onClick The callback when the checkbox is clicked. The caller is responsible for updating
 *   [state].
 * @param modifier The modifier to be applied to the [CheckBoxPreferenceCategory].
 * @param titleColor The color of the title.
 * @param checkboxColors The [CheckboxColors] of the checkbox.
 * @param enabled Whether the checkbox is enabled.
 */
@Composable
@NonRestartableComposable
fun CheckBoxPreferenceCategory(
    title: String,
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    titleColor: Color = COUITheme.colorScheme.onBackgroundVariant,
    checkboxColors: CheckboxColors = CheckboxDefaults.checkboxColors(),
    enabled: Boolean = true,
) {
    CategoryHeader(
        title = title,
        modifier = modifier,
        titleColor = titleColor,
        trailing = {
            Checkbox(
                state = state,
                onClick = onClick,
                modifier = Modifier.padding(
                    vertical = CheckBoxPreferenceCategoryDefaults.WidgetMarginVertical,
                ),
                colors = checkboxColors,
                enabled = enabled,
            )
        },
    )
}

object CheckBoxPreferenceCategoryDefaults {

    /**
     * `dimen/coui_preference_category_widget_layout_margin_vertical` (4dp), the `layout_marginVertical`
     * of the checkbox in `coui_preference_category_widget_layout_checkbox.xml`.
     */
    val WidgetMarginVertical = 4.dp
}
