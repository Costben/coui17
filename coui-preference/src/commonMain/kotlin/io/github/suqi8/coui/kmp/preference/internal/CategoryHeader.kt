// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SmallTitleDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The shared header row of [io.github.suqi8.coui.kmp.preference.LoadingPreferenceCategory] and
 * [io.github.suqi8.coui.kmp.preference.CheckBoxPreferenceCategory].
 *
 * `COUIPreferenceCategory` inflates `res/layout/coui_preference_category_layout.xml`: a horizontal
 * `COUICustomLinearLayoutForPreference` with the title `TextView` (`Widget.COUI.List.Category.Title`)
 * and a `widget_frame` LinearLayout at the end. [SmallTitle] already ports that title
 * (`couiTextAppearanceSmallButton` 12sp medium in `?couiColorSecondNeutral`), so the header reuses it
 * and mirrors its inset for the trailing widget.
 */
@Composable
internal fun CategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = COUITheme.colorScheme.onBackgroundVariant,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallTitle(
            text = title,
            modifier = Modifier.weight(1f),
            textColor = titleColor,
        )
        if (trailing != null) {
            Box(
                modifier = Modifier.padding(
                    end = SmallTitleDefaults.InsideMargin.calculateEndPadding(LocalLayoutDirection.current),
                ),
            ) {
                trailing()
            }
        }
    }
}
