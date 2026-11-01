// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.BasicComponentColors
import io.github.suqi8.coui.kmp.basic.BasicComponentDefaults
import io.github.suqi8.coui.kmp.basic.CardListPosition
import io.github.suqi8.coui.kmp.basic.RadioButton
import io.github.suqi8.coui.kmp.basic.RadioButtonColors
import io.github.suqi8.coui.kmp.basic.RadioButtonDefaults
import io.github.suqi8.coui.kmp.preference.internal.StartActionSlot

/**
 * A single-select row with a selection mark, mirroring COUIMarkPreference.
 *
 * The mark itself is not clickable — the whole row is the click target. Rows are meant to be
 * used as a single-select group: clicking a row invokes [onClick] and the caller moves the
 * checked state to that row.
 *
 * @param title The title of the [MarkPreference].
 * @param checked Whether this row currently carries the mark.
 * @param onClick The callback when the [MarkPreference] is clicked.
 * @param modifier The modifier to be applied to the [MarkPreference].
 * @param titleColor The color of the title.
 * @param summary The summary of the [MarkPreference].
 * @param summaryColor The color of the summary.
 * @param markColors The [RadioButtonColors] of the mark.
 * @param startAction The [Composable] content on the start side of the [MarkPreference].
 * @param endActions The [Composable] content on the end side of the [MarkPreference].
 * @param markLocation The location of the mark, [MarkLocation.End] or [MarkLocation.Start].
 * @param bottomAction The [Composable] content at the bottom of the [MarkPreference].
 * @param insideMargin The margin inside the [MarkPreference].
 * @param cardListPosition The row's position inside its card group. Rounded outer edges receive extra padding.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param enabled Whether the [MarkPreference] is clickable.
 * @param onMainLayoutClick Replaces the row click, and makes the mark itself clickable so the row
 *   keeps a select target. `COUIMarkWithDividerPreference` splits the row into `main_layout`, which
 *   reports to `OnMainLayoutClickListener`, and `radio_layout`, which still runs the preference
 *   click. `null` (default) keeps a single row-wide click target.
 */
@Composable
@NonRestartableComposable
fun MarkPreference(
    title: String,
    checked: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    markColors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    markLocation: MarkLocation = MarkLocation.End,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
    onMainLayoutClick: (() -> Unit)? = null,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnMainLayoutClick by rememberUpdatedState(onMainLayoutClick)

    BasicComponent(
        modifier = modifier,
        insideMargin = insideMargin,
        cardListPosition = cardListPosition,
        title = title,
        titleColor = titleColor,
        summary = summary,
        summaryColor = summaryColor,
        startAction = if (markLocation == MarkLocation.Start || startAction != null) {
            {
                Row {
                    if (markLocation == MarkLocation.Start) {
                        MarkPreferenceMark(
                            checked = checked,
                            enabled = enabled,
                            markColors = markColors,
                            // `coui_head_mark` in coui_preference_mark_divider_default.xml: the box
                            // carries support_preference_mark_checkbox_margin_end (11dp) before the
                            // text block. Its -4dp layout_marginStart has no Compose equivalent —
                            // padding cannot be negative — so the mark starts at the row's own
                            // padding instead.
                            modifier = Modifier.padding(end = MarkPreferenceHeadMarkMarginEnd),
                        )
                    }

                    startAction?.let {
                        StartActionSlot(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .weight(1f, fill = false),
                            endSpacing = 5.dp,
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
                        .align(Alignment.CenterVertically)
                        .weight(1f, fill = false),
                ) {
                    it()
                }
            }

            if (markLocation == MarkLocation.End) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                ) {
                    MarkPreferenceMark(
                        checked = checked,
                        enabled = enabled,
                        markColors = markColors,
                        // Only a split row needs its own mark target; otherwise the row is the target.
                        onClick = if (onMainLayoutClick != null) {
                            { currentOnClick?.invoke() }
                        } else {
                            null
                        },
                    )
                }
            }
        },
        bottomAction = bottomAction,
        onClick = {
            val mainLayoutClick = currentOnMainLayoutClick
            if (mainLayoutClick != null) {
                if (enabled) mainLayoutClick()
            } else {
                currentOnClick.takeIf { enabled }?.invoke()
            }
        },
        role = Role.RadioButton,
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
fun MarkPreference(
    title: String,
    checked: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    markColors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    startAction: @Composable (() -> Unit)? = null,
    endActions: @Composable (RowScope.() -> Unit)? = null,
    markLocation: MarkLocation = MarkLocation.End,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    cardListPosition: CardListPosition = CardListPosition.None,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
): Unit = MarkPreference(
    title = title,
    checked = checked,
    onClick = onClick,
    modifier = modifier,
    titleColor = titleColor,
    summary = summary,
    summaryColor = summaryColor,
    markColors = markColors,
    startAction = startAction,
    endActions = endActions,
    markLocation = markLocation,
    bottomAction = bottomAction,
    insideMargin = insideMargin,
    cardListPosition = cardListPosition,
    holdDownState = holdDownState,
    enabled = enabled,
)

@Composable
private fun MarkPreferenceMark(
    checked: Boolean,
    enabled: Boolean,
    markColors: RadioButtonColors,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    RadioButton(
        modifier = modifier,
        selected = checked,
        onClick = onClick,
        enabled = enabled,
        colors = markColors,
    )
}

/**
 * The gap `coui_head_mark` keeps between the box and the text block,
 * `support_preference_mark_checkbox_margin_end` (11dp).
 */
private val MarkPreferenceHeadMarkMarginEnd = 11.dp

/**
 * The location of the mark in a [MarkPreference]: [End] maps to COUI TAIL_MARK (default),
 * [Start] to HEAD_MARK.
 */
enum class MarkLocation {
    Start,
    End,
}
