// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The shared header row of [io.github.suqi8.coui.kmp.preference.LoadingPreferenceCategory] and
 * [io.github.suqi8.coui.kmp.preference.CheckBoxPreferenceCategory].
 *
 * `COUIPreferenceCategory` inflates `res/layout/coui_preference_category_layout.xml`: a horizontal
 * `COUICustomLinearLayoutForPreference` with the title `TextView` (`Widget.COUI.List.Category.Title`)
 * and a `widget_frame` LinearLayout at the end. [SmallTitle] already ports that title
 * (`couiTextAppearanceSmallButton` 12sp medium in `?couiColorSecondNeutral`), so the header reuses it.
 *
 * The title's `support_preference_category_layout_title_margin_start_small` (16dp) is measured from
 * the card's content edge, and the header row itself draws no card, so the header asks [SmallTitle]
 * for a 16dp inset instead of the default that already counts the page margin. `widget_frame` has no
 * end margin at all — the trailing content stops at the card's end edge.
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
            insideMargin = CategoryHeaderTitleMargin,
        )
        if (trailing != null) {
            Box {
                trailing()
            }
        }
    }
}

/** `support_preference_category_layout_title_margin_start_small` (16dp) with the 8dp row inset. */
private val CategoryHeaderTitleMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
