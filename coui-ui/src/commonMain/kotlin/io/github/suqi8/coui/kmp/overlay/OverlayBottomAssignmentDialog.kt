// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.layout.DialogContentLayout
import io.github.suqi8.coui.kmp.layout.DialogDefaults
import io.github.suqi8.coui.kmp.utils.COUIPopupUtils.Companion.DialogLayout

/**
 * A dialog pinned to the bottom of the window, matching `COUIAlertDialog.BottomAssignment`.
 *
 * This is the alert-dialog presentation, not a sheet: the panel is the alert-dialog surface
 * (`coui_alert_dialog_builder_background`, inset by `coui_dialog_layout_margin_horizontal` /
 * `..._vertical` — [DialogDefaults.outsideMargin]) and it slides up from the bottom edge without a
 * drag handle, without drag-to-dismiss and without the sheet's own material. `BottomAssignment`
 * sets the window gravity to bottom (`0x50`) and keeps it there on every window size, so neither
 * the large-screen centred presentation nor the sheet host applies.
 *
 * COUI anchors the window to the row that opened it (`create(preferenceView, point)`); a
 * Compose-scaffold hosted dialog is a single full-screen layer, so this port keeps the bottom
 * placement and drops the row anchor.
 *
 * New in 1.2.0.
 *
 * @param show Whether the dialog is shown.
 * @param onDismissRequest Invoked when the user taps outside or presses back.
 * @param modifier The modifier to be applied to the dialog content.
 * @param title The title of the dialog, laid out like the alert dialog's `alertTitle`.
 * @param summary The message of the dialog, laid out like the alert dialog's `message`.
 * @param colors The [AlertDialogColors] of the title, message and panel.
 * @param enableWindowDim Whether to enable window dimming when the dialog is shown.
 * @param onDismissFinished The callback when the dialog is completely dismissed.
 * @param renderInRootScaffold Whether to render the dialog in the root (outermost) Scaffold.
 *   When true (default), the dialog covers the full screen. When false, it renders within the
 *   current Scaffold's bounds.
 * @param cornerRadius Optional corner radius override. If null, [AlertDialogDefaults.CornerRadius]
 *   is used.
 * @param maxWidth The maximum width of the dialog.
 * @param content The body of the dialog, rendered under the title and message. It owns its button
 *   row, as the preference editors ship their own (`coui_center_alert_dialog_button_panel` for the
 *   edit dialog, the second toolbar for the list dialogs).
 */
@OptIn(ExperimentalCOUIApi::class)
@ExperimentalCOUIApi
@Composable
fun OverlayBottomAssignmentDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    colors: AlertDialogColors = AlertDialogDefaults.alertDialogColors(),
    enableWindowDim: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    renderInRootScaffold: Boolean = true,
    cornerRadius: Dp? = null,
    maxWidth: Dp = AlertDialogDefaults.MaxWidth,
    content: @Composable () -> Unit,
) {
    DialogContentLayout(
        show = show,
        titleColor = colors.titleColor,
        summaryColor = colors.summaryColor,
        backgroundColor = colors.backgroundColor,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        popupHost = { visible, hostContent ->
            val visibleState = remember { mutableStateOf(false) }
            visibleState.value = visible
            DialogLayout(
                visible = visibleState,
                enableWindowDim = false,
                enterTransition = EnterTransition.None,
                exitTransition = ExitTransition.None,
                enableAutoLargeScreen = false,
                renderInRootScaffold = renderInRootScaffold,
            ) {
                hostContent()
            }
        },
        modifier = modifier,
        title = title,
        summary = summary,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        forceCentered = false,
        largeScreen = false,
        maxWidth = maxWidth,
        cornerRadius = cornerRadius,
        // The body is `setView` content (the assignment list plus its own button row), so
        // `initContentSpacing` leaves only the 4dp `..._has_title_in_assignment` gap under the
        // message instead of the 16dp a bar-only dialog keeps.
        hasCustomContent = true,
        content = content,
    )
}
