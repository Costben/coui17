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
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.TextButtonColors
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.layout.AlertDialogButton
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
import io.github.suqi8.coui.kmp.layout.AlertDialogContentLayout
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.layout.AlertDialogStyle
import io.github.suqi8.coui.kmp.layout.DialogButtonBarDefaults
import io.github.suqi8.coui.kmp.utils.COUIPopupUtils.Companion.DialogLayout

/**
 * A centered warning dialog matching the COUI center-style alert dialog
 * (`AlertDialogBuildStyle.Center` / `coui_center_alert_dialog_layout.xml`): a title, a message,
 * an optional extra body slot and a cancel / neutral / confirm button row. Unlike [OverlayDialog],
 * which is bottom-anchored on compact windows, the panel is always centered on screen and uses the
 * COUI center-dialog scale + fade transitions.
 *
 * `style = AlertDialogStyle.CenterTiny` selects `AlertDialogBuildStyle.Center.Tiny` instead: the
 * same content pinned to the bottom edge in a compact, inset-free panel.
 *
 * New in 1.2.0.
 *
 * @param show Whether the [OverlayAlertDialog] is shown.
 * @param onConfirm Invoked when the confirm (positive) button is clicked.
 * @param onCancel Invoked when the cancel (negative) button is clicked, the user taps outside, or
 *   presses the back button.
 * @param modifier The modifier to be applied to the [OverlayAlertDialog].
 * @param title The title of the dialog.
 * @param summary The message of the dialog.
 * @param confirmText The label of the confirm (positive) button.
 * @param cancelText The label of the cancel (negative) button.
 * @param neutralText The label of the optional third (neutral) button, shown between the other two.
 * @param onNeutral Invoked when the neutral button is clicked.
 * @param recommendedButton The action drawn as the recommend (filled primary) button, matching the
 *   `boolean` overload of the native builder's button setters.
 * @param style The Center-family build style: [AlertDialogStyle.Center] floats the panel in the
 *   middle of the window, [AlertDialogStyle.CenterTiny] pins a compact panel to the bottom edge.
 * @param colors The [AlertDialogColors] of the title, message and panel.
 * @param enableWindowDim Whether to enable window dimming when the dialog is shown.
 * @param onDismissFinished The callback when the [OverlayAlertDialog] is completely dismissed.
 * @param renderInRootScaffold Whether to render the dialog in the root (outermost) Scaffold.
 *   When true (default), the dialog covers the full screen. When false, it renders within the
 *   current Scaffold's bounds.
 * @param cornerRadius Optional corner radius override. If null, [AlertDialogDefaults.CornerRadius]
 *   is used.
 * @param maxWidth The maximum width of the dialog window; the panel is 16dp narrower per side.
 * @param content An optional extra body slot rendered above the button row.
 */
@OptIn(ExperimentalCOUIApi::class)
@ExperimentalCOUIApi
@Composable
fun OverlayAlertDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    confirmText: String = AlertDialogDefaults.ConfirmText,
    cancelText: String = AlertDialogDefaults.CancelText,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    recommendedButton: AlertDialogButton? = null,
    style: AlertDialogStyle = AlertDialogStyle.Center,
    colors: AlertDialogColors = AlertDialogDefaults.alertDialogColors(),
    enableWindowDim: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    renderInRootScaffold: Boolean = true,
    cornerRadius: Dp? = null,
    maxWidth: Dp = AlertDialogDefaults.MaxWidth,
    buttonColors: TextButtonColors = DialogButtonBarDefaults.buttonColors(),
    recommendColors: TextButtonColors = ButtonDefaults.textButtonColorsPrimary(),
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialogContentLayout(
        show = show,
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
        onConfirm = onConfirm,
        onCancel = onCancel,
        modifier = modifier,
        title = title,
        summary = summary,
        confirmText = confirmText,
        cancelText = cancelText,
        neutralText = neutralText,
        onNeutral = onNeutral,
        recommendedButton = recommendedButton,
        style = style,
        colors = colors,
        enableWindowDim = enableWindowDim,
        onDismissFinished = onDismissFinished,
        cornerRadius = cornerRadius,
        maxWidth = maxWidth,
        buttonColors = buttonColors,
        recommendColors = recommendColors,
        content = content,
    )
}

/**
 * The signature from before `style` / `neutral` / `recommendedButton` existed, kept so callers
 * written against the earlier shape still compile and link.
 */
@OptIn(ExperimentalCOUIApi::class)
@ExperimentalCOUIApi
@Composable
fun OverlayAlertDialog(
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
    renderInRootScaffold: Boolean = true,
    cornerRadius: Dp? = null,
    maxWidth: Dp = AlertDialogDefaults.MaxWidth,
    buttonColors: TextButtonColors = DialogButtonBarDefaults.buttonColors(),
    recommendColors: TextButtonColors = ButtonDefaults.textButtonColorsPrimary(),
    content: (@Composable () -> Unit)? = null,
) = OverlayAlertDialog(
    show = show,
    onConfirm = onConfirm,
    onCancel = onCancel,
    modifier = modifier,
    title = title,
    summary = summary,
    confirmText = confirmText,
    cancelText = cancelText,
    // `style` exists only on the primary overload; passing it pins this forward there instead of
    // re-selecting this same overload (which would recurse forever).
    style = AlertDialogStyle.Center,
    colors = colors,
    enableWindowDim = enableWindowDim,
    onDismissFinished = onDismissFinished,
    renderInRootScaffold = renderInRootScaffold,
    cornerRadius = cornerRadius,
    maxWidth = maxWidth,
    buttonColors = buttonColors,
    recommendColors = recommendColors,
    content = content,
)
