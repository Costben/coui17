// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A preference row whose trailing widget switches between a status text and an icon, mirroring
 * COUIStatusSwitchingPreference.
 *
 * The device inflates `res/layout/coui_preference_widget_status_switching.xml` into the standard
 * row's `widget_frame`: a `FrameLayout` holding a `COUITextView` (`couiTextAppearanceBody`,
 * `?couiColorLabelSecondary`), an `ImageView`, and an `EffectiveAnimationView`. `showText` /
 * `showIcon` / `showAnim` make exactly one of them visible.
 *
 * The `EffectiveAnimationView` branch (`STATUS_TYPE_ANIM`, i.e. a Lottie `raw` resource played on a
 * loop) is intentionally not modelled: the repo has no animation runtime and the class stores the
 * raw resource id, not a drawable, so there is nothing to hand a Compose caller. Only
 * [StatusSwitchingContent.Text] and [StatusSwitchingContent.Icon] exist, mirroring
 * `STATUS_TYPE_TEXT` and `STATUS_TYPE_IMAGE`.
 *
 * @param title The title of the [StatusSwitchingPreference].
 * @param status The widget content, [StatusSwitchingContent.Text] or [StatusSwitchingContent.Icon].
 * @param modifier The modifier to be applied to the [StatusSwitchingPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [StatusSwitchingPreference].
 * @param summaryColor The color of the summary.
 * @param startAction The [Composable] content on the start side.
 * @param statusColor The color of the status text (COUI `?couiColorLabelSecondary`).
 * @param bottomAction The [Composable] content at the bottom of the row.
 * @param insideMargin The margin inside the [StatusSwitchingPreference].
 * @param cardListPosition The row's position inside its card group.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [StatusSwitchingPreference] is clickable.
 * @param onClick The callback when the row is clicked.
 */
@Composable
@NonRestartableComposable
fun StatusSwitchingPreference(
    title: String,
    status: StatusSwitchingContent,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,
    statusColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
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
                    .padding(start = StatusSwitchingPreferenceDefaults.WidgetMarginStart)
                    .align(Alignment.CenterVertically),
            ) {
                StatusSwitchingWidget(
                    status = status,
                    statusColor = statusColor,
                )
            }
        },
        bottomAction = bottomAction,
        onClick = onClick,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}

@Composable
private fun StatusSwitchingWidget(
    status: StatusSwitchingContent,
    statusColor: Color,
) {
    when (status) {
        is StatusSwitchingContent.Text -> Text(
            text = status.text,
            style = COUITheme.textStyles.body2,
            color = statusColor,
            maxLines = 1,
        )

        is StatusSwitchingContent.Icon -> Icon(
            imageVector = status.imageVector,
            contentDescription = status.contentDescription,
            tint = status.tint,
        )
    }
}

/**
 * The trailing widget of a [StatusSwitchingPreference], mirroring the `STATUS_TYPE_*` branches of
 * `COUIStatusSwitchingPreference`.
 */
@Immutable
sealed interface StatusSwitchingContent {

    /** `STATUS_TYPE_TEXT` (2): the `coui_text` `COUITextView`. */
    @Immutable
    data class Text(val text: String) : StatusSwitchingContent

    /** `STATUS_TYPE_IMAGE` (3): the `coui_image` `ImageView`. */
    @Immutable
    data class Icon(
        val imageVector: ImageVector,
        val contentDescription: String? = null,
        val tint: Color = Color.Unspecified,
    ) : StatusSwitchingContent
}

object StatusSwitchingPreferenceDefaults {

    /**
     * The start margin of the widget frame, from
     * `dimen/coui_status_switching_preference_widget_margin_start` (16dp).
     */
    val WidgetMarginStart = 16.dp
}
