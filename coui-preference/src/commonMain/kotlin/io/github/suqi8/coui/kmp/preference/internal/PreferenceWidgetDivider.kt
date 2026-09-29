// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The vertical hairline the `*WithDivider` preferences draw between their text block and their
 * trailing widget, from `coui_preference_widget_layout` divider views: 1dp x 22dp painted in
 * `?couiColorDivider` (`dimen/preference_divider_width` / `dimen/preference_divider_height`).
 *
 * The 8dp on each side is the remainder of the COUI gaps once the base preference is accounted for:
 * [io.github.suqi8.coui.kmp.basic.BasicComponent] already reserves an 8dp spacer between the text
 * block and the end slot, and the widgets place another 8dp on their side (`android_preference_switch_margin_left`
 * / `preference_divider_margin_end` on the device). 8 + 8 = the 16dp the device keeps on both sides
 * of the line.
 */
@Composable
internal fun PreferenceWidgetDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(start = 8.dp, end = 8.dp)
            .size(
                width = PreferenceWidgetDividerDefaults.Width,
                height = PreferenceWidgetDividerDefaults.Height,
            )
            .background(COUITheme.colorScheme.dividerLine),
    )
}

internal object PreferenceWidgetDividerDefaults {
    /** `dimen/preference_divider_width`: 1dp. */
    val Width = 1.dp

    /** `dimen/preference_divider_height`: 22dp. */
    val Height = 22.dp
}
