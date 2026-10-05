// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi

/**
 * Internal shared layout for the centered alert dialog variants, the COUI
 * `AlertDialogBuildStyle.Center` family (`coui_center_alert_dialog_layout.xml`): the regular
 * title / message / content panel, but always centered on screen with the COUI center-dialog
 * scale + fade transitions, and closed by a cancel / confirm button bar.
 *
 * Unlike [DialogContentLayout]'s bottom-anchored presentation, the panel is force-centered
 * (`forceCentered = true`) on every window size, matching `COUIAlertDialog.setBuildStyle(CENTER)`,
 * which keeps the center layout on phones as well. The `.Tiny` and `.Rotate` sub-styles of the
 * native builder have no Compose equivalent and are not ported.
 *
 * @param show Whether the dialog is currently shown.
 * @param popupHost A composable that provides the dialog container (e.g., DialogLayout or Dialog).
 * @param onConfirm Invoked when the confirm (positive) button is clicked.
 * @param onCancel Invoked when the cancel (negative) button is clicked, the user taps outside, or
 *   presses back (COUI reports the back key as the negative selection).
 * @param modifier The modifier to be applied to the dialog content.
 * @param title The title of the dialog.
 * @param summary The message of the dialog.
 * @param confirmText The label of the confirm (positive) button.
 * @param cancelText The label of the cancel (negative) button.
 * @param colors The [AlertDialogColors] of the title, message and panel.
 * @param enableWindowDim Whether to enable window dimming.
 * @param onDismissFinished Invoked when the hide animation completes; not invoked if the hide is
 *   cancelled mid-flight.
 * @param cornerRadius Optional corner radius override. If null, [AlertDialogDefaults.CornerRadius].
 * @param maxWidth The maximum width of the panel.
 * @param topInset Optional top inset override forwarded to [DialogContentLayout].
 * @param content An optional extra body slot rendered above the button bar. The title and summary
 *   are rendered by the panel itself, exactly like [DialogContentLayout].
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
internal fun AlertDialogContentLayout(
    show: Boolean,
    popupHost: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit,
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
    topInset: Dp? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnCancel by rememberUpdatedState(onCancel)

    val requestCancel = remember { { currentOnCancel() } }
    val cancelAction = remember(cancelText, requestCancel) {
        DialogButtonBarAction(text = cancelText, onClick = requestCancel)
    }
    val confirmAction = remember(confirmText) {
        DialogButtonBarAction(text = confirmText, onClick = { currentOnConfirm() })
    }

    DialogContentLayout(
        show = show,
        titleColor = colors.titleColor,
        summaryColor = colors.summaryColor,
        backgroundColor = colors.backgroundColor,
        outsideMargin = DialogDefaults.outsideMargin,
        insideMargin = DialogDefaults.insideMargin,
        popupHost = popupHost,
        modifier = modifier,
        title = title,
        summary = summary,
        enableWindowDim = enableWindowDim,
        onDismissRequest = requestCancel,
        onDismissFinished = onDismissFinished,
        // COUIAlertDialog.setBuildStyle(CENTER) keeps the centered presentation on every window
        // size, so the large-screen auto switch of DialogContentLayout is bypassed.
        forceCentered = true,
        maxWidth = maxWidth,
        cornerRadius = cornerRadius,
        topInset = topInset,
    ) {
        content?.invoke()
        DialogButtonBar(
            negative = cancelAction,
            positive = confirmAction,
            // COUI coui_alert_dialog_customer_layout_padding_bottom, the gap above the bar.
            modifier = Modifier.padding(top = AlertDialogDefaults.ButtonBarMarginTop),
        )
    }
}

@ExperimentalCOUIApi
object AlertDialogDefaults {

    /** The default confirm (positive) button label. */
    val ConfirmText = "OK"

    /** The default cancel (negative) button label. */
    val CancelText = "Cancel"

    /**
     * The default corner radius of the centered panel. COUI
     * `coui_round_corner_xxl_radius`, the same [DialogDefaults.CornerRadius] the centered
     * presentation of [DialogContentLayout] uses.
     */
    val CornerRadius = DialogDefaults.CornerRadius

    /**
     * The default upper bound on the panel width. COUI `coui_dialog_max_width`, shared with
     * [DialogDefaults.MaxWidth].
     */
    val MaxWidth = DialogDefaults.MaxWidth

    /**
     * The gap between the panel content and the button bar. COUI
     * `coui_alert_dialog_customer_layout_padding_bottom` (8dp).
     */
    val ButtonBarMarginTop = 8.dp

    /**
     * The default [AlertDialogColors]. Title and message reuse the shared dialog label colors and
     * the panel reuses the shared dialog background.
     *
     * @param titleColor The color of the title.
     * @param summaryColor The color of the message.
     * @param backgroundColor The background color of the panel.
     */
    @OptIn(ExperimentalCOUIApi::class)
    @Composable
    fun alertDialogColors(
        titleColor: Color = DialogDefaults.titleColor(),
        summaryColor: Color = DialogDefaults.summaryColor(),
        backgroundColor: Color = DialogDefaults.backgroundColor(),
    ): AlertDialogColors = remember(titleColor, summaryColor, backgroundColor) {
        AlertDialogColors(
            titleColor = titleColor,
            summaryColor = summaryColor,
            backgroundColor = backgroundColor,
        )
    }

    /**
     * The default color of the title, kept for parity with [DialogDefaults.titleColor].
     */
    @Composable
    fun titleColor() = DialogDefaults.titleColor()

    /**
     * The default color of the message, kept for parity with [DialogDefaults.summaryColor].
     */
    @Composable
    fun summaryColor() = DialogDefaults.summaryColor()

    /**
     * The default background color of the panel, kept for parity with
     * [DialogDefaults.backgroundColor].
     */
    @Composable
    fun backgroundColor() = DialogDefaults.backgroundColor()
}

/**
 * Colors used by a centered alert dialog.
 *
 * @param titleColor The color of the title.
 * @param summaryColor The color of the message.
 * @param backgroundColor The background color of the panel.
 */
@ExperimentalCOUIApi
@Immutable
data class AlertDialogColors(
    val titleColor: Color,
    val summaryColor: Color,
    val backgroundColor: Color,
)
