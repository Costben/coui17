// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
import io.github.suqi8.coui.kmp.layout.AlertDialogContentLayout
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.utils.RemovePlatformDialogDefaultEffects
import io.github.suqi8.coui.kmp.utils.platformDialogProperties

/**
 * A centered warning dialog rendered at window level without `Scaffold`, matching the COUI
 * center-style alert dialog (`AlertDialogBuildStyle.Center` / `coui_center_alert_dialog_layout.xml`):
 * a title, a message, an optional extra body slot and a cancel / confirm button row. The panel is
 * always centered on screen and uses the COUI center-dialog scale + fade transitions.
 *
 * New in 1.2.0.
 *
 * @param show Whether the [WindowAlertDialog] is shown.
 * @param onConfirm Invoked when the confirm (positive) button is clicked.
 * @param onCancel Invoked when the cancel (negative) button is clicked, the user taps outside, or
 *   presses the back button.
 * @param modifier The modifier to be applied to the [WindowAlertDialog].
 * @param title The title of the dialog.
 * @param summary The message of the dialog.
 * @param confirmText The label of the confirm (positive) button.
 * @param cancelText The label of the cancel (negative) button.
 * @param colors The [AlertDialogColors] of the title, message and panel.
 * @param enableWindowDim Whether to enable window dimming when the dialog is shown.
 * @param onDismissFinished The callback when the [WindowAlertDialog] is completely dismissed.
 * @param cornerRadius Optional corner radius override. If null, [AlertDialogDefaults.CornerRadius]
 *   is used.
 * @param maxWidth The maximum width of the dialog.
 * @param content An optional extra body slot rendered above the button row.
 */
@OptIn(ExperimentalCOUIApi::class)
@ExperimentalCOUIApi
@Composable
fun WindowAlertDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    confirmText: String = AlertDialogDefaults.ConfirmText,
    cancelText: String = AlertDialogDefaults.CancelText,
    colors: AlertDialogColors = AlertDialogDefaults.alertDialogColors(),
    enableWindowDim: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    cornerRadius: Dp? = null,
    maxWidth: Dp = AlertDialogDefaults.MaxWidth,
    content: (@Composable () -> Unit)? = null,
) {
    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val captionBarPadding = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
    val displayCutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val safeTopInset = remember(statusBarsPadding, captionBarPadding, displayCutoutPadding) {
        maxOf(statusBarsPadding, captionBarPadding, displayCutoutPadding)
    }

    val currentOnCancel = rememberUpdatedState(onCancel)

    AlertDialogContentLayout(
        show = show,
        popupHost = { visible, hostContent ->
            if (visible) {
                Dialog(
                    onDismissRequest = { currentOnCancel.value.invoke() },
                    properties = platformDialogProperties(),
                ) {
                    RemovePlatformDialogDefaultEffects()
                    hostContent()
                }
            }
        },
        onConfirm = onConfirm,
        onCancel = onCancel,
        modifier = modifier,
        title = title,
        summary = summary,
        confirmText = confirmText,
        cancelText = cancelText,
        colors = colors,
        enableWindowDim = enableWindowDim,
        onDismissFinished = onDismissFinished,
        cornerRadius = cornerRadius,
        maxWidth = maxWidth,
        topInset = safeTopInset,
        content = content,
    )
}
