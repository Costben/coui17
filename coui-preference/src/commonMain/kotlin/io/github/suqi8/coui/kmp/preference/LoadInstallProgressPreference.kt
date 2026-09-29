// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.InstallLoadProgress
import io.github.suqi8.coui.kmp.basic.InstallLoadProgressColors
import io.github.suqi8.coui.kmp.basic.InstallLoadProgressDefaults
import io.github.suqi8.coui.kmp.basic.InstallLoadStyle
import io.github.suqi8.coui.kmp.basic.LoadProgressButtonDefaults
import io.github.suqi8.coui.kmp.basic.LoadProgressState

/**
 * A preference row whose trailing widget is an [InstallLoadProgress] button, mirroring
 * COUILoadInstallProgressPreference.
 *
 * The device class extends `COUIPreference` and relies on the row style
 * `Preference_COUI_COUILoadInstallProgressPreference` to inflate
 * `res/layout/coui_preference_load_progress.xml` into the standard row's `widget_frame`: a bare
 * `COUIInstallLoadProgress` with `style="Widget.COUI.COUILoadProgress.InstallDownloadSmall"`, i.e.
 * [InstallLoadStyle.Default]. `onBindViewHolder` then copies the preference's text, text size, the
 * three colour state lists, `max`, `progress` and `state` onto it.
 *
 * The device's `setState` is called by the settings app, not by the button: `COUILoadProgress`
 * toggles itself on click and reports the new state through `OnStateChangeListener`, which the
 * preference forwards to its own listener. In Compose the state is a parameter, so there is no
 * listener here — [onClick] is the click and the caller moves [state].
 *
 * @param title The title of the [LoadInstallProgressPreference].
 * @param onClick The callback when the progress button is pressed and released inside its bounds.
 * @param modifier The modifier to be applied to the [LoadInstallProgressPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [LoadInstallProgressPreference].
 * @param summaryColor The color of the summary.
 * @param state The current [LoadProgressState] (COUI `mState`).
 * @param progress The current progress, clamped to `0..max` (COUI `mProgress`).
 * @param max The value [progress] is measured against (COUI `mMax`).
 * @param progressText The label of the button (COUI `progressText`).
 * @param style The [InstallLoadStyle] tier (COUI `couiStyle`).
 * @param colors The [InstallLoadProgressColors] of the button.
 * @param startAction The [Composable] content on the start side.
 * @param endActions The [Composable] content on the end side, placed before the progress button.
 * @param bottomAction The [Composable] content at the bottom of the row.
 * @param insideMargin The margin inside the [LoadInstallProgressPreference].
 * @param cardListPosition The row's position inside its card group.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the progress button is enabled.
 * @param hapticFeedback Whether the button vibrates on press and release (COUI
 *   `loadingButtonNeedVibrate`).
 * @param downloadingContentDescription The accessibility description used instead of
 *   [progressText] while the state is [LoadProgressState.Loading].
 */
@Composable
@NonRestartableComposable
fun LoadInstallProgressPreference(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    state: LoadProgressState = LoadProgressState.Default,
    progress: Int = LoadProgressButtonDefaults.PROGRESS,
    max: Int = LoadProgressButtonDefaults.MAX,
    progressText: String = LoadProgressButtonDefaults.DEFAULT_TEXT,
    style: InstallLoadStyle = InstallLoadStyle.Default,
    colors: InstallLoadProgressColors = InstallLoadProgressDefaults.colors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable RowScope.() -> Unit = {},
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    hapticFeedback: Boolean = false,
    downloadingContentDescription: String? = null,
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
                    .padding(end = 8.dp)
                    .align(Alignment.CenterVertically)
                    .weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                endActions()
            }
            InstallLoadProgress(
                onClick = onClick,
                state = state,
                progress = progress,
                max = max,
                text = progressText,
                style = style,
                colors = colors,
                enabled = enabled,
                hapticFeedback = hapticFeedback,
                downloadingContentDescription = downloadingContentDescription,
            )
        },
        bottomAction = bottomAction,
        holdDownState = holdDownState,
        enabled = enabled,
    )
}
