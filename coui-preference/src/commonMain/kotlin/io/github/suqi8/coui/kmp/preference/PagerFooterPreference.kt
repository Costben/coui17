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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The closing note a COUI settings page ends with, mirroring COUIPagerFooterPreference.
 *
 * The device class shares `COUIPagerHeaderPreference`'s layout shape — icon, title over summary,
 * empty `widget_frame` — with three differences: `res/layout/coui_pager_footer_preference.xml`
 * pins the summary to `dimen/coui_preference_header_footer_textsize` (12sp), `onBindViewHolder`
 * writes a `support_preference_footer_preference_margin_bottom` (24dp) bottom margin onto the item
 * unless `withExtraMarginBottom` is false, and it honours the `COUIPreference_couiEnalbeClickSpan`
 * flag by running the summary through `COUIPreferenceUtils.setSummaryView`, which makes its spans
 * tappable.
 *
 * The click-span flag is why [summary] is an [AnnotatedString]: the repo's [Text] renders one with
 * link styles already, so there is no separate boolean to carry — pass a plain
 * `AnnotatedString(text)` when no span should react.
 *
 * Like [PagerHeaderPreference] it does not reuse [io.github.suqi8.coui.kmp.basic.BasicComponent]:
 * that component enforces a 48dp minimum height and a 14sp summary, neither of which this row has.
 *
 * @param summary The summary of the [PagerFooterPreference], with its clickable spans.
 * @param modifier The modifier to be applied to the [PagerFooterPreference].
 * @param title The optional title of the [PagerFooterPreference].
 * @param startAction The [Composable] content on the start side, in the layout's `@android:id/icon`.
 * @param endActions The [Composable] content on the end side, in the layout's `widget_frame`.
 * @param titleColor The color of the title (`coui_preference_title_color`, `?couiColorLabelPrimary`).
 * @param summaryColor The color of the summary (`coui_preference_secondary_text_color`,
 *   `?couiColorSecondNeutral`).
 * @param withExtraMarginBottom Whether the 24dp bottom margin is applied (COUI
 *   `withExtraMarginBottom`, true on the device).
 */
@Composable
@NonRestartableComposable
fun PagerFooterPreference(
    summary: AnnotatedString,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    titleColor: Color = COUITheme.colorScheme.onBackground,
    summaryColor: Color = COUITheme.colorScheme.onBackgroundVariant,
    withExtraMarginBottom: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = if (withExtraMarginBottom) {
                    PagerFooterPreferenceDefaults.MarginBottom
                } else {
                    0.dp
                },
            )
            .padding(PagerFooterPreferenceDefaults.InsideMargin),
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
                    fontSize = PagerFooterPreferenceDefaults.TextSize,
                ),
                color = summaryColor,
            )
        }
        endActions()
    }
}

object PagerFooterPreferenceDefaults {

    /**
     * `support_preference_header_preference_padding_vertical` (8dp) horizontally paired with
     * `support_preference_title_padding_start` / `_end` (32dp), the same insets the header row uses.
     */
    val InsideMargin = PaddingValues(horizontal = 32.dp, vertical = 8.dp)

    /** `support_preference_footer_preference_margin_bottom` (24dp). */
    val MarginBottom = 24.dp

    /** `dimen/coui_preference_header_footer_textsize` (12sp). */
    val TextSize = 12.sp
}
