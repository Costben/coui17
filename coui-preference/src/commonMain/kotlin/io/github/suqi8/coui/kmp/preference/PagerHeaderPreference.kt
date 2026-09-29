// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The explanatory note a COUI settings page opens with, mirroring COUIPagerHeaderPreference.
 *
 * The device class adds nothing to `androidx.preference.Preference` but its layout:
 * `res/layout/coui_pager_header_preference.xml`, a horizontal `LinearLayout` with an optional round
 * icon, a title (`COUIPreferenceTitleStyle`) over a summary (`COUIPreferenceDescriptionStyle`), and
 * an empty `widget_frame`. The row is 8dp tall inside
 * (`support_preference_header_preference_padding_vertical`), inset 32dp horizontally
 * (`support_preference_title_padding_start` / `_end`) and carries a 12dp top margin
 * (`support_preference_header_preference_margin_top`).
 *
 * It deliberately does not reuse [io.github.suqi8.coui.kmp.basic.BasicComponent]: that component
 * enforces a 48dp minimum height and renders its summary in the 14sp body style, while this row is
 * only as tall as its text and its summary is the 12sp description style.
 *
 * @param summary The summary of the [PagerHeaderPreference].
 * @param modifier The modifier to be applied to the [PagerHeaderPreference].
 * @param title The optional title of the [PagerHeaderPreference].
 * @param startAction The [Composable] content on the start side, in the layout's `@android:id/icon`.
 * @param endActions The [Composable] content on the end side, in the layout's `widget_frame`.
 * @param titleColor The color of the title (`coui_preference_title_color`, `?couiColorLabelPrimary`).
 * @param summaryColor The color of the summary (`coui_preference_secondary_text_color`,
 *   `?couiColorSecondNeutral`).
 */
@Composable
@NonRestartableComposable
fun PagerHeaderPreference(
    summary: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    titleColor: Color = COUITheme.colorScheme.onBackground,
    summaryColor: Color = COUITheme.colorScheme.onBackgroundVariant,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = PagerHeaderPreferenceDefaults.MarginTop)
            .padding(PagerHeaderPreferenceDefaults.InsideMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (startAction != null) {
            Column {
                startAction()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = COUITheme.textStyles.headline1,
                    color = titleColor,
                )
            }
            Text(
                text = summary,
                style = COUITheme.textStyles.footnote1.copy(
                    fontSize = PagerHeaderPreferenceDefaults.TextSize,
                ),
                color = summaryColor,
            )
        }
        endActions()
    }
}

object PagerHeaderPreferenceDefaults {

    /**
     * `support_preference_header_preference_padding_vertical` (8dp) horizontally paired with
     * `support_preference_title_padding_start` / `_end` (32dp).
     */
    val InsideMargin = PaddingValues(horizontal = 32.dp, vertical = 8.dp)

    /** `support_preference_header_preference_margin_top` (12dp). */
    val MarginTop = 12.dp

    /**
     * `dimen/coui_preference_header_footer_textsize` (12sp), the size the header's
     * `COUIPreferenceDescriptionStyle` summary resolves to (`couiTextBodyXS`).
     */
    val TextSize = 12.sp
}
