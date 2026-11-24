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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.layout.DialogDefaults
import io.github.suqi8.coui.kmp.layout.LoadingDialogContentLayout
import io.github.suqi8.coui.kmp.layout.LoadingDialogDefaults
import io.github.suqi8.coui.kmp.utils.ProvideHostWindowBackdrop
import io.github.suqi8.coui.kmp.utils.RemovePlatformDialogDefaultEffects
import io.github.suqi8.coui.kmp.utils.platformDialogProperties
import io.github.suqi8.coui.kmp.utils.rememberHostWindowBackdrop

/**
 * A small always-centered loading dialog with a rotating spinner and an optional message,
 * rendered at window level without `Scaffold`, matching the COUI rotating progress dialog
 * (COUIRotatingDialogBuilder).
 *
 * @param show Whether the [WindowLoadingDialog] is shown.
 * @param modifier The modifier to be applied to the [WindowLoadingDialog].
 * @param text The optional message shown below the spinner.
 * @param textColor The color of the message text.
 * @param spinnerColor The color of the spinner.
 * @param backgroundColor The background color of the loading card.
 * @param enableWindowDim Whether to enable window dimming when the dialog is shown.
 * @param onDismissRequest Will be called when the user tries to dismiss the dialog by clicking
 *   outside or pressing the back button. When null (default), the dialog cannot be dismissed
 *   by the user.
 * @param onDismissFinished The callback when the [WindowLoadingDialog] is completely dismissed.
 * @param title The optional title above the spinner (`COUIRotatingDialogBuilder.setDialogTitle`).
 * @param titleColor The color of [title].
 * @param cancelAction The optional cancel button (`COUIRotatingDialogBuilder.setCancelButton`).
 *   With it the dialog takes the cancelable form (`COUIAlertDialog.Rotating.Cancelable`): a wider
 *   card without the minimum height, the message closer to a one-button bar.
 */
@Composable
fun WindowLoadingDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    text: String? = null,
    textColor: Color = LoadingDialogDefaults.textColor(),
    spinnerColor: Color = LoadingDialogDefaults.spinnerColor(),
    backgroundColor: Color = DialogDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    title: String? = null,
    titleColor: Color = DialogDefaults.titleColor(),
    cancelAction: DialogButtonBarAction? = null,
) {
    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val captionBarPadding = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
    val displayCutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val safeTopInset = remember(statusBarsPadding, captionBarPadding, displayCutoutPadding) {
        maxOf(statusBarsPadding, captionBarPadding, displayCutoutPadding)
    }

    val currentOnDismissRequest = rememberUpdatedState(onDismissRequest)

    LoadingDialogContentLayout(
        show = show,
        popupHost = { visible, hostContent ->
            // Captured here, in the host composition, so the dialog frosts the page it opens over.
            val hostBackdrop = rememberHostWindowBackdrop(show || visible)
            if (visible) {
                Dialog(
                    onDismissRequest = { currentOnDismissRequest.value?.invoke() },
                    properties = platformDialogProperties(),
                ) {
                    RemovePlatformDialogDefaultEffects()
                    ProvideHostWindowBackdrop(hostBackdrop.value) {
                        hostContent()
                    }
                }
            }
        },
        modifier = modifier,
        text = text,
        textColor = textColor,
        spinnerColor = spinnerColor,
        backgroundColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        topInset = safeTopInset,
        title = title,
        titleColor = titleColor,
        cancelAction = cancelAction,
    )
}

/** Binary-compatible bridge for callers compiled before [title] and [cancelAction] existed. */
@Deprecated(
    message = "Migrated to the overload that adds title, titleColor and cancelAction parameters.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun WindowLoadingDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    text: String? = null,
    textColor: Color = LoadingDialogDefaults.textColor(),
    spinnerColor: Color = LoadingDialogDefaults.spinnerColor(),
    backgroundColor: Color = DialogDefaults.backgroundColor(),
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
) {
    WindowLoadingDialog(
        show = show,
        modifier = modifier,
        text = text,
        textColor = textColor,
        spinnerColor = spinnerColor,
        backgroundColor = backgroundColor,
        enableWindowDim = enableWindowDim,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        title = null,
    )
}
