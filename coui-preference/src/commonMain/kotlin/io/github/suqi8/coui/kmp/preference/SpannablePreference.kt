// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A preference row whose summary is an [AnnotatedString], mirroring COUISpannablePreference.
 *
 * `COUISpannablePreference` adds no fields or methods of its own: its constructor only passes
 * `R.style.Preference_COUI_COUISpannablePreference`, which is `Preference.COUI` plus
 * `<item name="couiEnalbeClickSpan">true</item>`. `COUIPreference.onBindViewHolder` reads that flag
 * into `mIsEnableClickSpan` and, when set, calls `COUIPreferenceUtils.setSummaryView`, which attaches
 * a `LinkMovementMethod` to the summary `TextView` so `URLSpan`s inside it are tappable. The Compose
 * equivalent of "a summary whose spans respond" is an [AnnotatedString] summary, which the repo's
 * [Text] already renders with link styles.
 *
 * @param summary The summary, with its clickable spans.
 * @param modifier The modifier to be applied to the [SpannablePreference].
 * @param title The optional title of the [SpannablePreference].
 * @param colors The [SpannablePreferenceColors] of the row.
 * @param startAction The [Composable] content on the start side.
 * @param endActions The [Composable] content on the end side.
 * @param bottomAction The [Composable] content at the bottom of the row.
 * @param insideMargin The margin inside the [SpannablePreference].
 * @param cardListPosition The row's position inside its card group.
 * @param onClick The callback when the row is clicked.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [SpannablePreference] is clickable.
 */
@Composable
@NonRestartableComposable
fun SpannablePreference(
    summary: AnnotatedString,
    modifier: Modifier = Modifier,
    title: String? = null,
    colors: SpannablePreferenceColors = SpannablePreferenceDefaults.colors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
) {
    BasicComponent(
        modifier = modifier,
        startAction = startAction,
        endActions = endActions,
        bottomAction = bottomAction,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        onClick = onClick,
        holdDownState = holdDownState,
        enabled = enabled,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = COUITheme.textStyles.headline1,
                fontWeight = FontWeight.Medium,
                color = colors.titleColor(enabled),
            )
        }
        Text(
            text = summary,
            modifier = Modifier.padding(top = if (title != null) SpannablePreferenceDefaults.MarginBetweenLine else 0.dp),
            style = COUITheme.textStyles.body2,
            color = colors.summaryColor(enabled),
        )
    }
}

object SpannablePreferenceDefaults {

    /**
     * The gap between the title and the summary, matching
     * `dimen/support_preference_margin_between_line` (2dp), the same value
     * [io.github.suqi8.coui.kmp.basic.BasicComponent] applies between its own lines.
     */
    val MarginBetweenLine = 2.dp

    /**
     * The default [SpannablePreferenceColors]: the same four roles
     * [io.github.suqi8.coui.kmp.basic.BasicComponentDefaults] resolves for a standard preference row
     * — title `?couiColorPrimaryNeutral` and summary `coui_color_label_secondary_variant` — so a
     * [SpannablePreference] sits in a card next to the other rows without a colour step.
     */
    @Composable
    fun colors(
        titleColor: Color = COUITheme.colorScheme.onBackground,
        disabledTitleColor: Color = COUITheme.colorScheme.disabledOnSecondaryVariant,
        summaryColor: Color = COUITheme.colorScheme.onSurfaceVariantSummary,
        disabledSummaryColor: Color = COUITheme.colorScheme.disabledOnSecondaryVariant,
    ): SpannablePreferenceColors = remember(
        titleColor,
        disabledTitleColor,
        summaryColor,
        disabledSummaryColor,
    ) {
        SpannablePreferenceColors(
            titleColor = titleColor,
            disabledTitleColor = disabledTitleColor,
            summaryColor = summaryColor,
            disabledSummaryColor = disabledSummaryColor,
        )
    }
}

/**
 * Colors used by a [SpannablePreference].
 *
 * @param titleColor The color of the title.
 * @param disabledTitleColor The color of the title while disabled.
 * @param summaryColor The color of the summary.
 * @param disabledSummaryColor The color of the summary while disabled.
 */
@Immutable
data class SpannablePreferenceColors(
    val titleColor: Color,
    val disabledTitleColor: Color,
    val summaryColor: Color,
    val disabledSummaryColor: Color,
) {
    @Stable
    internal fun titleColor(enabled: Boolean): Color = if (enabled) titleColor else disabledTitleColor

    @Stable
    internal fun summaryColor(enabled: Boolean): Color = if (enabled) summaryColor else disabledSummaryColor
}
